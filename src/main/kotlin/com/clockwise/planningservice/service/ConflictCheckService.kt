package com.clockwise.planningservice.service

import com.clockwise.planningservice.dto.*
import com.clockwise.planningservice.repositories.ShiftRepository
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneId
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@Service
class ConflictCheckService(
    private val shiftRepository: ShiftRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    
    @Value("\${kafka.topic.schedule-conflict-check-response}")
    private lateinit var scheduleConflictCheckResponseTopic: String
    
    @Value("\${kafka.topic.swap-conflict-check-response}")
    private lateinit var swapConflictCheckResponseTopic: String
    
    /**
     * Check if a user has schedule conflicts for the given time period
     */
    suspend fun checkScheduleConflicts(request: ScheduleConflictCheckRequest) {
        logger.info { "Checking schedule conflicts for user ${request.userId} from ${request.startTime} to ${request.endTime}" }
        
        try {
            // Convert OffsetDateTime to ZonedDateTime for database query
            val startTime = request.startTime.atZoneSameInstant(ZoneId.of("UTC"))
            val endTime = request.endTime.atZoneSameInstant(ZoneId.of("UTC"))
            
            // Find overlapping shifts for the user
            val conflictingShifts = shiftRepository.findOverlappingShiftsForUser(
                request.userId, 
                startTime, 
                endTime
            ).toList()
            
            val hasConflict = conflictingShifts.isNotEmpty()
            val conflictingShiftIds = conflictingShifts.map { it.id!! }
            
            logger.info { "Conflict check result for user ${request.userId}: hasConflict=$hasConflict, conflictingShifts=${conflictingShiftIds.size}" }
            
            val response = ScheduleConflictCheckResponse(
                userId = request.userId,
                startTime = request.startTime,
                endTime = request.endTime,
                hasConflict = hasConflict,
                conflictingShiftIds = conflictingShiftIds,
                correlationId = request.correlationId
            )
            
            sendScheduleConflictCheckResponse(response)
            
        } catch (e: Exception) {
            logger.error(e) { "Error checking schedule conflicts for user ${request.userId}" }
            
            // Send error response (assume conflict exists to be safe)
            val response = ScheduleConflictCheckResponse(
                userId = request.userId,
                startTime = request.startTime,
                endTime = request.endTime,
                hasConflict = true, // Conservative approach - assume conflict if error
                conflictingShiftIds = emptyList(),
                correlationId = request.correlationId
            )
            
            sendScheduleConflictCheckResponse(response)
        }
    }
    
    /**
     * Check if a swap operation would create conflicts for both users
     */
    suspend fun checkSwapConflicts(request: SwapConflictCheckRequest) {
        logger.info { "Checking swap conflicts between users ${request.posterUserId} and ${request.requesterUserId}" }
        
        try {
            // Get the original shift (poster's shift being exchanged)
            val originalShift = shiftRepository.findById(request.originalShiftId)
            if (originalShift == null) {
                logger.error { "Original shift ${request.originalShiftId} not found" }
                sendSwapErrorResponse(request, "Original shift not found")
                return
            }
            
            // Get the swap shift (requester's shift being offered)
            val swapShift = shiftRepository.findById(request.swapShiftId)
            if (swapShift == null) {
                logger.error { "Swap shift ${request.swapShiftId} not found" }
                sendSwapErrorResponse(request, "Swap shift not found")
                return
            }
            
            // Check if poster would have conflicts if taking the swap shift
            val posterConflictingShifts = shiftRepository.findOverlappingShiftsForUser(
                request.posterUserId,
                swapShift.startTime,
                swapShift.endTime
            ).filter { it.id != originalShift.id } // Exclude the original shift being swapped
            .toList()
            
            // Check if requester would have conflicts if taking the original shift
            val requesterConflictingShifts = shiftRepository.findOverlappingShiftsForUser(
                request.requesterUserId,
                originalShift.startTime,
                originalShift.endTime
            ).filter { it.id != swapShift.id } // Exclude the swap shift being given up
            .toList()
            
            val posterHasConflict = posterConflictingShifts.isNotEmpty()
            val requesterHasConflict = requesterConflictingShifts.isNotEmpty()
            val isSwapPossible = !posterHasConflict && !requesterHasConflict
            
            logger.info { "Swap conflict check result: posterHasConflict=$posterHasConflict, requesterHasConflict=$requesterHasConflict, isSwapPossible=$isSwapPossible" }
            
            val response = SwapConflictCheckResponse(
                posterUserId = request.posterUserId,
                requesterUserId = request.requesterUserId,
                originalShiftId = request.originalShiftId,
                swapShiftId = request.swapShiftId,
                posterHasConflict = posterHasConflict,
                requesterHasConflict = requesterHasConflict,
                posterConflictingShiftIds = posterConflictingShifts.map { it.id!! },
                requesterConflictingShiftIds = requesterConflictingShifts.map { it.id!! },
                isSwapPossible = isSwapPossible,
                correlationId = request.correlationId
            )
            
            sendSwapConflictCheckResponse(response)
            
        } catch (e: Exception) {
            logger.error(e) { "Error checking swap conflicts" }
            sendSwapErrorResponse(request, e.message ?: "Unknown error")
        }
    }
    
    private fun sendScheduleConflictCheckResponse(response: ScheduleConflictCheckResponse) {
        try {
            val responseJson = objectMapper.writeValueAsString(response)
            logger.debug { "Sending schedule conflict check response: $responseJson" }
            kafkaTemplate.send(scheduleConflictCheckResponseTopic, response.correlationId, responseJson)
        } catch (e: Exception) {
            logger.error(e) { "Failed to send schedule conflict check response" }
        }
    }
    
    private fun sendSwapConflictCheckResponse(response: SwapConflictCheckResponse) {
        try {
            val responseJson = objectMapper.writeValueAsString(response)
            logger.debug { "Sending swap conflict check response: $responseJson" }
            kafkaTemplate.send(swapConflictCheckResponseTopic, response.correlationId, responseJson)
        } catch (e: Exception) {
            logger.error(e) { "Failed to send swap conflict check response" }
        }
    }
    
    private fun sendSwapErrorResponse(request: SwapConflictCheckRequest, errorMessage: String) {
        val response = SwapConflictCheckResponse(
            posterUserId = request.posterUserId,
            requesterUserId = request.requesterUserId,
            originalShiftId = request.originalShiftId,
            swapShiftId = request.swapShiftId,
            posterHasConflict = true, // Conservative approach
            requesterHasConflict = true, // Conservative approach
            posterConflictingShiftIds = emptyList(),
            requesterConflictingShiftIds = emptyList(),
            isSwapPossible = false,
            correlationId = request.correlationId
        )
        
        sendSwapConflictCheckResponse(response)
    }
}
