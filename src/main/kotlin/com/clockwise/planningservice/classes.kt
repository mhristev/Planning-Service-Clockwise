package com.clockwise.planningservice



import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

enum class ScheduleStatus {
    DRAFT, PUBLISHED, ARCHIVED
}

@Table("availabilities")
data class Availability(
    @Id
    val id: String? = null,
    @Column("employee_id")
    val employeeId: String,
    @Column("start_time")
    val startTime: LocalDateTime,
    @Column("end_time")
    val endTime: LocalDateTime,
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Table("schedules")
data class Schedule(
    @Id
    val id: String? = null,
    @Column("restaurant_id")
    val restaurantId: String,
    @Column("week_start")
    val weekStart: LocalDateTime,
    @Column("status")
    val status: ScheduleStatus = ScheduleStatus.DRAFT,
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Table("shifts")
data class Shift(
    @Id
    val id: String? = null,
    @Column("schedule_id")
    val scheduleId: String,
    @Column("employee_id")
    val employeeId: String,
    @Column("start_time")
    val startTime: LocalDateTime,
    @Column("end_time")
    val endTime: LocalDateTime,
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)