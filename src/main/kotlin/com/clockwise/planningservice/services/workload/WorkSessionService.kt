package com.clockwise.planningservice.services.workload

import com.clockwise.planningservice.dto.workload.WorkHoursResponse
import com.clockwise.planningservice.dto.workload.WorkSessionResponse
import com.clockwise.planningservice.domains.workload.WorkSession
import com.clockwise.planningservice.domains.workload.WorkSessionStatus
import com.clockwise.planningservice.repositories.workload.WorkSessionRepository
import com.clockwise.planningservice.repositories.ShiftRepository
import com.clockwise.planningservice.utils.toOffsetDateTime
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class WorkSessionService(
    private val workSessionRepository: WorkSessionRepository,
    private val shiftRepository: ShiftRepository,
    private val sessionNoteService: SessionNoteService
) {

    /**
     * Automatically create a work session for a shift
     * This is called when a shift is created to ensure every shift has a work session
     */
    suspend fun createWorkSessionForShift(shiftId: String, employeeId: String): WorkSession {
        // Check if work session already exists
        val existingSession = workSessionRepository.findByShiftId(shiftId)
        if (existingSession != null) {
            return existingSession
        }

        // Get the shift to determine the default clock in/out times
        val shift = shiftRepository.findById(shiftId)
            ?: throw IllegalArgumentException("Shift with ID $shiftId does not exist")

        // Create a new work session with null values
        val workSession = WorkSession(
            userId = employeeId,
            shiftId = shiftId,
            clockInTime = null,
            clockOutTime = null,
            totalMinutes = null,
            status = WorkSessionStatus.CREATED,
            confirmed = false,
            originalClockInTime = null,
            originalClockOutTime = null
        )

        return workSessionRepository.save(workSession)
    }

    /**
     * Confirm a work session
     */
    suspend fun confirmWorkSession(workSessionId: String, confirmedBy: String): WorkSessionResponse {
        val workSession = workSessionRepository.findById(workSessionId)
            ?: throw IllegalArgumentException("Work session not found with ID: $workSessionId")
        
        val confirmedSession = workSession.copy(
            confirmed = true,
            confirmedBy = confirmedBy,
            confirmedAt = OffsetDateTime.now()
        )
        
        workSessionRepository.save(confirmedSession)
        return toWorkSessionResponse(confirmedSession)
    }

    /**
     * Modify work session times
     */
    suspend fun modifyWorkSession(
        workSessionId: String,
        newClockInTime: OffsetDateTime,
        newClockOutTime: OffsetDateTime?,
        modifiedBy: String
    ): WorkSessionResponse {
        val workSession = workSessionRepository.findById(workSessionId)
            ?: throw IllegalArgumentException("Work session not found with ID: $workSessionId")
        
        val totalMinutes = if (newClockOutTime != null) {
            ChronoUnit.MINUTES.between(newClockInTime, newClockOutTime).toInt()
        } else {
            null
        }

        val newStatus = if (newClockOutTime != null) WorkSessionStatus.COMPLETED else WorkSessionStatus.ACTIVE

        val modifiedSession = workSession.copy(
            clockInTime = newClockInTime,
            clockOutTime = newClockOutTime,
            totalMinutes = totalMinutes,
            status = newStatus,
            modifiedBy = modifiedBy,
            originalClockInTime = workSession.originalClockInTime ?: workSession.clockInTime,
            originalClockOutTime = workSession.originalClockOutTime ?: workSession.clockOutTime
        )
        
        workSessionRepository.save(modifiedSession)
        return toWorkSessionResponse(modifiedSession)
    }

    /**
     * Modify and confirm work session in one operation
     */
    suspend fun modifyAndConfirmWorkSession(
        workSessionId: String,
        newClockInTime: OffsetDateTime,
        newClockOutTime: OffsetDateTime?,
        modifiedBy: String
    ): WorkSessionResponse {
        val workSession = workSessionRepository.findById(workSessionId)
            ?: throw IllegalArgumentException("Work session not found with ID: $workSessionId")
        
        val totalMinutes = if (newClockOutTime != null) {
            ChronoUnit.MINUTES.between(newClockInTime, newClockOutTime).toInt()
        } else {
            null
        }

        val newStatus = if (newClockOutTime != null) WorkSessionStatus.COMPLETED else WorkSessionStatus.ACTIVE
        
        val modifiedAndConfirmedSession = workSession.copy(
            clockInTime = newClockInTime,
            clockOutTime = newClockOutTime,
            totalMinutes = totalMinutes,
            status = newStatus,
            modifiedBy = modifiedBy,
            originalClockInTime = workSession.originalClockInTime ?: workSession.clockInTime,
            originalClockOutTime = workSession.originalClockOutTime ?: workSession.clockOutTime,
            confirmed = true,
            confirmedBy = modifiedBy,
            confirmedAt = OffsetDateTime.now()
        )
        
        workSessionRepository.save(modifiedAndConfirmedSession)
        return toWorkSessionResponse(modifiedAndConfirmedSession)
    }

    suspend fun clockIn(userId: String, shiftId: String): WorkSessionResponse {
        // Validate that the shift exists
        val shift = shiftRepository.findById(shiftId)
            ?: throw IllegalArgumentException("Shift with ID $shiftId does not exist")
        
        // Check if work session already exists and update it
        val existingSession = workSessionRepository.findByShiftId(shiftId)
        if (existingSession != null) {
            val updatedSession = existingSession.copy(
                clockInTime = OffsetDateTime.now(),
                status = WorkSessionStatus.ACTIVE,
                confirmed = false, // Reset confirmation when employee clocks in
                confirmedBy = null,
                confirmedAt = null,
                updatedAt = OffsetDateTime.now()
            )
            val savedSession = workSessionRepository.save(updatedSession)
            return toWorkSessionResponse(savedSession)
        }
        
        // Create new work session if none exists
        val workSession = WorkSession(
            id = null,
            userId = userId,
            shiftId = shiftId,
            clockInTime = OffsetDateTime.now(),
            clockOutTime = null,
            totalMinutes = null,
            status = WorkSessionStatus.ACTIVE,
            confirmed = false
        )
        
        val savedSession = workSessionRepository.save(workSession)
        return toWorkSessionResponse(savedSession)
    }

    suspend fun clockOut(userId: String, shiftId: String): WorkSessionResponse {
        // Validate that the shift exists
        val shift = shiftRepository.findById(shiftId)
            ?: throw IllegalArgumentException("Shift with ID $shiftId does not exist")
            
        val session = workSessionRepository.findByShiftId(shiftId)
            ?: throw IllegalStateException("No work session found for shift $shiftId")
        
        val clockOutTime = OffsetDateTime.now()
        val totalMinutes = if (session.clockInTime != null) {
            java.time.Duration.between(session.clockInTime, clockOutTime).toMinutes().toInt()
        } else {
            null
        }
        
        val updatedSession = session.copy(
            clockOutTime = clockOutTime,
            totalMinutes = totalMinutes,
            status = WorkSessionStatus.COMPLETED,
            confirmed = false, // Reset confirmation when employee clocks out
            confirmedBy = null,
            confirmedAt = null,
            updatedAt = OffsetDateTime.now()
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

    suspend fun getWorkSessionByShiftId(shiftId: String): WorkSession? {
        return workSessionRepository.findByShiftId(shiftId)
    }

    suspend fun getWorkSessionById(workSessionId: String): WorkSessionResponse {
        val workSession = workSessionRepository.findById(workSessionId)
            ?: throw IllegalArgumentException("Work session not found with ID: $workSessionId")
        return toWorkSessionResponse(workSession)
    }

    /**
     * Get all unconfirmed work sessions for a business unit with shift information
     */
    suspend fun getUnconfirmedWorkSessionsWithShiftInfo(businessUnitId: String): List<com.clockwise.planningservice.dto.workload.WorkSessionWithShiftInfoResponse> {
        val unconfirmedSessions = workSessionRepository.findUnconfirmedByBusinessUnitId(businessUnitId).toList()
        
        return unconfirmedSessions.mapNotNull { workSession ->
            val shift = shiftRepository.findById(workSession.shiftId)
            shift?.let { s ->
                val sessionNote = workSession.id?.let { sessionId ->
                    try {
                        sessionNoteService.getSessionNoteByWorkSessionId(sessionId)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                com.clockwise.planningservice.dto.workload.WorkSessionWithShiftInfoResponse(
                    id = workSession.id,
                    userId = workSession.userId,
                    shiftId = workSession.shiftId,
                    clockInTime = workSession.clockInTime,
                    clockOutTime = workSession.clockOutTime,
                    totalMinutes = workSession.totalMinutes,
                    status = workSession.status,
                    confirmed = workSession.confirmed,
                    confirmedBy = workSession.confirmedBy,
                    confirmedAt = workSession.confirmedAt,
                    modifiedBy = workSession.modifiedBy,
                    originalClockInTime = workSession.originalClockInTime,
                    originalClockOutTime = workSession.originalClockOutTime,
                    shiftStartTime = s.startTime.toOffsetDateTime(),
                    shiftEndTime = s.endTime.toOffsetDateTime(),
                    employeeId = s.employeeId,
                    position = s.position,
                    sessionNote = sessionNoteService.toResponse(sessionNote)
                )
            }
        }
    }

    /**
     * Get all unconfirmed work sessions for a business unit
     */
    suspend fun getUnconfirmedWorkSessions(businessUnitId: String): List<WorkSession> {
        return workSessionRepository.findUnconfirmedByBusinessUnitId(businessUnitId).toList()
    }

    private fun toWorkSessionResponse(workSession: WorkSession): WorkSessionResponse {
        return WorkSessionResponse(
            id = workSession.id,
            userId = workSession.userId,
            shiftId = workSession.shiftId,
            clockInTime = workSession.clockInTime,
            clockOutTime = workSession.clockOutTime,
            totalMinutes = workSession.totalMinutes,
            status = workSession.status,
            confirmed = workSession.confirmed,
            confirmedBy = workSession.confirmedBy,
            confirmedAt = workSession.confirmedAt,
            modifiedBy = workSession.modifiedBy,
            originalClockInTime = workSession.originalClockInTime,
            originalClockOutTime = workSession.originalClockOutTime
        )
    }
}