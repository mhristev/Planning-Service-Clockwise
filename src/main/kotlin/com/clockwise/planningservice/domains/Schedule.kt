package com.clockwise.planningservice.domains

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

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