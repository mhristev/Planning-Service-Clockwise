package com.clockwise.planningservice.services

import com.clockwise.planningservice.domains.ScheduleStatus
import com.clockwise.planningservice.domains.Shift
import com.clockwise.planningservice.dto.ShiftRequest
import com.clockwise.planningservice.dto.ShiftResponse
import com.clockwise.planningservice.repositories.ScheduleRepository
import com.clockwise.planningservice.repositories.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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