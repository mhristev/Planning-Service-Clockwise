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

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val shiftService: ShiftService
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

        val shifts = shiftService.getScheduleShifts(schedule.id!!).toList()
        return mapToResponseWithShifts(schedule, shifts)
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