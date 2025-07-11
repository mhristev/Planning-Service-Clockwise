package com.clockwise.planningservice.service

import com.clockwise.planningservice.dto.UserInfoRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service for requesting user information from User Service via Kafka
 * Follows existing Kafka patterns from organization service
 */
@Service
class UserInfoService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(UserInfoService::class.java)

    @Value("\${kafka.topic.user-info-request}")
    private lateinit var userInfoRequestTopic: String

    /**
     * Request user information (first and last name) for a specific shift
     * This is an async fire-and-forget operation - response will be handled by listener
     * 
     * @param userId The user ID to get information for
     * @param shiftId The shift ID that needs employee name population
     */
    fun requestUserInfo(userId: String, shiftId: String) {
        try {
            val requestId = UUID.randomUUID().toString()
            val request = UserInfoRequest(
                requestId = requestId,
                userId = userId,
                shiftId = shiftId
            )
            val message = objectMapper.writeValueAsString(request)
            
            logger.info("Requesting user info for userId: {}, shiftId: {}, requestId: {}", userId, shiftId, requestId)
            kafkaTemplate.send(userInfoRequestTopic, userId, message)
            logger.debug("User info request sent successfully")
            
        } catch (e: Exception) {
            logger.error("Error sending user info request for userId: {}, shiftId: {}", userId, shiftId, e)
            // Don't rethrow - this is async and shouldn't break shift creation
        }
    }
}
