package com.clockwise.planningservice.listener

import com.clockwise.planningservice.domains.Schedule
import com.clockwise.planningservice.dto.ShiftResponse
import com.clockwise.planningservice.service.NotificationService
import com.clockwise.planningservice.service.UserInfo
import com.clockwise.planningservice.service.UserWithShiftInfo
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Response DTO for users by business unit request
 */
data class UsersByBusinessUnitResponse(
    val businessUnitId: String,
    val correlationId: String,
    val users: List<UserInfoDto>
)

data class UserInfoDto(
    val id: String,
    val fcmToken: String?,
    val firstName: String?,
    val lastName: String?,
    val role: String?
)

/**
 * Enum to differentiate between notification types
 */
enum class NotificationType {
    SCHEDULE_PUBLISHED
}

/**
 * Internal data class to track pending notifications
 */
private data class PendingNotification(
    val type: NotificationType,
    val schedule: Schedule,
    val userShifts: Map<String, List<ShiftResponse>> // userId -> list of shifts
)

/**
 * Listener for users by business unit responses from User Service
 */
@Component
class UsersByBusinessUnitResponseListener(
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {

    // Store pending notification requests by correlation ID
    private val pendingNotifications = ConcurrentHashMap<String, PendingNotification>()

    @KafkaListener(topics = ["\${kafka.topic.users-by-business-unit-response}"], groupId = "planning-service")
    fun handleUsersByBusinessUnitResponse(message: String) {
        try {
            logger.info { "Received users by business unit response: $message" }
            val response = objectMapper.readValue(message, UsersByBusinessUnitResponse::class.java)
            
            val pendingNotification = pendingNotifications.remove(response.correlationId)
            if (pendingNotification == null) {
                logger.warn { "No pending notification found for correlation ID: ${response.correlationId}" }
                return
            }

            // Convert DTOs to domain objects
            val users = response.users.map { userDto ->
                UserInfo(
                    id = userDto.id,
                    fcmToken = userDto.fcmToken,
                    firstName = userDto.firstName,
                    lastName = userDto.lastName,
                    role = userDto.role
                )
            }

            logger.info { "Processing notification for ${users.size} users in business unit ${response.businessUnitId}" }

            // Send notifications asynchronously
            runBlocking {
                try {
                    when (pendingNotification.type) {
                        NotificationType.SCHEDULE_PUBLISHED -> {
                            // Create individual UserWithShiftInfo objects for each shift
                            val usersWithShifts = mutableListOf<UserWithShiftInfo>()
                            
                            users.forEach { user ->
                                val userShifts = pendingNotification.userShifts[user.id]
                                if (!userShifts.isNullOrEmpty()) {
                                    // Create a separate UserWithShiftInfo for each shift
                                    userShifts.forEach { shift ->
                                        usersWithShifts.add(UserWithShiftInfo(user, shift))
                                    }
                                }
                            }
                            
                            if (usersWithShifts.isNotEmpty()) {
                                logger.info { "Sending ${usersWithShifts.size} individual shift notifications for schedule ${pendingNotification.schedule.id}" }
                                notificationService.sendSchedulePublishedNotification(
                                    pendingNotification.schedule, 
                                    usersWithShifts
                                )
                            } else {
                                logger.info { "No users with shifts found for schedule ${pendingNotification.schedule.id}" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error sending notifications: ${e.message}" }
                }
            }

        } catch (e: Exception) {
            logger.error(e) { "Error processing users by business unit response: ${e.message}" }
        }
    }

    /**
     * Registers a pending schedule published notification that will be sent once users are received
     */
    fun registerPendingScheduleNotification(
        correlationId: String, 
        schedule: Schedule, 
        userShifts: Map<String, List<ShiftResponse>>
    ) {
        pendingNotifications[correlationId] = PendingNotification(
            type = NotificationType.SCHEDULE_PUBLISHED,
            schedule = schedule,
            userShifts = userShifts
        )
        logger.debug { "Registered pending schedule notification for correlation ID: $correlationId" }
    }

    /**
     * Gets the count of pending notifications (for monitoring/testing)
     */
    fun getPendingNotificationCount(): Int = pendingNotifications.size
}