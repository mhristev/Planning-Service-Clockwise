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
    val status: WorkSessionStatus
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