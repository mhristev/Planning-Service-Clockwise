package com.clockwise.planningservice.services

import com.clockwise.planningservice.ResourceNotFoundException
import com.clockwise.planningservice.domains.Availability
import com.clockwise.planningservice.dto.AvailabilityRequest
import com.clockwise.planningservice.dto.AvailabilityResponse
import com.clockwise.planningservice.repositories.AvailabilityRepository
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