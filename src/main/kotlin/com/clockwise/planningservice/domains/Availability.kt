package com.clockwise.planningservice.domains

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.ZonedDateTime
import java.time.ZoneId

@Table("availabilities")
data class Availability(
    @Id
    val id: String? = null,
    @Column("employee_id")
    val employeeId: String,
    @Column("start_time")
    val startTime: ZonedDateTime,
    @Column("end_time")
    val endTime: ZonedDateTime,
    @Column("business_unit_id")
    val businessUnitId: String? = null,
    @Column("created_at")
    val createdAt: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
    @Column("updated_at")
    val updatedAt: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))
) 