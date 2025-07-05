package com.clockwise.planningservice.domains.workload

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("work_sessions")
data class WorkSession(
    @Id
    val id: String? = null,
    val userId: String,
    val shiftId: String,
    val clockInTime: OffsetDateTime,
    val clockOutTime: OffsetDateTime? = null,
    val totalMinutes: Int? = null,
    val status: WorkSessionStatus = WorkSessionStatus.ACTIVE,
    val confirmed: Boolean = false,
    val confirmedBy: String? = null,
    val confirmedAt: OffsetDateTime? = null,
    val modifiedBy: String? = null,
    val originalClockInTime: OffsetDateTime? = null,
    val originalClockOutTime: OffsetDateTime? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)

enum class WorkSessionStatus {
    ACTIVE, COMPLETED, CANCELLED
} 