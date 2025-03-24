package com.clockwise.planningservice


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import java.time.LocalDateTime


@Service
class AvailabilityService(private val availabilityRepository: AvailabilityRepository) {

    suspend fun createAvailability(request: AvailabilityRequest): AvailabilityResponse {
        val availability = Availability(
            employeeId = request.employeeId!!,
            startTime = request.startTime!!,
            endTime = request.endTime!!
        )

        val saved = availabilityRepository.save(availability)
        return mapToResponse(saved)
    }

    suspend fun getAvailabilityById(id: String): AvailabilityResponse {
        val availability = availabilityRepository.findById(id)
            ?: throw ResourceNotFoundException("Availability not found with id: $id")

        return mapToResponse(availability)
    }

    suspend fun updateAvailability(id: String, request: AvailabilityRequest): AvailabilityResponse {
        val existing = availabilityRepository.findById(id)
            ?: throw ResourceNotFoundException("Availability not found with id: $id")

        val updated = existing.copy(
            employeeId = request.employeeId!!,
            startTime = request.startTime!!,
            endTime = request.endTime!!,
            updatedAt = LocalDateTime.now()
        )

        val saved = availabilityRepository.save(updated)
        return mapToResponse(saved)
    }

    suspend fun deleteAvailability(id: String) {
        val availability = availabilityRepository.findById(id)
            ?: throw ResourceNotFoundException("Availability not found with id: $id")

        availabilityRepository.delete(availability)
    }

    fun getEmployeeAvailabilities(employeeId: String): Flow<AvailabilityResponse> {
        return availabilityRepository.findByEmployeeId(employeeId)
            .map { mapToResponse(it) }
    }

    fun getRestaurantAvailabilities(restaurantId: String): Flow<AvailabilityResponse> {
        return availabilityRepository.findByRestaurantId(restaurantId)
            .map { mapToResponse(it) }
    }

    fun getRestaurantAvailabilitiesByDateRange(
        restaurantId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<AvailabilityResponse> {
        return availabilityRepository.findByRestaurantIdAndDateRange(restaurantId, startDate, endDate)
            .map { mapToResponse(it) }
    }

    private fun mapToResponse(availability: Availability): AvailabilityResponse {
        return AvailabilityResponse(
            id = availability.id!!,
            employeeId = availability.employeeId,
            startTime = availability.startTime,
            endTime = availability.endTime,
            createdAt = availability.createdAt,
            updatedAt = availability.updatedAt
        )
    }
}






@Service
class ScheduleService(private val scheduleRepository: ScheduleRepository) {

