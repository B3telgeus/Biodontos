package com.example.biodontos.core.util

import com.example.biodontos.domain.scheduling.ClinicClock
import java.time.LocalDateTime
import java.time.ZoneId

class SystemClinicClock(
    override val zone: ZoneId = ZoneId.of("America/Bogota")
) : ClinicClock {
    override fun now(): LocalDateTime = LocalDateTime.now(zone)
}
