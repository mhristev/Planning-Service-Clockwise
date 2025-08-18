package com.clockwise.planningservice.listener

import com.clockwise.planningservice.dto.ScheduleConflictCheckRequest
import com.clockwise.planningservice.dto.SwapConflictCheckRequest
import com.clockwise.planningservice.service.ConflictCheckService
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class ConflictCheckListener(
    private val conflictCheckService: ConflictCheckService,
    private val objectMapper: ObjectMapper
) {
    
    @KafkaListener(
        topics = ["\${kafka.topic.schedule-conflict-check-request}"],
        groupId = "planning-service"
    )
    fun handleScheduleConflictCheckRequest(message: String) {
        try {
            logger.info { "Received schedule conflict check request: $message" }
            val request = objectMapper.readValue(message, ScheduleConflictCheckRequest::class.java)
            
            // Process asynchronously to avoid blocking the Kafka consumer
            CoroutineScope(Dispatchers.IO).launch {
                conflictCheckService.checkScheduleConflicts(request)
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to process schedule conflict check request: $message" }
        }
    }
    
    @KafkaListener(
        topics = ["\${kafka.topic.swap-conflict-check-request}"],
        groupId = "planning-service"
    )
    fun handleSwapConflictCheckRequest(message: String) {
        try {
            logger.info { "Received swap conflict check request: $message" }
            val request = objectMapper.readValue(message, SwapConflictCheckRequest::class.java)
            
            // Process asynchronously to avoid blocking the Kafka consumer
            CoroutineScope(Dispatchers.IO).launch {
                conflictCheckService.checkSwapConflicts(request)
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to process swap conflict check request: $message" }
        }
    }
}
