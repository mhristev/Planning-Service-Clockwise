package com.clockwise.planningservice

import jakarta.validation.constraints.FutureOrPresent
import org.jetbrains.annotations.NotNull
import java.time.LocalDateTime

data class AvailabilityRequest(

    val employeeId: String?,

    val startTime: LocalDateTime?,

    val endTime: LocalDateTime?
)


data class ScheduleRequest(
    val restaurantId: String?,

    val weekStart: LocalDateTime?
)


data class ShiftRequest(
    val scheduleId: String?,

    val employeeId: String?,
    val startTime: LocalDateTime?,

    @field:NotNull("End time is required")
    @field:FutureOrPresent(message = "End time must be in the present or future")
    val endTime: LocalDateTime?
)

data class AvailabilityResponse(
    val id: String,
    val employeeId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)


data class ScheduleResponse(
    val id: String,
    val restaurantId: String,
    val weekStart: LocalDateTime,
    val status: ScheduleStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ShiftResponse(
    val id: String,
    val scheduleId: String,
    val employeeId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)