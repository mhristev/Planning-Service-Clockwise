package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.ScheduleRequest
import com.clockwise.planningservice.dto.ScheduleResponse
import com.clockwise.planningservice.services.ScheduleService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime
import java.time.ZonedDateTime
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1")
class ScheduleController(private val scheduleService: ScheduleService) {

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

    @PostMapping("/schedules")
    suspend fun createSchedule(
        @RequestBody request: ScheduleRequest,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to create schedule" }
        
        val response = scheduleService.createSchedule(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/schedules/{id}")
    suspend fun getScheduleById(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to get schedule with ID: $id" }
        
        return try {
            val schedule = scheduleService.getScheduleById(id)
            ResponseEntity.ok(schedule)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @PutMapping("/schedules/{id}")
    suspend fun updateSchedule(
        @PathVariable id: String,
        @RequestBody request: ScheduleRequest,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to update schedule with ID: $id" }
        
        return try {
            val schedule = scheduleService.updateSchedule(id, request)
            ResponseEntity.ok(schedule)
        } catch (e: Exception) {
            when (e) {
                is IllegalStateException -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
                else -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }
        }
    }

    @PostMapping("/schedules/{id}/publish")
    suspend fun publishSchedule(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to publish schedule with ID: $id" }
        
        return try {
            val schedule = scheduleService.publishSchedule(id)
            ResponseEntity.ok(schedule)
        } catch (e: Exception) {
            when (e) {
                is IllegalStateException -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
                else -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }
        }
    }

    @GetMapping("/restaurants/{id}/schedules")
    suspend fun getRestaurantSchedules(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<List<ScheduleResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested schedules for restaurant ID: $id" }
        
        val schedules = scheduleService.getRestaurantSchedules(id).toList()
        return ResponseEntity.ok(schedules)
    }

    @GetMapping("/restaurants/{id}/schedules/current")
    suspend fun getCurrentRestaurantSchedule(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested current schedule for restaurant ID: $id" }
        
        val schedule = scheduleService.getCurrentRestaurantSchedule(id)
        return if (schedule != null) {
            ResponseEntity.ok(schedule)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @GetMapping("/restaurants/{id}/schedules/week")
    suspend fun getScheduleByWeekStart(
        @PathVariable id: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) weekStart: ZonedDateTime,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested schedule for restaurant ID: $id, week start: $weekStart" }
        
        return try {
            val schedule = scheduleService.getScheduleByWeekStart(id, weekStart)
            if (schedule != null) {
                ResponseEntity.ok(schedule)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    // dont use that!!!!
    @PutMapping("/schedules/{id}/published")
    suspend fun updatePublishedSchedule(
        @PathVariable id: String,
        @RequestBody request: ScheduleRequest,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to update published schedule with ID: $id" }
        
        return try {
            val schedule = scheduleService.updatePublishedSchedule(id, request)
            ResponseEntity.ok(schedule)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @PostMapping("/schedules/{id}/draft")
    suspend fun revertToDraft(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to revert schedule with ID: $id to draft" }
        
        return try {
            val schedule = scheduleService.revertToDraft(id)
            ResponseEntity.ok(schedule)
        } catch (e: Exception) {
            when (e) {
                is IllegalStateException -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
                else -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }
        }
    }

    @GetMapping("/schedules")
    suspend fun getAllSchedules(
        authentication: Authentication
    ): ResponseEntity<List<ScheduleResponse>> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested all schedules" }
        
        // For now, return an empty list - this could be enhanced to filter by user's restaurant/organization
        val schedules = scheduleService.getAllSchedules().toList()
        return ResponseEntity.ok(schedules)
    }
} 