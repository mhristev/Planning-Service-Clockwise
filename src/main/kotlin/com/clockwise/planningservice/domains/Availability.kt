package com.clockwise.planningservice.domains

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

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
    @Column("business_unit_id")
    val businessUnitId: String? = null,
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
) 