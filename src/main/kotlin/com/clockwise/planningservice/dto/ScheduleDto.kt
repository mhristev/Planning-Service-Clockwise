package com.clockwise.planningservice.dto

import java.time.LocalDateTime

data class ScheduleRequest(
    val restaurantId: String?,
    val weekStart: LocalDateTime?
)

data class ScheduleResponse(
    val id: String,
    val restaurantId: String,
    val weekStart: LocalDateTime,
    val status: com.clockwise.planningservice.domains.ScheduleStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) 