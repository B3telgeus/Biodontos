package com.example.biodontos.domain.scheduling

import java.time.LocalDateTime
import java.time.ZoneId

interface ClinicClock {
    val zone: ZoneId
    fun now(): LocalDateTime
}
