package com.example.biodontos.domain.scheduling

import com.example.biodontos.domain.model.WeeklyAvailability
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

data class TimeSlot(
    val start: LocalDateTime,
    val end: LocalDateTime
) {
    init {
        require(end.isAfter(start)) { "El fin del slot debe ser posterior al inicio" }
    }
}

object SlotCalculator {
    fun calculateAvailableSlots(
        targetDate: LocalDate,
        availabilities: List<WeeklyAvailability>,
        busyIntervals: List<ClosedRange<LocalDateTime>>,
        currentTime: LocalDateTime,
        minLeadTimeHours: Long = 2
    ): List<TimeSlot> {
        val dayAvailabilities = availabilities.filter { 
            it.isActive && it.dayOfWeek == targetDate.dayOfWeek 
        }

        if (dayAvailabilities.isEmpty()) {
            return emptyList()
        }

        val minAllowedTime = currentTime.plusHours(minLeadTimeHours)

        val potentialSlots = mutableListOf<TimeSlot>()

        for (avail in dayAvailabilities) {
            var currentSlotStart = targetDate.atTime(avail.startTime)
            val blockEnd = targetDate.atTime(avail.endTime)
            val slotDuration = Duration.ofMinutes(avail.slotMinutes.toLong())

            while (true) {
                val currentSlotEnd = currentSlotStart.plus(slotDuration)
                if (currentSlotEnd.isAfter(blockEnd)) {
                    break
                }
                potentialSlots.add(TimeSlot(currentSlotStart, currentSlotEnd))
                currentSlotStart = currentSlotEnd
            }
        }

        return potentialSlots.filter { slot ->
            // Debe estar en el futuro respetando la ventana mínima
            slot.start >= minAllowedTime &&
            // No debe intersectar con citas existentes
            busyIntervals.none { busy ->
                slot.start < busy.endInclusive && slot.end > busy.start
            }
        }.sortedBy { it.start }
    }
}
