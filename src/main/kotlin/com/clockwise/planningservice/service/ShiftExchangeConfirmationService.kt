package com.clockwise.planningservice.service

import com.clockwise.planningservice.dto.ShiftExchangeConfirmationDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class ShiftExchangeConfirmationService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ShiftExchangeConfirmationService::class.java)

    @Value("\${kafka.topic.shift-exchange-confirmations}")
    private lateinit var shiftExchangeConfirmationsTopic: String
    
    fun sendSuccessConfirmation(confirmation: ShiftExchangeConfirmationDto) {
        sendConfirmation(confirmation.copy(status = "SUCCESS"))
    }
    
    fun sendFailureConfirmation(confirmation: ShiftExchangeConfirmationDto, errorMessage: String) {
        sendConfirmation(confirmation.copy(status = "FAILED", message = errorMessage))
    }
    
    private fun sendConfirmation(confirmation: ShiftExchangeConfirmationDto) {
        try {
            val confirmationJson = objectMapper.writeValueAsString(confirmation)
            logger.info("Sending shift exchange confirmation to topic {}: {}", shiftExchangeConfirmationsTopic, confirmationJson)
            
            kafkaTemplate.send(shiftExchangeConfirmationsTopic, confirmation.requestId, confirmationJson)
            
            logger.info("Successfully sent shift exchange confirmation for request: {}", confirmation.requestId)
            
        } catch (e: Exception) {
            logger.error("Failed to send shift exchange confirmation for request: {}", confirmation.requestId, e)
        }
    }
}
