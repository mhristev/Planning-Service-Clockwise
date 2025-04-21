package com.clockwise.planningservice.dto

import jakarta.validation.constraints.FutureOrPresent
import org.jetbrains.annotations.NotNull
import java.time.LocalDateTime

data class AvailabilityRequest(
    val employeeId: String?,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
    val businessUnitId: String?
)

data class AvailabilityResponse(
    val id: String,
    val employeeId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val businessUnitId: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) 