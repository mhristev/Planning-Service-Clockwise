package com.clockwise.planningservice.dto

import jakarta.validation.constraints.FutureOrPresent
import org.jetbrains.annotations.NotNull
import java.time.LocalDateTime

data class ShiftRequest(
    val scheduleId: String?,
    val employeeId: String?,
    val startTime: LocalDateTime?,
    @field:NotNull("End time is required")
    @field:FutureOrPresent(message = "End time must be in the present or future")
    val endTime: LocalDateTime?,
    val position: String? = null
)

data class ShiftResponse(
    val id: String,
    val scheduleId: String,
    val employeeId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val position: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) 