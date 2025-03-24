package com.clockwise.planningservice.services

import com.clockwise.planningservice.domains.Schedule
import com.clockwise.planningservice.domains.ScheduleStatus
import com.clockwise.planningservice.dto.ScheduleRequest
import com.clockwise.planningservice.dto.ScheduleResponse
import com.clockwise.planningservice.repositories.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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