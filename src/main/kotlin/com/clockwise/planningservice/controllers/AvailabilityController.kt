package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.AvailabilityRequest
import com.clockwise.planningservice.dto.AvailabilityResponse
import com.clockwise.planningservice.services.AvailabilityService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime

@RestController
@RequestMapping("/v1")
class AvailabilityController(private val availabilityService: AvailabilityService) {

    @PostMapping("/availabilities")
    suspend fun createAvailability(@RequestBody request: AvailabilityRequest): ResponseEntity<AvailabilityResponse> {
        return try {
            val availability = availabilityService.createAvailability(request)
            ResponseEntity.status(HttpStatus.CREATED).body(availability)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @GetMapping("/availabilities/{id}")
    suspend fun getAvailabilityById(@PathVariable id: String): ResponseEntity<AvailabilityResponse> {
        return try {
            val availability = availabilityService.getAvailabilityById(id)
            ResponseEntity.ok(availability)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @PutMapping("/availabilities/{id}")
    suspend fun updateAvailability(
        @PathVariable id: String,
        @RequestBody request: AvailabilityRequest
    ): ResponseEntity<AvailabilityResponse> {
        return try {
            val availability = availabilityService.updateAvailability(id, request)
            ResponseEntity.ok(availability)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @DeleteMapping("/availabilities/{id}")
    suspend fun deleteAvailability(@PathVariable id: String): ResponseEntity<Void> {
        return try {
            availabilityService.deleteAvailability(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @GetMapping("/users/{id}/availabilities")
    suspend fun getEmployeeAvailabilities(@PathVariable id: String): ResponseEntity<List<AvailabilityResponse>> {
        val availabilities = availabilityService.getEmployeeAvailabilities(id).toList()
        return ResponseEntity.ok(availabilities)
    }
    
    @GetMapping("/users/me/availabilities")
    suspend fun getCurrentUserAvailabilities(
        @RequestParam(required = false) userId: String?
    ): ResponseEntity<List<AvailabilityResponse>> {
        // Use provided userId or get the authenticated user's ID
        val userIdToUse = userId ?: ReactiveSecurityContextHolder.getContext()
            .map { context: SecurityContext -> context.authentication.name }
            .block() ?: throw IllegalStateException("No authenticated user found")
        
        val availabilities = availabilityService.getEmployeeAvailabilities(userIdToUse).toList()
        return ResponseEntity.ok(availabilities)
    }

    @GetMapping("/restaurants/{id}/availabilities")
    suspend fun getRestaurantAvailabilities(
        @PathVariable id: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: ZonedDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: ZonedDateTime?
    ): ResponseEntity<List<AvailabilityResponse>> {
        val availabilities = if (startDate != null && endDate != null) {
            availabilityService.getRestaurantAvailabilitiesByDateRange(id, startDate, endDate).toList()
        } else {
            availabilityService.getRestaurantAvailabilities(id).toList()
        }
        return ResponseEntity.ok(availabilities)
    }

    @GetMapping("/business-units/{id}/availabilities")
    suspend fun getBusinessUnitAvailabilities(
        @PathVariable id: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: ZonedDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: ZonedDateTime?
    ): ResponseEntity<List<AvailabilityResponse>> {
        val availabilities = if (startDate != null && endDate != null) {
            availabilityService.getBusinessUnitAvailabilitiesByDateRange(id, startDate, endDate).toList()
        } else {
            availabilityService.getBusinessUnitAvailabilities(id).toList()
        }
        return ResponseEntity.ok(availabilities)
    }

    @GetMapping("/availabilities")
    suspend fun getAllAvailabilities(): ResponseEntity<List<AvailabilityResponse>> {
        // For now, return an empty list - this could be enhanced to filter by user's permissions
        val availabilities = availabilityService.getAllAvailabilities().toList()
        return ResponseEntity.ok(availabilities)
    }
} 