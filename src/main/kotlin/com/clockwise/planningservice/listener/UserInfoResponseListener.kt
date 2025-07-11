package com.clockwise.planningservice.listener

import com.clockwise.planningservice.dto.UserInfoResponse
import com.clockwise.planningservice.repositories.ShiftRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Listener for user info responses from User Service
 * Updates shift records with employee names when received
 */
@Component
class UserInfoResponseListener(
    private val shiftRepository: ShiftRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(UserInfoResponseListener::class.java)

    @KafkaListener(
        topics = ["\${kafka.topic.user-info-response}"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    suspend fun handleUserInfoResponse(message: String, ack: Acknowledgment) {
        try {
            logger.info("Received user info response message: {}", message)
            
            val response = objectMapper.readValue(message, UserInfoResponse::class.java)
            logger.info("Processing user info response for userId: {}, shiftId: {}, requestId: {}", 
                       response.userId, response.shiftId, response.requestId)
            
            if (response.errorMessage != null) {
                logger.warn("User info response contains error for userId: {}, shiftId: {}, error: {}", 
                           response.userId, response.shiftId, response.errorMessage)
                ack.acknowledge()
                return
            }
            
            // Update shift with employee names
            val shift = shiftRepository.findById(response.shiftId)
            if (shift != null) {
                val updatedShift = shift.copy(
                    employeeFirstName = response.firstName,
                    employeeLastName = response.lastName
                )
                
                shiftRepository.save(updatedShift)
                logger.info("Successfully updated shift {} with employee names: {} {}", 
                           response.shiftId, response.firstName, response.lastName)
            } else {
                logger.warn("Shift with ID {} not found when trying to update employee names", response.shiftId)
            }
            
            ack.acknowledge()
            logger.info("Successfully processed user info response for shiftId: {}", response.shiftId)
            
        } catch (e: Exception) {
            logger.error("Error processing user info response message: {}", message, e)
            throw e
        }
    }
}
