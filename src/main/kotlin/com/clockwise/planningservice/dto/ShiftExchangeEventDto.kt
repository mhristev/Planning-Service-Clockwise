package com.clockwise.planningservice.dto

import java.time.OffsetDateTime

data class ShiftExchangeEventDto(
    val requestId: String,
    val exchangeShiftId: String,
    val originalShiftId: String,
    val posterUserId: String,
    val requesterUserId: String,
    val requestType: RequestType,
    val swapShiftId: String? = null,
    val businessUnitId: String,
    val status: String, // APPROVED or REJECTED
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)

enum class RequestType {
    TAKE_SHIFT,
    SWAP_SHIFT
}