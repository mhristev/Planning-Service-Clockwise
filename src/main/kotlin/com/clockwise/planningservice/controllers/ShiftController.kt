package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.ShiftRequest
import com.clockwise.planningservice.dto.ShiftResponse
import com.clockwise.planningservice.dto.ShiftWithWorkSessionResponse
import com.clockwise.planningservice.services.ShiftService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.format.annotation.DateTimeFormat
import java.time.ZonedDateTime
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1")
class ShiftController(private val shiftService: ShiftService) {

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

    @PostMapping("/shifts")
    suspend fun createShift(
        @RequestBody request: ShiftRequest,
        authentication: Authentication
    ): ResponseEntity<ShiftResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to create shift" }
        
        return try {
            val shift = shiftService.createShift(request)
            ResponseEntity.status(HttpStatus.CREATED).body(shift)
        } catch (e: Exception) {
            when (e) {
                is IllegalStateException -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
                else -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }
        }
    }

    @GetMapping("/shifts/{id}")
    suspend fun getShiftById(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ShiftResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to get shift with ID: $id" }
        
        return try {
            val shift = shiftService.getShiftById(id)
            ResponseEntity.ok(shift)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @PutMapping("/shifts/{id}")
    suspend fun updateShift(
        @PathVariable id: String,
        @RequestBody request: ShiftRequest,
        authentication: Authentication
    ): ResponseEntity<ShiftResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to update shift with ID: $id" }
        
        return try {
            val shift = shiftService.updateShift(id, request)
            ResponseEntity.ok(shift)
        } catch (e: Exception) {
            when (e) {
                is IllegalStateException -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
                else -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }
        }
    }

    @DeleteMapping("/shifts/{id}")
    suspend fun deleteShift(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to delete shift with ID: $id" }
        
        return try {
            shiftService.deleteShift(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            when (e) {
                is IllegalStateException -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
                else -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }
        }
    }

    @GetMapping("/schedules/{id}/shifts")
    suspend fun getScheduleShifts(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<List<ShiftResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested shifts for schedule ID: $id" }
        
        val shifts = shiftService.getScheduleShifts(id).toList()
        return ResponseEntity.ok(shifts)
    }

    @GetMapping("/users/{id}/shifts")
    suspend fun getEmployeeShifts(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<List<ShiftResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested shifts for employee ID: $id" }
        
        val shifts = shiftService.getEmployeeShifts(id).toList()
        return ResponseEntity.ok(shifts)
    }
    
    @GetMapping("/users/{id}/shifts/upcoming")
    suspend fun getUpcomingEmployeeShifts(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<List<ShiftResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested upcoming shifts for employee ID: $id" }
        
        val shifts = shiftService.getUpcomingEmployeeShifts(id).toList()
        return ResponseEntity.ok(shifts)
    }
    
    @GetMapping("/business-units/{id}/shifts/week")
    suspend fun getBusinessUnitShiftsForWeek(
        @PathVariable id: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) weekStart: ZonedDateTime,
        authentication: Authentication
    ): ResponseEntity<List<ShiftResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested shifts for business unit ID: $id, week start: $weekStart" }
        
        val shifts = shiftService.getBusinessUnitShiftsForWeek(id, weekStart).toList()
        return ResponseEntity.ok(shifts)
    }
    
    @GetMapping("/business-units/{id}/shifts/day")
    suspend fun getBusinessUnitShiftsForDay(
        @PathVariable id: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) date: ZonedDateTime,
        authentication: Authentication
    ): ResponseEntity<List<ShiftResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested shifts for business unit ID: $id, date: $date" }
        
        val shifts = shiftService.getBusinessUnitShiftsForDay(id, date).toList()
        return ResponseEntity.ok(shifts)
    }

    /**
     * ADMIN/MANAGER ENDPOINT: Get comprehensive shifts with work sessions and session notes
     * Available to admin and manager roles only
     * Returns all shifts with their associated work sessions and session notes for a date range
     */
    @GetMapping("/business-units/{businessUnitId}/shifts/comprehensive")
    suspend fun getShiftsWithWorkSessionsAndNotes(
        @PathVariable businessUnitId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: ZonedDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: ZonedDateTime,
        authentication: Authentication
    ): ResponseEntity<List<ShiftWithWorkSessionResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested comprehensive shifts for business unit ID: $businessUnitId, from: $startDate to: $endDate" }
        
        val shifts = shiftService.getShiftsWithWorkSessionsAndNotes(businessUnitId, startDate, endDate)
        return ResponseEntity.ok(shifts)
    }
} 