package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.AvailabilityRequest
import com.clockwise.planningservice.dto.AvailabilityResponse
import com.clockwise.planningservice.services.AvailabilityService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.ZonedDateTime
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1")
class AvailabilityController(private val availabilityService: AvailabilityService) {

    private fun extractUserInfo(authentication: Authentication): Map<String, Any?> {
        val jwt = authentication.principal as Jwt
        return mapOf(
            "userId" to jwt.getClaimAsString("sub"),
            "email" to jwt.getClaimAsString("email"),
            "firstName" to jwt.getClaimAsString("given_name"),
            "lastName" to jwt.getClaimAsString("family_name"),
            "roles" to jwt.getClaimAsStringList("roles")
        )
    }

    @PostMapping("/availabilities")
    suspend fun createAvailability(
        @RequestBody request: AvailabilityRequest,
        authentication: Authentication
    ): ResponseEntity<AvailabilityResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to create availability" }
        
        return try {
            val availability = availabilityService.createAvailability(request)
            ResponseEntity.status(HttpStatus.CREATED).body(availability)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @GetMapping("/availabilities/{id}")
    suspend fun getAvailabilityById(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<AvailabilityResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to get availability with ID: $id" }
        
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
        @RequestBody request: AvailabilityRequest,
        authentication: Authentication
    ): ResponseEntity<AvailabilityResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to update availability with ID: $id" }
        
        return try {
            val availability = availabilityService.updateAvailability(id, request)
            ResponseEntity.ok(availability)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @DeleteMapping("/availabilities/{id}")
    suspend fun deleteAvailability(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to delete availability with ID: $id" }
        
        return try {
            availabilityService.deleteAvailability(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @GetMapping("/users/{userId}/availabilities")
    suspend fun getEmployeeAvailabilities(
        @PathVariable userId: String?,
        authentication: Authentication
    ): ResponseEntity<List<AvailabilityResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested availabilities for user ID: $userId" }
        
        val userIdToUse = userId ?: userInfo["userId"] as String
        
        val availabilities = availabilityService.getEmployeeAvailabilities(userIdToUse).toList()
        return ResponseEntity.ok(availabilities)
    }

    @GetMapping("/business-units/{id}/availabilities")
    suspend fun getBusinessUnitAvailabilities(
        @PathVariable id: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: ZonedDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: ZonedDateTime?,
        authentication: Authentication
    ): ResponseEntity<List<AvailabilityResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested availabilities for business unit ID: $id" }
        
        val availabilities = if (startDate != null && endDate != null) {
            availabilityService.getBusinessUnitAvailabilitiesByDateRange(id, startDate, endDate).toList()
        } else {
            availabilityService.getBusinessUnitAvailabilities(id).toList()
        }
        return ResponseEntity.ok(availabilities)
    }

    @GetMapping("/availabilities")
    suspend fun getAllAvailabilities(
        authentication: Authentication
    ): ResponseEntity<List<AvailabilityResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested all availabilities" }
        
        val availabilities = availabilityService.getAllAvailabilities().toList()
        return ResponseEntity.ok(availabilities)
    }
} 