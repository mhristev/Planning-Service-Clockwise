package com.clockwise.planningservice.dto

/**
 * Request for user information from User Service
 * Sent by Planning Service to request employee name details
 */
data class UserInfoRequest(
    val requestId: String,
    val userId: String,
    val shiftId: String
)

/**
 * Response containing user information from User Service
 * Contains employee name details or error information
 */
data class UserInfoResponse(
    val requestId: String,
    val userId: String,
    val shiftId: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val errorMessage: String? = null
)
