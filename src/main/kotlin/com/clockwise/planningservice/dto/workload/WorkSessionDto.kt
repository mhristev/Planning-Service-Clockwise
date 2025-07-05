package com.clockwise.planningservice.dto.workload

import com.clockwise.planningservice.domains.workload.WorkSessionStatus
import java.time.OffsetDateTime

data class ClockInRequest(
    val userId: String,
    val shiftId: String
)

data class ClockOutRequest(
    val userId: String,
    val shiftId: String
)

data class WorkSessionResponse(
    val id: String?,
    val userId: String,
    val shiftId: String,
    val clockInTime: OffsetDateTime,
    val clockOutTime: OffsetDateTime?,
    val totalMinutes: Int?,
    val status: WorkSessionStatus,
    val confirmed: Boolean = false,
    val confirmedBy: String? = null,
    val confirmedAt: OffsetDateTime? = null,
    val modifiedBy: String? = null,
    val originalClockInTime: OffsetDateTime? = null,
    val originalClockOutTime: OffsetDateTime? = null
)

data class WorkHoursResponse(
    val userId: String,
    val totalSessions: Int,
    val totalMinutesWorked: Int,
    val sessions: List<WorkSessionResponse>
)

data class SessionNoteRequest(
    val workSessionId: String,
    val content: String
)

data class SessionNoteResponse(
    val id: String?,
    val workSessionId: String,
    val content: String,
    val createdAt: OffsetDateTime
)

// New DTOs for work session confirmation
data class WorkSessionConfirmationRequest(
    val workSessionId: String,
    val confirmedBy: String
)

data class WorkSessionModificationRequest(
    val workSessionId: String,
    val newClockInTime: OffsetDateTime,
    val newClockOutTime: OffsetDateTime?,
    val modifiedBy: String
)

data class WorkSessionModifyAndConfirmRequest(
    val workSessionId: String,
    val newClockInTime: OffsetDateTime,
    val newClockOutTime: OffsetDateTime?,
    val modifiedBy: String
)

data class UnconfirmedWorkSessionsResponse(
    val businessUnitId: String,
    val totalUnconfirmed: Int,
    val workSessions: List<WorkSessionWithShiftInfoResponse>
)

data class WorkSessionWithShiftInfoResponse(
    val id: String?,
    val userId: String,
    val shiftId: String,
    val clockInTime: OffsetDateTime,
    val clockOutTime: OffsetDateTime?,
    val totalMinutes: Int?,
    val status: WorkSessionStatus,
    val confirmed: Boolean,
    val confirmedBy: String?,
    val confirmedAt: OffsetDateTime?,
    val modifiedBy: String?,
    val originalClockInTime: OffsetDateTime?,
    val originalClockOutTime: OffsetDateTime?,
    val shiftStartTime: OffsetDateTime,
    val shiftEndTime: OffsetDateTime,
    val employeeId: String,
    val position: String?,
    val sessionNote: SessionNoteResponse?
) 