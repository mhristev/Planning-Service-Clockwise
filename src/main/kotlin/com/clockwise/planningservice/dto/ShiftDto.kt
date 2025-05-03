package com.clockwise.planningservice.dto

import jakarta.validation.constraints.FutureOrPresent
import org.jetbrains.annotations.NotNull
import java.time.ZonedDateTime

data class ShiftRequest(
    val scheduleId: String?,
    val employeeId: String?,
    val startTime: ZonedDateTime?,
    val position: String?,
    val businessUnitId: String?,
    val endTime: ZonedDateTime?
)

data class ShiftResponse(
    val id: String,
    val scheduleId: String,
    val employeeId: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val position: String?,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
) 