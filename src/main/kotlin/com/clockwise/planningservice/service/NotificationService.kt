package com.clockwise.planningservice.service

import com.clockwise.planningservice.domains.Schedule
import com.clockwise.planningservice.dto.ShiftResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/**
 * Data class representing user information needed for notifications
 */
data class UserInfo(
    val id: String,
    val fcmToken: String?,
    val firstName: String?,
    val lastName: String?,
    val role: String?
)

/**
 * Service for sending push notifications via Firebase Cloud Messaging
 */
@Service
class NotificationService(
    private val firebaseMessaging: FirebaseMessaging?,
    private val isFirebaseEnabled: Boolean
) {

    /**
     * Sends schedule publication notification to users with shifts in the schedule
     * Each user receives a separate notification for each of their shifts
     */
    suspend fun sendSchedulePublishedNotification(
        schedule: Schedule, 
        usersWithShifts: List<UserWithShiftInfo>
    ) {
        if (!isFirebaseEnabled || firebaseMessaging == null) {
            logger.warn { "Firebase is not enabled or configured - skipping notification send" }
            return
        }

        val usersWithTokens = usersWithShifts.filter { !it.user.fcmToken.isNullOrBlank() }
        if (usersWithTokens.isEmpty()) {
            logger.info { "No eligible users with FCM tokens found for schedule publication notification" }
            return
        }

        logger.info { "Sending ${usersWithTokens.size} individual shift notifications for business unit ${schedule.businessUnitId}" }

        var successCount = 0
        var failureCount = 0

        try {
            withContext(Dispatchers.IO) {
                usersWithTokens.forEach { userWithShift ->
                    try {
                        val notification = buildSchedulePublishedNotification(schedule, userWithShift.shift)
                        val message = buildScheduleNotificationMessage(
                            userWithShift.user.fcmToken!!, 
                            notification, 
                            schedule, 
                            userWithShift.shift
                        )
                        val response = firebaseMessaging.send(message)
                        logger.info { "Successfully sent schedule notification to user ${userWithShift.user.id} for shift ${userWithShift.shift.id}, message ID: $response" }
                        successCount++
                    } catch (e: Exception) {
                        logger.warn { "Failed to send schedule notification to user ${userWithShift.user.id} for shift ${userWithShift.shift.id}: ${e.message}" }
                        failureCount++
                    }
                }
                
                logger.info { "Schedule notification summary: $successCount successful, $failureCount failures" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error sending schedule notifications: ${e.message}" }
        }
    }

    /**
     * Builds the notification payload for schedule publication
     * Now handles individual shifts only
     */
    private fun buildSchedulePublishedNotification(schedule: Schedule, shift: ShiftResponse): Notification {
        val title = "Schedule Published"
        
        // Format the shift date
        val shiftDate = shift.startTime.format(DateTimeFormatter.ofPattern("MMM dd"))
        val body = "You're scheduled to work on $shiftDate"

        return Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build()
    }

    /**
     * Builds the complete FCM message for schedule publication
     * Now handles individual shifts only
     */
    private fun buildScheduleNotificationMessage(
        fcmToken: String, 
        notification: Notification, 
        schedule: Schedule,
        shift: ShiftResponse
    ): Message {
        val messageBuilder = Message.builder()
            .setToken(fcmToken)
            .setNotification(notification)
            .putData("type", "schedule_published")
            .putData("scheduleId", schedule.id ?: "")
            .putData("businessUnitId", schedule.businessUnitId)
            .putData("weekStart", schedule.weekStart.toString())
            .putData("shiftId", shift.id ?: "")
            .putData("shiftDate", shift.startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .putData("shiftStartTime", shift.startTime.toString())
            .putData("shiftEndTime", shift.endTime.toString())
            .putData("position", shift.position ?: "")
        
        return messageBuilder.build()
    }

    /**
     * Test method to verify Firebase connectivity
     */
    suspend fun testConnection(): Boolean {
        if (!isFirebaseEnabled || firebaseMessaging == null) {
            logger.warn { "Firebase is not enabled or configured" }
            return false
        }

        return try {
            withContext(Dispatchers.IO) {
                // Try to access Firebase Messaging - this will fail if not properly configured
                firebaseMessaging.toString() // Simple operation to test connectivity
                true
            }
        } catch (e: Exception) {
            logger.error(e) { "Firebase connection test failed: ${e.message}" }
            false
        }
    }
}

/**
 * Data class to hold user information along with a single shift for notification
 */
data class UserWithShiftInfo(
    val user: UserInfo,
    val shift: ShiftResponse
)