package com.clockwise.planningservice.dto

import jakarta.validation.constraints.FutureOrPresent
import org.jetbrains.annotations.NotNull
import java.time.ZonedDateTime

data class AvailabilityRequest(
    val employeeId: String?,
    val startTime: ZonedDateTime?,
    val endTime: ZonedDateTime?,
    val businessUnitId: String?
)

data class AvailabilityResponse(
    val id: String,
    val employeeId: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val businessUnitId: String?,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
) 