package com.clockwise.planningservice.services

import com.clockwise.planningservice.ResourceNotFoundException
import com.clockwise.planningservice.domains.Availability
import com.clockwise.planningservice.dto.AvailabilityRequest
import com.clockwise.planningservice.dto.AvailabilityResponse
import com.clockwise.planningservice.repositories.AvailabilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.time.ZoneId

@Service
class AvailabilityService(private val availabilityRepository: AvailabilityRepository) {

    suspend fun createAvailability(request: AvailabilityRequest): AvailabilityResponse {
        val availability = Availability(
            employeeId = request.employeeId!!,
            startTime = request.startTime!!,
            endTime = request.endTime!!,
            businessUnitId = request.businessUnitId
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
        val existingAvailability = availabilityRepository.findById(id)
            ?: throw IllegalArgumentException("Availability not found")

        val updatedAvailability = Availability(
            id = existingAvailability.id,
            employeeId = request.employeeId ?: existingAvailability.employeeId,
            startTime = request.startTime ?: existingAvailability.startTime,
            endTime = request.endTime ?: existingAvailability.endTime,
            businessUnitId = request.businessUnitId ?: existingAvailability.businessUnitId,
            createdAt = existingAvailability.createdAt,
            updatedAt = ZonedDateTime.now(ZoneId.of("UTC"))
        )

        val saved = availabilityRepository.save(updatedAvailability)
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

    suspend fun getRestaurantAvailabilitiesByDateRange(
        restaurantId: String,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime
    ): Flow<AvailabilityResponse> {
        return availabilityRepository.findByRestaurantIdAndDateRange(restaurantId, startDate, endDate)
            .map { mapToResponse(it) }
    }

    fun getBusinessUnitAvailabilities(businessUnitId: String): Flow<AvailabilityResponse> {
        return availabilityRepository.findByBusinessUnitId(businessUnitId)
            .map { mapToResponse(it) }
    }

    suspend fun getBusinessUnitAvailabilitiesByDateRange(
        businessUnitId: String,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime
    ): Flow<AvailabilityResponse> {
        return availabilityRepository.findByBusinessUnitIdAndDateRange(businessUnitId, startDate, endDate)
            .map { mapToResponse(it) }
    }

    private fun mapToResponse(availability: Availability): AvailabilityResponse {
        return AvailabilityResponse(
            id = availability.id!!,
            employeeId = availability.employeeId,
            startTime = availability.startTime,
            endTime = availability.endTime,
            businessUnitId = availability.businessUnitId,
            createdAt = availability.createdAt,
            updatedAt = availability.updatedAt
        )
    }
} 