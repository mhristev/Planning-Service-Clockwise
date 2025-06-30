package com.clockwise.planningservice.domains

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime
import java.time.ZoneId

@Table("schedules")
data class Schedule(
    @Id
    val id: String? = null,
    @Column("business_unit_id")
    val businessUnitId: String,
    @Column("week_start")
    val weekStart: ZonedDateTime,
    @Column("status")
    val status: String,
    @Column("created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
    @Column("updated_at")
    val updatedAt: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))
) 