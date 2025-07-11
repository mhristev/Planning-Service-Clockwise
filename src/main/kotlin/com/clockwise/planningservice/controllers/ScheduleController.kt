package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.ScheduleRequest
import com.clockwise.planningservice.dto.ScheduleResponse
import com.clockwise.planningservice.dto.ScheduleWithShiftsResponse
import com.clockwise.planningservice.dto.MonthlyScheduleDto
import com.clockwise.planningservice.services.ScheduleService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
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
    ): ResponseEntity<ScheduleResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to create schedule" }
        
        val response = async { scheduleService.createSchedule(request) }
        ResponseEntity.status(HttpStatus.CREATED).body(response.await())
    }

    @GetMapping("/schedules/{id}")
    suspend fun getScheduleById(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to get schedule with ID: $id" }
        
        val schedule = async { scheduleService.getScheduleById(id) }
        ResponseEntity.ok(schedule.await())
    }

    @PutMapping("/schedules/{id}")
    suspend fun updateSchedule(
        @PathVariable id: String,
        @RequestBody request: ScheduleRequest,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to update schedule with ID: $id" }
        
        val schedule = async { scheduleService.updateSchedule(id, request) }
        ResponseEntity.ok(schedule.await())
    }

    @PostMapping("/schedules/{id}/publish")
    suspend fun publishSchedule(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to publish schedule with ID: $id" }
        
        val schedule = async { scheduleService.publishSchedule(id) }
        ResponseEntity.ok(schedule.await())
    }

    // in the web app theres a button to create schedule check if its fully connected to the planning service endpoint /v1/schedules post create schedule 

    @GetMapping("/business-units/{id}/schedules")
    suspend fun getBusinessUnitSchedules(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<Flow<ScheduleResponse>> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested schedules for business unit ID: $id" }
        
        val schedulesFlow = async { scheduleService.getBusinessUnitSchedules(id) }
        ResponseEntity.ok(schedulesFlow.await())
    }

    @GetMapping("/business-units/{id}/schedules/current")
    suspend fun getCurrentBusinessUnitSchedule(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse?> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested current schedule for business unit ID: $id" }
        
        val schedule = async { scheduleService.getCurrentBusinessUnitSchedule(id) }
        val result = schedule.await()
        if (result != null) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @PostMapping("/schedules/{id}/draft")
    suspend fun revertToDraft(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ScheduleResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested to revert schedule with ID: $id to draft" }
        
        val schedule = async { scheduleService.revertToDraft(id) }
        ResponseEntity.ok(schedule.await())
    }

    @GetMapping("/schedules")
    suspend fun getAllSchedules(
        authentication: Authentication
    ): ResponseEntity<Flow<ScheduleResponse>> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested all schedules" }
        
        val schedulesFlow = async { scheduleService.getAllSchedules() }
        ResponseEntity.ok(schedulesFlow.await())
    }

    /**
     * PUBLIC ENDPOINT: Get a published schedule with shifts by business unit and week
     * Available to all authenticated users (admin, manager, user)
     * Only returns published schedules
     */
    @GetMapping("/business-units/{businessUnitId}/schedules/week/published")
    suspend fun getPublishedScheduleWithShiftsByWeek(
        @PathVariable businessUnitId: String,
        @RequestParam weekStart: LocalDate
    ): ResponseEntity<ScheduleWithShiftsResponse> {
        val schedule = scheduleService.getPublishedScheduleWithShiftsByWeek(businessUnitId, weekStart)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(schedule)
    }

    /**
     * ADMIN ENDPOINT: Get any schedule with shifts by business unit and week
     * Available to admin and manager roles only
     * Returns schedules regardless of status
     */
    @GetMapping("/business-units/{businessUnitId}/schedules/week")
    suspend fun getScheduleWithShiftsByWeekForAdmin(
        @PathVariable businessUnitId: String,
        @RequestParam weekStart: LocalDate,
        authentication: Authentication
    ): ResponseEntity<ScheduleWithShiftsResponse> {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested schedule with shifts for business unit ID: $businessUnitId, week start: $weekStart" }
        
        val schedule = scheduleService.getScheduleWithShiftsByWeekForAdmin(businessUnitId, weekStart)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(schedule)
    }

    /**
     * MANAGER ENDPOINT: Get monthly schedule for a specific employee
     * Available to admin and manager roles only
     * Returns comprehensive schedule data including work sessions and notes
     */
    @GetMapping("/business-units/{businessUnitId}/users/{userId}/monthly-schedule")
    suspend fun getMonthlyScheduleForUser(
        @PathVariable businessUnitId: String,
        @PathVariable userId: String,
        @RequestParam month: Int,
        @RequestParam year: Int,
        authentication: Authentication
    ): ResponseEntity<List<MonthlyScheduleDto>> {
        val userInfo = extractUserInfo(authentication)
        
        logger.info { "User ${userInfo["email"]} requested monthly schedule for user $userId in business unit $businessUnitId for $month/$year" }
        
        // Validate month parameter
        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().build()
        }
        
        val monthlySchedule = scheduleService.getMonthlyScheduleForUser(businessUnitId, userId, month, year)
        return ResponseEntity.ok(monthlySchedule)
    }
} 
// so in the webview make when th euser that logs in is admin make it also have extra pages. Start with first one that allows the admin to modify current users in the app by changing their role and their businessUnitId and businessUnitName for now add it to the side menu and the page should be accessibel only for admins. Connect it to the endpoints in the backend to make it work
