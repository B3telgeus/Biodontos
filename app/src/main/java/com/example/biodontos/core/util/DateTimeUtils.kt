package com.example.biodontos.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    val CLINICAL_ZONE: ZoneId = ZoneId.of("America/Bogota")

    val CLINICAL_SLOTS = listOf(
        "08:00 AM",
        "09:00 AM",
        "10:00 AM",
        "11:00 AM",
        "02:00 PM",
        "03:00 PM",
        "04:00 PM",
        "05:00 PM"
    )

    private val spanishLocale = Locale.forLanguageTag("es-CO")

    fun getTodayDate(): String {
        return LocalDate.now(CLINICAL_ZONE).toString() // YYYY-MM-DD
    }

    fun getUpcomingDays(count: Int = 14): List<String> {
        val today = LocalDate.now(CLINICAL_ZONE)
        val days = mutableListOf<String>()
        var current = today
        while (days.size < count) {
            // Excluir domingos si la clínica está cerrada
            if (current.dayOfWeek.value != 7) {
                days.add(current.toString())
            }
            current = current.plusDays(1)
        }
        return days
    }

    fun parseToClinicalZonedDateTime(dateTimeStr: String): ZonedDateTime? {
        return try {
            val trimmed = dateTimeStr.trim()
            val cleaned = if (trimmed.contains("[")) trimmed.substringBefore("[") else trimmed
            when {
                cleaned.endsWith("Z", ignoreCase = true) ||
                cleaned.contains("+") ||
                (cleaned.contains("-") && cleaned.indexOf("-", 10) > 0) -> {
                    OffsetDateTime.parse(cleaned).atZoneSameInstant(CLINICAL_ZONE)
                }
                cleaned.contains("T") -> {
                    LocalDateTime.parse(cleaned).atZone(CLINICAL_ZONE)
                }
                cleaned.contains(" ") -> {
                    val parts = cleaned.split(" ")
                    val datePart = parts[0]
                    val timePart = if (parts.getOrNull(1)?.length == 5) "${parts[1]}:00" else parts.getOrNull(1) ?: "00:00:00"
                    LocalDateTime.parse("${datePart}T${timePart}").atZone(CLINICAL_ZONE)
                }
                else -> {
                    LocalDate.parse(cleaned).atStartOfDay(CLINICAL_ZONE)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isUpcoming(dateTime: LocalDateTime): Boolean {
        return dateTime.atZone(CLINICAL_ZONE).isAfter(ZonedDateTime.now(CLINICAL_ZONE))
    }

    fun isUpcoming(dateTimeStr: String): Boolean {
        val zdt = parseToClinicalZonedDateTime(dateTimeStr) ?: return false
        val now = ZonedDateTime.now(CLINICAL_ZONE)
        return zdt.isAfter(now)
    }

    fun formatToReadableDate(dateTime: LocalDateTime): String = formatToReadableDate(dateTime.toString())

    fun formatToShortDate(dateTime: LocalDateTime): String = formatToShortDate(dateTime.toString())

    fun extractTime(dateTime: LocalDateTime): String = extractTime(dateTime.toString())

    fun formatToReadableDate(dateTimeStr: String): String {
        val zdt = parseToClinicalZonedDateTime(dateTimeStr)
        val date = zdt?.toLocalDate() ?: try {
            val datePart = dateTimeStr.substringBefore(" ").substringBefore("T")
            LocalDate.parse(datePart)
        } catch (_: Exception) {
            return dateTimeStr
        }
        val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", spanishLocale)
        return date.format(formatter).replaceFirstChar { if (it.isLowerCase()) it.titlecase(spanishLocale) else it.toString() }
    }

    fun formatToShortDate(dateTimeStr: String): String {
        val zdt = parseToClinicalZonedDateTime(dateTimeStr)
        val date = zdt?.toLocalDate() ?: try {
            val datePart = dateTimeStr.substringBefore(" ").substringBefore("T")
            LocalDate.parse(datePart)
        } catch (_: Exception) {
            return dateTimeStr
        }
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", spanishLocale)
        return date.format(formatter)
    }

    fun extractTime(dateTimeStr: String): String {
        val zdt = parseToClinicalZonedDateTime(dateTimeStr)
        if (zdt != null) {
            val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
            return zdt.format(formatter)
        }
        return try {
            if (dateTimeStr.contains(" ")) {
                val parts = dateTimeStr.split(" ")
                if (parts.size >= 2) {
                    val timePart = parts[1]
                    val amPm = parts.getOrNull(2) ?: ""
                    "$timePart $amPm".trim()
                } else {
                    dateTimeStr
                }
            } else {
                dateTimeStr
            }
        } catch (_: Exception) {
            dateTimeStr
        }
    }

    fun parseSlotToLocalTime(slotStr: String): LocalTime = when (slotStr) {
        "08:00 AM" -> LocalTime.of(8, 0)
        "09:00 AM" -> LocalTime.of(9, 0)
        "10:00 AM" -> LocalTime.of(10, 0)
        "11:00 AM" -> LocalTime.of(11, 0)
        "02:00 PM" -> LocalTime.of(14, 0)
        "03:00 PM" -> LocalTime.of(15, 0)
        "04:00 PM" -> LocalTime.of(16, 0)
        "05:00 PM" -> LocalTime.of(17, 0)
        else -> LocalTime.of(9, 0)
    }

    fun buildDateTimeString(dateStr: String, slotStr: String): String {
        val localTime = parseSlotToLocalTime(slotStr)
        val localDate = LocalDate.parse(dateStr)
        val zonedDateTime = localDate.atTime(localTime).atZone(CLINICAL_ZONE)
        return zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    fun computeAvailableSlots(
        allSlots: List<String>,
        takenStartTimes: List<String>,
        selectedDate: String = getTodayDate(),
        referenceTime: LocalTime? = null
    ): List<String> {
        val occupiedNormalized = takenStartTimes.mapNotNull { startTime ->
            val zdt = parseToClinicalZonedDateTime(startTime)
            if (zdt != null) {
                zdt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
            } else {
                normalizeLegacySlot(startTime)
            }
        }.toSet()

        val isSelectedToday = selectedDate == getTodayDate()
        val currentTime = referenceTime ?: LocalTime.now(CLINICAL_ZONE)

        return allSlots.filter { slot ->
            val isNotOccupied = !occupiedNormalized.contains(slot)
            if (!isNotOccupied) return@filter false

            if (isSelectedToday) {
                val slotTime = parseSlotToLocalTime(slot)
                slotTime.isAfter(currentTime)
            } else {
                true
            }
        }
    }

    private fun normalizeLegacySlot(str: String): String? = when {
        str.contains("08:00") -> "08:00 AM"
        str.contains("09:00") -> "09:00 AM"
        str.contains("10:00") -> "10:00 AM"
        str.contains("11:00") -> "11:00 AM"
        str.contains("14:00") || str.contains("02:00 PM") -> "02:00 PM"
        str.contains("15:00") || str.contains("03:00 PM") -> "03:00 PM"
        str.contains("16:00") || str.contains("04:00 PM") -> "04:00 PM"
        str.contains("17:00") || str.contains("05:00 PM") -> "05:00 PM"
        else -> null
    }
}
