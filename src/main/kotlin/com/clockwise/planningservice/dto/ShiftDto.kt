package com.clockwise.planningservice.dto

import com.clockwise.planningservice.dto.workload.WorkSessionResponse
import com.clockwise.planningservice.dto.workload.SessionNoteResponse
import jakarta.validation.constraints.FutureOrPresent
import org.jetbrains.annotations.NotNull
import java.time.ZonedDateTime
import java.time.OffsetDateTime
import com.clockwise.planningservice.domains.workload.WorkSessionStatus

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

/**
 * Enhanced shift response that includes work session and session note data
 */
data class ShiftWithWorkSessionResponse(
    val id: String,
    val scheduleId: String,
    val employeeId: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val position: String?,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val workSession: WorkSessionWithNoteResponse?
)

/**
 * Work session response that includes the session note
 */
data class WorkSessionWithNoteResponse(
    val id: String?,
    val userId: String,
    val shiftId: String,
    val clockInTime: OffsetDateTime?,
    val clockOutTime: OffsetDateTime?,
    val totalMinutes: Int?,
    val status: WorkSessionStatus,
    val confirmed: Boolean = false,
    val confirmedBy: String? = null,
    val confirmedAt: OffsetDateTime? = null,
    val sessionNote: SessionNoteResponse?
) 