    suspend fun createSchedule(request: ScheduleRequest): ScheduleResponse {
        val schedule = Schedule(
            restaurantId = request.restaurantId!!,
            weekStart = request.weekStart!!,
            status = ScheduleStatus.DRAFT
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
        if (existing.status != ScheduleStatus.DRAFT) {
            throw IllegalStateException("Cannot update a schedule that is not in DRAFT status")
        }

        val updated = existing.copy(
            restaurantId = request.restaurantId!!,
            weekStart = request.weekStart!!,
            updatedAt = LocalDateTime.now()
        )

        val saved = scheduleRepository.save(updated)
        return mapToResponse(saved)
    }

    suspend fun publishSchedule(id: String): ScheduleResponse {
        val schedule = scheduleRepository.findById(id)
            ?: throw ResourceNotFoundException("Schedule not found with id: $id")

        if (schedule.status != ScheduleStatus.DRAFT) {
            throw IllegalStateException("Only schedules in DRAFT status can be published")
        }

        val updated = schedule.copy(
            status = ScheduleStatus.PUBLISHED,
            updatedAt = LocalDateTime.now()
        )

        val saved = scheduleRepository.save(updated)
        return mapToResponse(saved)
    }

    fun getRestaurantSchedules(restaurantId: String): Flow<ScheduleResponse> {
        return scheduleRepository.findByRestaurantId(restaurantId)
            .map { mapToResponse(it) }
    }

    suspend fun getCurrentRestaurantSchedule(restaurantId: String): ScheduleResponse? {
        val schedule = scheduleRepository.findCurrentScheduleByRestaurantId(restaurantId)
        return schedule?.let { mapToResponse(it) }
    }

    suspend fun updatePublishedSchedule(id: String, request: ScheduleRequest): ScheduleResponse {
        val existing = scheduleRepository.findById(id)
            ?: throw ResourceNotFoundException("Schedule not found with id: $id")

        // Allow updates to published schedules
        val updated = existing.copy(
            restaurantId = request.restaurantId!!,
            weekStart = request.weekStart!!,
            updatedAt = LocalDateTime.now()
        )

        val saved = scheduleRepository.save(updated)
        return mapToResponse(saved)
    }

    private fun mapToResponse(schedule: Schedule): ScheduleResponse {
        return ScheduleResponse(
            id = schedule.id!!,
            restaurantId = schedule.restaurantId,
            weekStart = schedule.weekStart,
            status = schedule.status,
            createdAt = schedule.createdAt,
            updatedAt = schedule.updatedAt
        )
    }
}

// ShiftService.kt


@Service
class ShiftService(
    private val shiftRepository: ShiftRepository,
    private val scheduleRepository: ScheduleRepository
) {

    suspend fun createShift(request: ShiftRequest): ShiftResponse {
        // Verify schedule exists and is in draft or published status
        val schedule = scheduleRepository.findById(request.scheduleId!!)
            ?: throw ResourceNotFoundException("Schedule not found with id: ${request.scheduleId}")

        if (schedule.status == ScheduleStatus.ARCHIVED) {
            throw IllegalStateException("Cannot add shifts to an archived schedule")
        }

        val shift = Shift(
            scheduleId = request.scheduleId,
            employeeId = request.employeeId!!,
            startTime = request.startTime!!,
            endTime = request.endTime!!
        )

        val saved = shiftRepository.save(shift)
        return mapToResponse(saved)
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

        if (schedule.status == ScheduleStatus.ARCHIVED) {
            throw IllegalStateException("Cannot update shifts in an archived schedule")
        }

        val updated = existing.copy(
            scheduleId = request.scheduleId,
            employeeId = request.employeeId!!,
            startTime = request.startTime!!,
            endTime = request.endTime!!,
            updatedAt = LocalDateTime.now()
        )

        val saved = shiftRepository.save(updated)
        return mapToResponse(saved)
    }

    suspend fun deleteShift(id: String) {
        val shift = shiftRepository.findById(id)
            ?: throw ResourceNotFoundException("Shift not found with id: $id")

        // Verify schedule is not archived
        val schedule = scheduleRepository.findById(shift.scheduleId)
            ?: throw ResourceNotFoundException("Schedule not found with id: ${shift.scheduleId}")

        if (schedule.status == ScheduleStatus.ARCHIVED) {
            throw IllegalStateException("Cannot delete shifts from an archived schedule")
        }

        shiftRepository.delete(shift)
    }

    fun getScheduleShifts(scheduleId: String): Flow<ShiftResponse> {
        return shiftRepository.findByScheduleId(scheduleId)
            .map { mapToResponse(it) }
    }

    fun getEmployeeShifts(employeeId: String): Flow<ShiftResponse> {
        return shiftRepository.findByEmployeeId(employeeId)
            .map { mapToResponse(it) }
    }
    private fun mapToResponse(shift: Shift): ShiftResponse {
        return ShiftResponse(
            id = shift.id!!,
            scheduleId = shift.scheduleId,
            employeeId = shift.employeeId,
            startTime = shift.startTime,
            endTime = shift.endTime,
            createdAt = shift.createdAt,
            updatedAt = shift.updatedAt
        )
    }
}