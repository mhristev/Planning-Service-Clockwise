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
import com.clockwise.planningservice.service.UserInfoService
import com.clockwise.planningservice.dto.workload.SessionNoteRequest
import com.clockwise.planningservice.utils.toOffsetDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.LocalDate

@Service
class ShiftService(
    private val shiftRepository: ShiftRepository,
    private val scheduleRepository: ScheduleRepository,
    private val workSessionRepository: WorkSessionRepository,
    private val sessionNoteRepository: SessionNoteRepository,
    private val sessionNoteService: SessionNoteService,
    private val workSessionService: WorkSessionService,
    private val userInfoService: UserInfoService
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
        
        // Trigger async user info request (fire and forget)
        userInfoService.requestUserInfo(savedShift.employeeId, savedShift.id!!)
        
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
                    employeeFirstName = shift.employeeFirstName,
                    employeeLastName = shift.employeeLastName,
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
                employeeFirstName = shift.employeeFirstName,
                employeeLastName = shift.employeeLastName,
                createdAt = shift.createdAt,
                updatedAt = shift.updatedAt,
                workSession = workSessionWithNote
                ))
            }
        }
        return shiftResponses
    }
    
    /**
     * ADMIN/MANAGER ENDPOINT: Get comprehensive shifts with work sessions and session notes
     * for a business unit within a specific month and year
     */
    suspend fun getShiftsWithWorkSessionsAndNotesByMonth(
        businessUnitId: String,
        month: Int,
        year: Int
    ): List<ShiftWithWorkSessionResponse> {
        // Calculate the start and end dates for the specified month
        val startDate = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val endDate = startDate.plusMonths(1).minusNanos(1)
        
        // Query shifts directly by their start_time within the month range
        val shifts = shiftRepository.findByBusinessUnitIdAndDateRange(
            businessUnitId, startDate, endDate
        ).toList()

        val shiftResponses = mutableListOf<ShiftWithWorkSessionResponse>()

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
                employeeFirstName = shift.employeeFirstName,
                employeeLastName = shift.employeeLastName,
                createdAt = shift.createdAt,
                updatedAt = shift.updatedAt,
                workSession = workSessionWithNote
            ))
        }
        return shiftResponses
    }

    /**
     * Get shifts with work sessions and session notes for a specific user in a business unit by month
     * This method retrieves all shifts for a specific user in a given business unit, month and year,
     * including their work sessions and session notes.
     */
    suspend fun getShiftsWithWorkSessionsAndNotesByMonthForUser(
        businessUnitId: String,
        userId: String,
        month: Int,
        year: Int
    ): List<ShiftWithWorkSessionResponse> {
        // Calculate the start and end dates for the specified month
        val startDate = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val endDate = startDate.plusMonths(1).minusNanos(1)
        
        // Query shifts directly by their start_time within the month range and filter by employeeId (userId)
        val shifts = shiftRepository.findByBusinessUnitIdAndDateRange(
            businessUnitId, startDate, endDate
        ).toList().filter { it.employeeId == userId }

        val shiftResponses = mutableListOf<ShiftWithWorkSessionResponse>()

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
                employeeFirstName = shift.employeeFirstName,
                employeeLastName = shift.employeeLastName,
                createdAt = shift.createdAt,
                updatedAt = shift.updatedAt,
                workSession = workSessionWithNote
            ))
        }
        return shiftResponses
    }
    
    // Helper method to check if a shift belongs to a business unit
    private suspend fun isShiftForBusinessUnit(shift: Shift, businessUnitId: String): Boolean {
        val schedule = scheduleRepository.findById(shift.scheduleId)
        return schedule?.businessUnitId == businessUnitId
    }
    
    /**
     * Reassign a shift to a different employee (used for TAKE_SHIFT requests)
     */
    suspend fun reassignShift(shiftId: String, newEmployeeId: String): Boolean {
        val shift = shiftRepository.findById(shiftId) ?: return false
        
        // Update the shift with new employee ID and clear cached names (they'll be updated via UserInfoService)
        val updatedShift = shift.copy(
            employeeId = newEmployeeId,
            employeeFirstName = null,
            employeeLastName = null,
            updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        )
        
        shiftRepository.save(updatedShift)
        
        // Update the associated work session with the new user
        val workSession = workSessionRepository.findByShiftId(shiftId)
        if (workSession != null) {
            val updatedWorkSession = workSession.copy(
                userId = newEmployeeId
            )
            workSessionRepository.save(updatedWorkSession)
        }
        
        // Trigger async user info request to get new employee names
        userInfoService.requestUserInfo(newEmployeeId, shiftId)
        
        return true
    }
    
    /**
     * Reassign a shift to a different employee using Keycloak user ID (used for TAKE_SHIFT requests from Collaboration Service)
     */
    suspend fun reassignShiftWithKeycloakId(shiftId: String, keycloakUserId: String): Boolean {
        val shift = shiftRepository.findById(shiftId) ?: return false
        
        // For TAKE_SHIFT from Collaboration Service, we directly use the Keycloak user ID
        // The Planning Service should be storing Keycloak user IDs as employee IDs
        val updatedShift = shift.copy(
            employeeId = keycloakUserId,
            employeeFirstName = null, // Will be updated by UserInfoService
            employeeLastName = null,
            updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        )
        
        shiftRepository.save(updatedShift)
        
        // Update the associated work session with the new user
        val workSession = workSessionRepository.findByShiftId(shiftId)
        if (workSession != null) {
            val updatedWorkSession = workSession.copy(
                userId = keycloakUserId
            )
            workSessionRepository.save(updatedWorkSession)
        }
        
        // Trigger async user info request to get new employee names
        userInfoService.requestUserInfo(keycloakUserId, shiftId)
        
        return true
    }
    
    /**
     * Swap two shifts between two employees using Keycloak user IDs (used for SWAP_SHIFT requests from Collaboration Service)
     */
    suspend fun swapShiftsWithKeycloakIds(shift1Id: String, shift2Id: String, keycloakUser1Id: String, keycloakUser2Id: String): Boolean {
        val shift1 = shiftRepository.findById(shift1Id) ?: return false
        val shift2 = shiftRepository.findById(shift2Id) ?: return false
        
        // For Keycloak ID validation, we need to check if the shifts' employee IDs correspond to the Keycloak IDs
        // Since we don't have direct access to user mapping, we'll verify by checking if the current employee IDs
        // make sense with the swap operation (i.e., the shifts exist and belong to different employees)
        
        if (shift1.employeeId == shift2.employeeId) {
            throw IllegalArgumentException("Cannot swap shifts that belong to the same employee")
        }
        
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        
        // For swaps from Collaboration Service, we swap the assignments:
        // shift1 (original) goes to the requester's employee ID (shift2's current employee)
        // shift2 (swap) goes to the poster's employee ID (shift1's current employee)
        val updatedShift1 = shift1.copy(
            employeeId = shift2.employeeId, // Give original shift to requester
            employeeFirstName = null, // Will be updated by UserInfoService
            employeeLastName = null,
            updatedAt = now
        )
        
        val updatedShift2 = shift2.copy(
            employeeId = shift1.employeeId, // Give swap shift to poster
            employeeFirstName = null, // Will be updated by UserInfoService
            employeeLastName = null,
            updatedAt = now
        )
        
        // Save both shifts
        shiftRepository.save(updatedShift1)
        shiftRepository.save(updatedShift2)
        
        // Update work sessions if they exist
        val workSession1 = workSessionRepository.findByShiftId(shift1Id)
        val workSession2 = workSessionRepository.findByShiftId(shift2Id)
        
        if (workSession1 != null) {
            val updatedWorkSession1 = workSession1.copy(userId = shift2.employeeId)
            workSessionRepository.save(updatedWorkSession1)
        }
        
        if (workSession2 != null) {
            val updatedWorkSession2 = workSession2.copy(userId = shift1.employeeId)
            workSessionRepository.save(updatedWorkSession2)
        }
        
        // Trigger async user info requests for both shifts to update employee names
        userInfoService.requestUserInfo(shift2.employeeId, shift1Id)
        userInfoService.requestUserInfo(shift1.employeeId, shift2Id)
        
        return true
    }
    
    /**
     * Swap two shifts between two employees (used for SWAP_SHIFT requests)
     */
    suspend fun swapShifts(shift1Id: String, shift2Id: String, user1Id: String, user2Id: String): Boolean {
        val shift1 = shiftRepository.findById(shift1Id) ?: return false
        val shift2 = shiftRepository.findById(shift2Id) ?: return false
        
        // Verify that the users currently own the shifts they're trying to swap
        if (shift1.employeeId != user1Id || shift2.employeeId != user2Id) {
            throw IllegalArgumentException("Shift ownership verification failed")
        }
        
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        
        // Update shift1 to be assigned to user2
        val updatedShift1 = shift1.copy(
            employeeId = user2Id,
            employeeFirstName = null, // Will be updated by UserInfoService
            employeeLastName = null,
            updatedAt = now
        )
        
        // Update shift2 to be assigned to user1
        val updatedShift2 = shift2.copy(
            employeeId = user1Id,
            employeeFirstName = null, // Will be updated by UserInfoService
            employeeLastName = null,
            updatedAt = now
        )
        
        // Save both shifts
        shiftRepository.save(updatedShift1)
        shiftRepository.save(updatedShift2)
        
        // Update work sessions if they exist
        val workSession1 = workSessionRepository.findByShiftId(shift1Id)
        val workSession2 = workSessionRepository.findByShiftId(shift2Id)
        
        if (workSession1 != null) {
            val updatedWorkSession1 = workSession1.copy(userId = user2Id)
            workSessionRepository.save(updatedWorkSession1)
        }
        
        if (workSession2 != null) {
            val updatedWorkSession2 = workSession2.copy(userId = user1Id)
            workSessionRepository.save(updatedWorkSession2)
        }
        
        // Trigger async user info requests for both shifts to update employee names
        userInfoService.requestUserInfo(user2Id, shift1Id)
        userInfoService.requestUserInfo(user1Id, shift2Id)
        
        return true
    }
    
    private fun mapToResponse(shift: Shift): ShiftResponse {
        return ShiftResponse(
            id = shift.id!!,
            scheduleId = shift.scheduleId,
            employeeId = shift.employeeId,
            startTime = shift.startTime,
            endTime = shift.endTime,
            position = shift.position,
            employeeFirstName = shift.employeeFirstName,
            employeeLastName = shift.employeeLastName,
            createdAt = shift.createdAt,
            updatedAt = shift.updatedAt
        )
    }
}