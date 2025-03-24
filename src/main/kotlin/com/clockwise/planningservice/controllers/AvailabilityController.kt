package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.AvailabilityRequest
import com.clockwise.planningservice.dto.AvailabilityResponse
import com.clockwise.planningservice.services.AvailabilityService
import kotlinx.coroutines.flow.Flow
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/v1")
class AvailabilityController(private val availabilityService: AvailabilityService) {

    @PostMapping("/availabilities")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createAvailability(@RequestBody request: AvailabilityRequest): AvailabilityResponse {
        return availabilityService.createAvailability(request)
    }

    @GetMapping("/availabilities/{id}")
    suspend fun getAvailabilityById(@PathVariable id: String): AvailabilityResponse {
        return availabilityService.getAvailabilityById(id)
    }

    @PutMapping("/availabilities/{id}")
    suspend fun updateAvailability(
        @PathVariable id: String,
        @RequestBody request: AvailabilityRequest
    ): AvailabilityResponse {
        return availabilityService.updateAvailability(id, request)
    }

    @DeleteMapping("/availabilities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteAvailability(@PathVariable id: String) {
        availabilityService.deleteAvailability(id)
    }

    @GetMapping("/users/{id}/availabilities")
    fun getEmployeeAvailabilities(@PathVariable id: String): Flow<AvailabilityResponse> {
        return availabilityService.getEmployeeAvailabilities(id)
    }

    @GetMapping("/restaurants/{id}/availabilities")
    fun getRestaurantAvailabilities(
        @PathVariable id: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): Flow<AvailabilityResponse> {
        return if (startDate != null && endDate != null) {
            availabilityService.getRestaurantAvailabilitiesByDateRange(id, startDate, endDate)
        } else {
            availabilityService.getRestaurantAvailabilities(id)
        }
    }
} 