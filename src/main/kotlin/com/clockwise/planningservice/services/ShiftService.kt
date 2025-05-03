package com.clockwise.planningservice.services

import com.clockwise.planningservice.ResourceNotFoundException
import com.clockwise.planningservice.domains.Shift
import com.clockwise.planningservice.dto.ShiftRequest
import com.clockwise.planningservice.dto.ShiftResponse
import com.clockwise.planningservice.repositories.ScheduleRepository
import com.clockwise.planningservice.repositories.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.LocalDate

@Service
class ShiftService(
    private val shiftRepository: ShiftRepository,
    private val scheduleRepository: ScheduleRepository
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
    
    // Helper method to check if a shift belongs to a business unit
    private suspend fun isShiftForBusinessUnit(shift: Shift, businessUnitId: String): Boolean {
        val schedule = scheduleRepository.findById(shift.scheduleId) ?: return false
        return schedule.restaurantId == businessUnitId
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