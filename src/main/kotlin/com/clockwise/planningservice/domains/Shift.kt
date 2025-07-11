package com.clockwise.planningservice.domains

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime
import java.time.ZoneId

@Table("shifts")
data class Shift(
    @Id
    val id: String? = null,
    @Column("schedule_id")
    val scheduleId: String,
    @Column("employee_id")
    val employeeId: String,
    @Column("start_time")
    val startTime: ZonedDateTime,
    @Column("end_time")
    val endTime: ZonedDateTime,
    @Column("position")
    val position: String? = null,
    @Column("employee_first_name")
    val employeeFirstName: String? = null,
    @Column("employee_last_name")
    val employeeLastName: String? = null,
    @Column("created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
    @Column("updated_at")
    val updatedAt: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))
) 