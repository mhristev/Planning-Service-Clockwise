package com.clockwise.planningservice.dto

import java.time.LocalDate
import java.time.ZonedDateTime

data class ScheduleRequest(
    val businessUnitId: String?,
    val weekStart: LocalDate?,
    val status: String? = "DRAFT"
)

data class ScheduleResponse(
    val id: String,
    val businessUnitId: String,
    val weekStart: ZonedDateTime,
    val status: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
)

data class ScheduleWithShiftsResponse(
    val id: String,
    val businessUnitId: String,
    val weekStart: ZonedDateTime,
    val status: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val shifts: List<ShiftResponse>
) 