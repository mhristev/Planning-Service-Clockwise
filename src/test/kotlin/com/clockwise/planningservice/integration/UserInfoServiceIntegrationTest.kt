package com.clockwise.planningservice.integration

import com.clockwise.planningservice.dto.UserInfoRequest
import com.clockwise.planningservice.service.UserInfoService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.TestPropertySource

/**
 * Integration test for User Info Service Kafka messaging
 * Uses embedded Kafka to test the async communication
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["user-info-request", "user-info-response"])
@TestPropertySource(properties = [
    "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
    "kafka.topic.user-info-request=user-info-request",
    "kafka.topic.user-info-response=user-info-response"
])
class UserInfoServiceIntegrationTest {

    @Autowired
    private lateinit var userInfoService: UserInfoService

    @Autowired 
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should send user info request message successfully`() {
        // Given
        val userId = "test-user-123"
        val shiftId = "test-shift-456"

        // When - This should not throw any exceptions
        userInfoService.requestUserInfo(userId, shiftId)

        // Then - If we get here without exception, the Kafka send was successful
        // In a real test, we would also verify the message was received
        // For now, we're just testing that the service doesn't crash
    }
}
