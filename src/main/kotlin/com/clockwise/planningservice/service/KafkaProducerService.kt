package com.clockwise.planningservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Service
class KafkaProducerService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    
    @Value("\${kafka.topic.users-by-business-unit-request}")
    private lateinit var usersRequestTopic: String
    
    /**
     * Requests users by business unit ID from the User Service
     */
    fun requestUsersByBusinessUnitId(businessUnitId: String, correlationId: String): Mono<Void> {
        return Mono.fromCallable {
            val request = UsersByBusinessUnitRequest(
                businessUnitId = businessUnitId,
                correlationId = correlationId
            )
            val requestJson = objectMapper.writeValueAsString(request)
            logger.info { "Requesting users for business unit $businessUnitId with correlation ID $correlationId" }
            
            kafkaTemplate.send(usersRequestTopic, businessUnitId, requestJson)
        }
        .doOnSuccess { 
            logger.info { "Successfully sent users request for business unit $businessUnitId" }
        }
        .doOnError { error ->
            logger.error(error) { "Failed to send users request for business unit $businessUnitId" }
        }
        .then()
    }
}

/**
 * Request DTO for fetching users by business unit
 */
data class UsersByBusinessUnitRequest(
    val businessUnitId: String,
    val correlationId: String
)