package com.clockwise.planningservice.services

import com.clockwise.planningservice.ResourceNotFoundException
import com.clockwise.planningservice.domains.Shift
import com.clockwise.planningservice.dto.ShiftRequest
import com.clockwise.planningservice.dto.ShiftResponse
import com.clockwise.planningservice.dto.ShiftWithWorkSessionResponse
import com.clockwise.planningservice.dto.WorkSessionWithNoteResponse
import com.clockwise.planningservice.repositories.ScheduleRepository
import com.clockwise.planningservice.repositories.ShiftRepository
import com.clockwise.planningservice.repositories.workload.WorkSessionRepository
import com.clockwise.planningservice.repositories.workload.SessionNoteRepository
import com.clockwise.planningservice.services.workload.SessionNoteService
import com.clockwise.planningservice.services.workload.WorkSessionService
import com.clockwise.planningservice.dto.workload.SessionNoteRequest
import com.clockwise.planningservice.utils.toOffsetDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.LocalDate

@Service
class ShiftService(
    private val shiftRepository: ShiftRepository,
    private val scheduleRepository: ScheduleRepository,
    private val workSessionRepository: WorkSessionRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionNoteService: SessionNoteService,
    private val workSessionService: WorkSessionService
) {

    suspend fun createShift(request: ShiftRequest): ShiftResponse {
        // Verify schedule exists and is in draft or published status
        val schedule = scheduleRepository.findById(request.scheduleId!!)
            ?: throw ResourceNotFoundException("Schedule not found with id: ${request.scheduleId}")

        if (schedule.status == "ARCHIVED") {
            throw IllegalStateException("Cannot add shifts to an archived schedule")
        }
       
        val shift = Shift(
            scheduleId = request.scheduleId,
            employeeId = request.employeeId!!,
            startTime = request.startTime!!,
            endTime = request.endTime!!,
            position = request.position
        )

        val savedShift = shiftRepository.save(shift)
        
        // Automatically create work session for the shift
        val workSession = workSessionService.createWorkSessionForShift(savedShift.id!!, savedShift.employeeId)
        
        // Automatically create session note with empty content initially
        try {
            sessionNoteService.createNote(
                SessionNoteRequest(
                    workSessionId = workSession.id!!,
                    content = ""
                )
            )
        } catch (e: Exception) {
            // If note creation fails, continue - the work session is still created
            println("Warning: Failed to create session note for work session ${workSession.id}: ${e.message}")
        }
        
        return mapToResponse(savedShift)
    }

    suspend fun getShiftById(id: String): ShiftResponse {
        val shift = shiftRepository.findById(id)
            ?: throw ResourceNotFoundException("Shift not found with id: $id")

        return mapToResponse(shift)
    }

    suspend fun updateShift(id: String, request: ShiftRequest): ShiftResponse {
        val existing = shiftRepository.findById(id)
            ?: throw ResourceNotFoundException("Shift not found with id: $id")

        // Verify schedule exists and is not archived
        val schedule = scheduleRepository.findById(request.scheduleId!!)
            ?: throw ResourceNotFoundException("Schedule not found with id: ${request.scheduleId}")

        if (schedule.status == "ARCHIVED") {
            throw IllegalStateException("Cannot update shifts in an archived schedule")
        }

        val updated = existing.copy(
            scheduleId = request.scheduleId,
            employeeId = request.employeeId!!,
            startTime = request.startTime!!,
            endTime = request.endTime!!,
            position = request.position,
            updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        )

        val saved = shiftRepository.save(updated)
        
        // Update work session if shift times changed
        val workSession = workSessionRepository.findByShiftId(saved.id!!)
        val shiftStartOffset = saved.startTime.toOffsetDateTime()
        val shiftEndOffset = saved.endTime.toOffsetDateTime()
        
        if (workSession != null && (
            workSession.clockOutTime != shiftEndOffset || workSession.clockInTime != shiftStartOffset
        )) {
            // Reset confirmation when shift is updated
            workSessionService.modifyWorkSession(
                workSessionId = workSession.id!!,
                newClockInTime = shiftStartOffset,
                newClockOutTime = shiftEndOffset,
                modifiedBy = "system"
            )
        }
        
        return mapToResponse(saved)
    }

    suspend fun deleteShift(id: String) {
        val shift = shiftRepository.findById(id)
            ?: throw ResourceNotFoundException("Shift not found with id: $id")

        // Verify schedule is not archived
        val schedule = scheduleRepository.findById(shift.scheduleId)
            ?: throw ResourceNotFoundException("Schedule not found with id: ${shift.scheduleId}")

        if (schedule.status == "ARCHIVED") {
            throw IllegalStateException("Cannot delete shifts from an archived schedule")
        }

        // Manually delete related entities before deleting the shift
        val workSession = workSessionRepository.findByShiftId(id)
        if (workSession != null) {
            // Delete the session note if it exists
            val sessionNote = sessionNoteService.getSessionNoteByWorkSessionId(workSession.id!!)
            if (sessionNote != null) {
                sessionNoteRepository.delete(sessionNote)
            }
            // Delete the work session
            workSessionRepository.delete(workSession)
        }

        shiftRepository.delete(shift)
    }

    fun getScheduleShifts(scheduleId: String): Flow<ShiftResponse> {
        return shiftRepository.findByScheduleId(scheduleId)
            .map { mapToResponse(it) }
    }

    fun getScheduleShiftsForUser(scheduleId: String, userId: String): Flow<ShiftResponse> {
        return shiftRepository.findByScheduleIdAndEmployeeId(scheduleId, userId)
            .map { mapToResponse(it) }
    }

    fun getEmployeeShifts(employeeId: String): Flow<ShiftResponse> {
        return shiftRepository.findByEmployeeId(employeeId)
            .map { mapToResponse(it) }
    }

    fun getUpcomingEmployeeShifts(employeeId: String): Flow<ShiftResponse> {
        // Get current time in UTC
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        // Get start of today in UTC
        val startOfToday = now.toLocalDate().atStartOfDay(ZoneId.of("UTC"))

        return shiftRepository.findByEmployeeId(employeeId)
            .filter { shift ->
                // Make sure the shift timezone is correctly interpreted
                val shiftStartTime = if (shift.startTime.zone == ZoneId.of("UTC")) {
                    shift.startTime
                } else {
                    // If timezone is different, convert to UTC for comparison
                    shift.startTime.withZoneSameInstant(ZoneId.of("UTC"))
                }

                // Compare the actual timestamps rather than just the dates
                !shiftStartTime.isBefore(startOfToday)
            }
            .map { mapToResponse(it) }
    }

    suspend fun getUpcomingEmployeeShiftsWithWorkSessions(employeeId: String): Flow<ShiftWithWorkSessionResponse> {
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val startOfToday = now.toLocalDate().atStartOfDay(ZoneId.of("UTC"))

        return shiftRepository.findByEmployeeId(employeeId)
            .filter { shift ->
                val shiftStartTime = shift.startTime.withZoneSameInstant(ZoneId.of("UTC"))
                !shiftStartTime.isBefore(startOfToday)
            }
            .map { shift ->
                val workSession = workSessionRepository.findByShiftId(shift.id!!)
                val workSessionWithNote = workSession?.let { session ->
                    val sessionNote = session.id?.let { sessionId ->
                        sessionNoteService.getNoteByWorkSessionId(sessionId)
                    }
                    WorkSessionWithNoteResponse(
                        id = session.id,
                        userId = session.userId,
                        shiftId = session.shiftId,
                        clockInTime = session.clockInTime,
                        clockOutTime = session.clockOutTime,
                        totalMinutes = session.totalMinutes,
                        status = session.status,
                        confirmed = session.confirmed,
                        confirmedBy = session.confirmedBy,
                        confirmedAt = session.confirmedAt,
                        sessionNote = sessionNote
                    )
                }

                ShiftWithWorkSessionResponse(
                    id = shift.id!!,
                    scheduleId = shift.scheduleId,
                    employeeId = shift.employeeId,
                    startTime = shift.startTime,
                    endTime = shift.endTime,
                    position = shift.position,
                    createdAt = shift.createdAt,
                    updatedAt = shift.updatedAt,
                    workSession = workSessionWithNote
                )
            }
    }

    fun getBusinessUnitShiftsForWeek(businessUnitId: String, weekStart: ZonedDateTime): Flow<ShiftResponse> {
        // Calculate the end of the week (weekStart + 6 days)
        val weekEnd = weekStart.toLocalDate().plusDays(6).atTime(23, 59, 59).atZone(ZoneId.of("UTC"))
        
        // Get all schedules for the business unit
        return shiftRepository.findAll()
            .filter { shift ->
                // Filter shifts that fall within the week timeframe
                val shiftDate = shift.startTime
                (!shiftDate.isBefore(weekStart)) && (!shiftDate.isAfter(weekEnd)) &&
                // Check if the shift belongs to the requested business unit
                isShiftForBusinessUnit(shift, businessUnitId)
            }
            .map { mapToResponse(it) }
    }
    
    fun getBusinessUnitShiftsForDay(businessUnitId: String, date: ZonedDateTime): Flow<ShiftResponse> {
        val dayStart = date.toLocalDate().atStartOfDay(ZoneId.of("UTC"))
        val dayEnd = date.toLocalDate().atTime(23, 59, 59).atZone(ZoneId.of("UTC"))
        
        return shiftRepository.findAll()
            .filter { shift ->
                // Filter shifts that fall within the day timeframe
                val shiftDate = shift.startTime
                (!shiftDate.isBefore(dayStart)) && (!shiftDate.isAfter(dayEnd)) &&
                // Check if the shift belongs to the requested business unit
                isShiftForBusinessUnit(shift, businessUnitId)
            }
            .map { mapToResponse(it) }
    }

    /**
     * ADMIN/MANAGER ENDPOINT: Get comprehensive shifts with work sessions and session notes
     * for a business unit within a date range
     */
    suspend fun getShiftsWithWorkSessionsAndNotes(
        businessUnitId: String,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime
    ): List<ShiftWithWorkSessionResponse> {
        val schedules = scheduleRepository.findByBusinessUnitIdAndWeekStartBetween(
            businessUnitId, startDate, endDate
        ).toList()

        val shiftResponses = mutableListOf<ShiftWithWorkSessionResponse>()

        for (schedule in schedules) {
            val shifts = shiftRepository.findByScheduleId(schedule.id!!).toList()
            for (shift in shifts) {
            val workSession = workSessionRepository.findByShiftId(shift.id!!)
            val workSessionWithNote = workSession?.let { session ->
                val sessionNote = session.id?.let { sessionId ->
                    sessionNoteService.getNoteByWorkSessionId(sessionId)
                }
                WorkSessionWithNoteResponse(
                    id = session.id,
                    userId = session.userId,
                    shiftId = session.shiftId,
                    clockInTime = session.clockInTime,
                    clockOutTime = session.clockOutTime,
                    totalMinutes = session.totalMinutes,
                    status = session.status,
                    confirmed = session.confirmed,
                    confirmedBy = session.confirmedBy,
                    confirmedAt = session.confirmedAt,
                    sessionNote = sessionNote
                )
            }

                shiftResponses.add(ShiftWithWorkSessionResponse(
                id = shift.id!!,
                scheduleId = shift.scheduleId,
                employeeId = shift.employeeId,
                startTime = shift.startTime,
                endTime = shift.endTime,
                position = shift.position,
                createdAt = shift.createdAt,
                updatedAt = shift.updatedAt,
                workSession = workSessionWithNote
                ))
            }
        }
        return shiftResponses
    }
    
    // Helper method to check if a shift belongs to a business unit
    private suspend fun isShiftForBusinessUnit(shift: Shift, businessUnitId: String): Boolean {
        val schedule = scheduleRepository.findById(shift.scheduleId)
        return schedule?.businessUnitId == businessUnitId
    }
    
    private fun mapToResponse(shift: Shift): ShiftResponse {
        return ShiftResponse(
            id = shift.id!!,
            scheduleId = shift.scheduleId,
            employeeId = shift.employeeId,
            startTime = shift.startTime,
            endTime = shift.endTime,
            position = shift.position,
            createdAt = shift.createdAt,
            updatedAt = shift.updatedAt
        )
    }
}