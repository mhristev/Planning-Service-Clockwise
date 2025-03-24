package com.clockwise.planningservice.dto

import com.clockwise.planningservice.domain.ScheduleStatus
import java.time.LocalDateTime

data class ScheduleRequest(
    val restaurantId: String?,
    val weekStart: LocalDateTime?
)

data class ScheduleResponse(
    val id: String,
    val restaurantId: String,
    val weekStart: LocalDateTime,
    val status: ScheduleStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) 