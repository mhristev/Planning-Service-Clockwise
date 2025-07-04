package com.clockwise.planningservice.services

import com.clockwise.planningservice.ResourceNotFoundException
import com.clockwise.planningservice.domains.Schedule
import com.clockwise.planningservice.domains.ScheduleStatus
import com.clockwise.planningservice.dto.ScheduleRequest
import com.clockwise.planningservice.dto.ScheduleResponse
import com.clockwise.planningservice.dto.ScheduleWithShiftsResponse
import com.clockwise.planningservice.dto.ShiftResponse
import com.clockwise.planningservice.repositories.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId
import com.clockwise.planningservice.dto.MonthlyScheduleDto
import com.clockwise.planningservice.dto.ShiftWithSessionsDto
import com.clockwise.planningservice.dto.WorkSessionDto
import com.clockwise.planningservice.dto.SessionNoteDto
import com.clockwise.planningservice.services.workload.WorkSessionService
import com.clockwise.planningservice.services.workload.SessionNoteService
import java.time.YearMonth

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val shiftService: ShiftService,
    private val workSessionService: WorkSessionService,
    private val sessionNoteService: SessionNoteService
) {

    /**
     * Normalizes a LocalDate to the start of the week (Monday at 00:00:00 UTC).
     * This ensures consistent week_start values regardless of which day of the week is provided.
     */
    private fun normalizeToWeekStart(date: LocalDate): ZonedDateTime {
        val mondayOfWeek = date.with(DayOfWeek.MONDAY)
        return mondayOfWeek.atStartOfDay(ZoneId.of("UTC"))
    }

    /**
     * PUBLIC ENDPOINT: Get a published schedule with shifts by business unit and week
     * Only returns schedules with PUBLISHED status
     */
    suspend fun getPublishedScheduleWithShiftsByWeek(businessUnitId: String, weekStart: LocalDate): ScheduleWithShiftsResponse? {
        val normalizedWeekStart = normalizeToWeekStart(weekStart)
        val schedule = scheduleRepository.findByBusinessUnitIdAndWeekStart(businessUnitId, normalizedWeekStart)
            ?: return null

        if (schedule.status != "PUBLISHED") {
            throw IllegalStateException("Schedule for week ${weekStart} is not published and therefore not accessible")
        }

        val shifts = shiftService.getScheduleShifts(schedule.id!!).toList()
        return mapToResponseWithShifts(schedule, shifts)
    }

    /**
     * ADMIN ENDPOINT: Get any schedule with shifts by business unit and week
     * Returns schedules regardless of status - restricted to admin/manager roles
     */
    suspend fun getScheduleWithShiftsByWeekForAdmin(businessUnitId: String, weekStart: LocalDate): ScheduleWithShiftsResponse? {
        val normalizedWeekStart = normalizeToWeekStart(weekStart)
        val schedule = scheduleRepository.findByBusinessUnitIdAndWeekStart(businessUnitId, normalizedWeekStart)
            ?: return null

        println("🔍 DEBUG: Found schedule with ID: ${schedule.id}")
        val shifts = shiftService.getScheduleShifts(schedule.id!!).toList()
        println("🔍 DEBUG: Found ${shifts.size} shifts for schedule")
        shifts.forEach { shift ->
            println("🔍 DEBUG: Shift - ID: ${shift.id}, Employee: ${shift.employeeId}, Position: ${shift.position}")
        }
        
        val response = mapToResponseWithShifts(schedule, shifts)
        println("🔍 DEBUG: Created response with ${response.shifts.size} shifts")
        return response
    }

    suspend fun createSchedule(request: ScheduleRequest): ScheduleResponse {
        val normalizedWeekStart = normalizeToWeekStart(request.weekStart!!)
        
        val schedule = Schedule(
            businessUnitId = request.businessUnitId!!,
            weekStart = normalizedWeekStart,
            status = "DRAFT"
        )

        val saved = scheduleRepository.save(schedule)
        return mapToResponse(saved)
    }

    suspend fun getScheduleById(id: String): ScheduleResponse {
        val schedule = scheduleRepository.findById(id)
            ?: throw ResourceNotFoundException("Schedule not found with id: $id")

        return mapToResponse(schedule)
    }

    suspend fun updateSchedule(id: String, request: ScheduleRequest): ScheduleResponse {
        val existing = scheduleRepository.findById(id)
            ?: throw ResourceNotFoundException("Schedule not found with id: $id")

        // Only allow updates if schedule is in DRAFT status
        if (existing.status != "DRAFT") {
            throw IllegalStateException("Cannot update a schedule that is not in DRAFT status")
        }

        val normalizedWeekStart = normalizeToWeekStart(request.weekStart!!)

        val updated = existing.copy(
            businessUnitId = request.businessUnitId!!,
            weekStart = normalizedWeekStart,
            updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        )

        val saved = scheduleRepository.save(updated)
        return mapToResponse(saved)
    }

    suspend fun publishSchedule(id: String): ScheduleResponse {
        val schedule = scheduleRepository.findById(id)
            ?: throw ResourceNotFoundException("Schedule not found with id: $id")

        if (schedule.status != "DRAFT") {
            throw IllegalStateException("Only schedules in DRAFT status can be published")
        }

        val updated = schedule.copy(
            status = "PUBLISHED",
            updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        )

        val saved = scheduleRepository.save(updated)
        return mapToResponse(saved)
    }

    fun getAllSchedules(): Flow<ScheduleResponse> {
        return scheduleRepository.findAll()
            .map { mapToResponse(it) }
    }

    fun getBusinessUnitSchedules(businessUnitId: String): Flow<ScheduleResponse> {
        return scheduleRepository.findByBusinessUnitId(businessUnitId)
            .map { mapToResponse(it) }
    }

    suspend fun getCurrentBusinessUnitSchedule(businessUnitId: String): ScheduleResponse? {
        val schedule = scheduleRepository.findCurrentScheduleByBusinessUnitId(businessUnitId)
        return schedule?.let { mapToResponse(it) }
    }

    suspend fun getScheduleByWeekStart(businessUnitId: String, weekStart: LocalDate): ScheduleResponse? {
        val normalizedWeekStart = normalizeToWeekStart(weekStart)
        val schedule = scheduleRepository.findByBusinessUnitIdAndWeekStart(businessUnitId, normalizedWeekStart)
        return schedule?.let { mapToResponse(it) }
    }

    suspend fun updatePublishedSchedule(id: String, request: ScheduleRequest): ScheduleResponse {
        val existing = scheduleRepository.findById(id)
            ?: throw ResourceNotFoundException("Schedule not found with id: $id")

        val normalizedWeekStart = normalizeToWeekStart(request.weekStart!!)

        // Allow updates to published schedules
        val updated = existing.copy(
            businessUnitId = request.businessUnitId!!,
            weekStart = normalizedWeekStart,
            updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        )

        val saved = scheduleRepository.save(updated)
        return mapToResponse(saved)
    }

    suspend fun revertToDraft(id: String): ScheduleResponse {
        val schedule = scheduleRepository.findById(id)
            ?: throw ResourceNotFoundException("Schedule not found with id: $id")

        if (schedule.status != "PUBLISHED") {
            throw IllegalStateException("Only schedules in PUBLISHED status can be reverted to DRAFT")
        }

        val updated = schedule.copy(
            status = "DRAFT",
            updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        )

        val saved = scheduleRepository.save(updated)
        return mapToResponse(saved)
    }

    /**
     * Get monthly schedule for a specific user
     * Returns all weekly schedules for the given month that contain shifts for the specified user
     */
    suspend fun getMonthlyScheduleForUser(
        businessUnitId: String, 
        userId: String, 
        month: Int, 
        year: Int
    ): List<MonthlyScheduleDto> {
        // Calculate the date range for the given month
        val yearMonth = YearMonth.of(year, month)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        
        // Find all weekly schedules for the business unit within the date range
        val schedules = scheduleRepository.findByBusinessUnitIdAndWeekStartBetween(
            businessUnitId, 
            normalizeToWeekStart(startDate),
            normalizeToWeekStart(endDate.plusDays(6)) // Add 6 days to catch schedules that start before the month but contain days in the month
        ).toList()
        
        // For each schedule, get shifts for the specified user and their work sessions
        val monthlySchedules = mutableListOf<MonthlyScheduleDto>()
        
        for (schedule in schedules) {
            val userShifts = shiftService.getScheduleShiftsForUser(schedule.id!!, userId).toList()
            
            if (userShifts.isNotEmpty()) {
                val shiftsWithSessions = mutableListOf<ShiftWithSessionsDto>()
                
                for (shift in userShifts) {
                    // Get work session for this shift
                    val workSession = workSessionService.getWorkSessionByShiftId(shift.id)
                    
                    val workSessionDto = if (workSession != null) {
                        // Get session note if exists
                        val sessionNote = sessionNoteService.getSessionNoteByWorkSessionId(workSession.id!!)
                        
                        WorkSessionDto(
                            id = workSession.id!!,
                            clockInTime = workSession.clockInTime.atZoneSameInstant(ZoneId.of("UTC")),
                            clockOutTime = workSession.clockOutTime?.atZoneSameInstant(ZoneId.of("UTC")),
                            note = sessionNote?.let { 
                                SessionNoteDto(
                                    id = it.id!!,
                                    noteContent = it.content
                                )
                            }
                        )
                    } else null
                    
                    shiftsWithSessions.add(
                        ShiftWithSessionsDto(
                            id = shift.id,
                            startTime = shift.startTime,
                            endTime = shift.endTime,
                            role = shift.position,
                            workSession = workSessionDto
                        )
                    )
                }
                
                monthlySchedules.add(
                    MonthlyScheduleDto(
                        scheduleId = schedule.id!!,
                        weekStartDate = schedule.weekStart.toLocalDate(),
                        shifts = shiftsWithSessions
                    )
                )
            }
        }
        
        return monthlySchedules
    }

    private fun mapToResponse(schedule: Schedule): ScheduleResponse {
        return ScheduleResponse(
            id = schedule.id!!,
            businessUnitId = schedule.businessUnitId,
            weekStart = schedule.weekStart,
            status = schedule.status,
            createdAt = schedule.createdAt,
            updatedAt = schedule.updatedAt
        )
    }

    private fun mapToResponseWithShifts(schedule: Schedule, shifts: List<ShiftResponse>): ScheduleWithShiftsResponse {
        return ScheduleWithShiftsResponse(
            id = schedule.id!!,
            businessUnitId = schedule.businessUnitId,
            weekStart = schedule.weekStart,
            status = schedule.status,
            createdAt = schedule.createdAt,
            updatedAt = schedule.updatedAt,
            shifts = shifts
        )
    }
}