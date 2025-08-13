package com.clockwise.planningservice.listener

import com.clockwise.planningservice.dto.ShiftExchangeEventDto
import com.clockwise.planningservice.dto.ShiftExchangeConfirmationDto
import com.clockwise.planningservice.dto.RequestType
import com.clockwise.planningservice.services.ShiftService
import com.clockwise.planningservice.service.ShiftExchangeConfirmationService
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Listener for shift exchange events from Collaboration Service
 * Handles shift swaps and take requests when approved by managers
 */
@Component
class ShiftExchangeListener(
    private val shiftService: ShiftService,
    private val shiftExchangeConfirmationService: ShiftExchangeConfirmationService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ShiftExchangeListener::class.java)

    @KafkaListener(
        topics = ["\${kafka.topic.shift-exchange-approval:shift-exchange-approval}"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    fun handleShiftExchangeEvent(message: String, ack: Acknowledgment) {
        try {
            logger.info("Received shift exchange event: {}", message)
            
            val event = objectMapper.readValue(message, ShiftExchangeEventDto::class.java)
            logger.info("Processing shift exchange event - Type: {}, Status: {}, Original Shift: {}, Requester: {}", 
                       event.requestType, event.status, event.originalShiftId, event.requesterUserId)
            
            // Only process APPROVED events
            if (event.status != "APPROVED") {
                logger.info("Ignoring non-approved event with status: {}", event.status)
                ack.acknowledge()
                return
            }
            
            // Process based on request type using runBlocking for suspend functions
            runBlocking {
                try {
                    when (event.requestType) {
                        RequestType.TAKE_SHIFT -> {
                            handleTakeShiftApproval(event)
                        }
                        RequestType.SWAP_SHIFT -> {
                            handleSwapShiftApproval(event)
                        }
                    }
                    
                    // Send success confirmation to Collaboration Service
                    val confirmation = ShiftExchangeConfirmationDto(
                        requestId = event.requestId,
                        exchangeShiftId = event.exchangeShiftId,
                        originalShiftId = event.originalShiftId,
                        posterUserId = event.posterUserId,
                        requesterUserId = event.requesterUserId,
                        requestType = event.requestType,
                        swapShiftId = event.swapShiftId,
                        businessUnitId = event.businessUnitId,
                        status = "SUCCESS"
                    )
                    shiftExchangeConfirmationService.sendSuccessConfirmation(confirmation)
                    
                } catch (e: Exception) {
                    logger.error("Error processing shift exchange, sending failure confirmation", e)
                    
                    // Send failure confirmation to Collaboration Service
                    val confirmation = ShiftExchangeConfirmationDto(
                        requestId = event.requestId,
                        exchangeShiftId = event.exchangeShiftId,
                        originalShiftId = event.originalShiftId,
                        posterUserId = event.posterUserId,
                        requesterUserId = event.requesterUserId,
                        requestType = event.requestType,
                        swapShiftId = event.swapShiftId,
                        businessUnitId = event.businessUnitId,
                        status = "FAILED"
                    )
                    shiftExchangeConfirmationService.sendFailureConfirmation(confirmation, e.message ?: "Unknown error")
                    throw e
                }
            }
            
            ack.acknowledge()
            logger.info("Successfully processed shift exchange event for original shift: {}", event.originalShiftId)
            
        } catch (e: Exception) {
            logger.error("Error processing shift exchange event: {}", message, e)
            throw e
        }
    }
    
    private suspend fun handleTakeShiftApproval(event: ShiftExchangeEventDto) {
        logger.info("Processing TAKE_SHIFT approval - changing owner of shift {} from {} to {}", 
                   event.originalShiftId, event.posterUserId, event.requesterUserId)
        
        try {
            // Update the shift to assign it to the new user (requester) using Keycloak ID
            val updated = shiftService.reassignShiftWithKeycloakId(
                shiftId = event.originalShiftId,
                keycloakUserId = event.requesterUserId
            )
            
            if (updated) {
                logger.info("Successfully reassigned shift {} to user {}", 
                           event.originalShiftId, event.requesterUserId)
            } else {
                logger.warn("Failed to reassign shift {} - shift may not exist", event.originalShiftId)
            }
            
        } catch (e: Exception) {
            logger.error("Error reassigning shift {} to user {}: {}", 
                        event.originalShiftId, event.requesterUserId, e.message, e)
            throw e
        }
    }
    
    private suspend fun handleSwapShiftApproval(event: ShiftExchangeEventDto) {
        if (event.swapShiftId == null) {
            logger.error("SWAP_SHIFT event missing swapShiftId - cannot process swap")
            throw IllegalArgumentException("swapShiftId is required for SWAP_SHIFT events")
        }
        
        logger.info("Processing SWAP_SHIFT approval - swapping shifts {} and {} between users {} and {}", 
                   event.originalShiftId, event.swapShiftId, 
                   event.posterUserId, event.requesterUserId)
        
        try {
            // Swap the two shifts between the users using Keycloak IDs
            val swapped = shiftService.swapShiftsWithKeycloakIds(
                shift1Id = event.originalShiftId,
                shift2Id = event.swapShiftId,
                keycloakUser1Id = event.posterUserId,
                keycloakUser2Id = event.requesterUserId
            )
            
            if (swapped) {
                logger.info("Successfully swapped shifts {} and {} between users {} and {}", 
                           event.originalShiftId, event.swapShiftId,
                           event.posterUserId, event.requesterUserId)
            } else {
                logger.warn("Failed to swap shifts {} and {} - one or both shifts may not exist", 
                           event.originalShiftId, event.swapShiftId)
            }
            
        } catch (e: Exception) {
            logger.error("Error swapping shifts {} and {} between users {} and {}: {}", 
                        event.originalShiftId, event.swapShiftId,
                        event.posterUserId, event.requesterUserId, e.message, e)
            throw e
        }
    }
}