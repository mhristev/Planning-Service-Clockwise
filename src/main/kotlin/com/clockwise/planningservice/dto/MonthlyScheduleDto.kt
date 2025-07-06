package com.clockwise.planningservice.dto

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * DTO for session note in the monthly schedule
 */
data class SessionNoteDto(
    val id: String,
    val noteContent: String
)

/**
 * DTO for work session in the monthly schedule
 */
data class WorkSessionDto(
    val id: String,
    val clockInTime: ZonedDateTime?,
    val clockOutTime: ZonedDateTime?,
    val confirmed: Boolean,
    val note: SessionNoteDto?
)

/**
 * DTO for a shift with its work session details
 */
data class ShiftWithSessionsDto(
    val id: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val role: String?,
    val workSession: WorkSessionDto?
)

/**
 * DTO for a weekly schedule containing shifts for a specific user
 */
data class MonthlyScheduleDto(
    val scheduleId: String,
    val weekStartDate: LocalDate,
    val shifts: List<ShiftWithSessionsDto>
)
