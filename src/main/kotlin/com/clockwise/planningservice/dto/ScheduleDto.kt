package com.clockwise.planningservice.dto

import java.time.ZonedDateTime

data class ScheduleRequest(
    val restaurantId: String?,
    val weekStart: ZonedDateTime?,
    val status: String? = "DRAFT"
)

data class ScheduleResponse(
    val id: String,
    val restaurantId: String,
    val weekStart: ZonedDateTime,
    val status: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
) 