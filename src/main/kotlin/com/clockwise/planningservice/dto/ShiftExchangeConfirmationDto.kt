package com.clockwise.planningservice.dto

import java.time.OffsetDateTime

data class ShiftExchangeConfirmationDto(
    val requestId: String,
    val exchangeShiftId: String,
    val originalShiftId: String,
    val posterUserId: String,
    val requesterUserId: String,
    val requestType: RequestType,
    val swapShiftId: String? = null,
    val businessUnitId: String,
    val status: String, // SUCCESS, FAILED
    val message: String? = null,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)
