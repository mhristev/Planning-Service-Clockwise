package com.clockwise.planningservice.services.workload

import com.clockwise.planningservice.dto.workload.WorkHoursResponse
import com.clockwise.planningservice.dto.workload.WorkSessionResponse
import com.clockwise.planningservice.domains.workload.WorkSession
import com.clockwise.planningservice.domains.workload.WorkSessionStatus
import com.clockwise.planningservice.repositories.workload.WorkSessionRepository
import com.clockwise.planningservice.repositories.ShiftRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class WorkSessionService(
    private val workSessionRepository: WorkSessionRepository,
    private val shiftRepository: ShiftRepository
) {

    suspend fun clockIn(userId: String, shiftId: String): WorkSessionResponse {
        // Validate that the shift exists
        val shift = shiftRepository.findById(shiftId)
            ?: throw IllegalArgumentException("Shift with ID $shiftId does not exist")
        
        if (workSessionRepository.existsByShiftId(shiftId)) {
            throw IllegalStateException("A work session for shift $shiftId already exists")
        }
        
        // Check if there's already an active session for this user and shift
        val existingSession = workSessionRepository.findActiveByUserIdAndShiftId(userId, shiftId)
        
        if (existingSession != null) {
            throw IllegalStateException("User $userId already has an active work session for shift $shiftId")
        }
        
        // Create new work session
        val workSession = WorkSession(
            id = null,
            userId = userId,
            shiftId = shiftId,
            clockInTime = OffsetDateTime.now(),
            clockOutTime = null,
            totalMinutes = null,
            status = WorkSessionStatus.ACTIVE
        )
        
        val savedSession = workSessionRepository.save(workSession)
        return toWorkSessionResponse(savedSession)
    }

    suspend fun clockOut(userId: String, shiftId: String): WorkSessionResponse {
        // Validate that the shift exists
        val shift = shiftRepository.findById(shiftId)
            ?: throw IllegalArgumentException("Shift with ID $shiftId does not exist")
            
        val session = workSessionRepository.findActiveByUserIdAndShiftId(userId, shiftId)
            ?: throw IllegalStateException("No active work session found for user $userId and shift $shiftId")
        
        val clockOutTime = OffsetDateTime.now()
        val totalMinutes = java.time.Duration.between(session.clockInTime, clockOutTime).toMinutes().toInt()
        
        val updatedSession = session.copy(
            clockOutTime = clockOutTime,
            totalMinutes = totalMinutes,
            status = WorkSessionStatus.COMPLETED
        )
        
        val savedSession = workSessionRepository.save(updatedSession)
        return toWorkSessionResponse(savedSession)
    }

    suspend fun getEmployeeWorkHours(
        userId: String, 
        startDate: OffsetDateTime, 
        endDate: OffsetDateTime
    ): WorkHoursResponse {
        val sessions = workSessionRepository.findByUserIdAndClockInTimeBetween(userId, startDate, endDate)
            .toList()
        
        val sessionResponses = sessions.map { toWorkSessionResponse(it) }
        val totalMinutes = sessions.sumOf { it.totalMinutes ?: 0 }
        
        return WorkHoursResponse(
            userId = userId,
            totalSessions = sessions.size,
            totalMinutesWorked = totalMinutes,
            sessions = sessionResponses
        )
    }

    private fun toWorkSessionResponse(workSession: WorkSession): WorkSessionResponse {
        return WorkSessionResponse(
            id = workSession.id,
            userId = workSession.userId,
            shiftId = workSession.shiftId,
            clockInTime = workSession.clockInTime,
            clockOutTime = workSession.clockOutTime,
            totalMinutes = workSession.totalMinutes,
            status = workSession.status
        )
    }
} 