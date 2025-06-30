package com.clockwise.planningservice.controllers.workload

import com.clockwise.planningservice.dto.workload.ClockInRequest
import com.clockwise.planningservice.dto.workload.ClockOutRequest
import com.clockwise.planningservice.dto.workload.WorkHoursResponse
import com.clockwise.planningservice.dto.workload.WorkSessionResponse
import com.clockwise.planningservice.services.workload.WorkSessionService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneOffset

@RestController
@RequestMapping("/v1/work-sessions")
class WorkSessionController(private val workSessionService: WorkSessionService) {

    @PostMapping("/clock-in")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun clockIn(
        @RequestBody request: ClockInRequest,
        authentication: Authentication?
    ): ResponseEntity<WorkSessionResponse> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        val workSession = async { workSessionService.clockIn(request.userId, request.shiftId) }
        ResponseEntity(workSession.await(), HttpStatus.CREATED)
    }

    @PostMapping("/clock-out")
    suspend fun clockOut(
        @RequestBody request: ClockOutRequest,
        authentication: Authentication?
    ): ResponseEntity<WorkSessionResponse> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        val workSession = async { workSessionService.clockOut(request.userId, request.shiftId) }
        ResponseEntity(workSession.await(), HttpStatus.OK)
    }

    @GetMapping("/user/{userId}")
    suspend fun getUserWorkHours(
        @PathVariable userId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        authentication: Authentication?
    ): ResponseEntity<WorkHoursResponse> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        val startDateTime = startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
        val endDateTime = endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        
        val workHours = async { workSessionService.getEmployeeWorkHours(userId, startDateTime, endDateTime) }
        ResponseEntity(workHours.await(), HttpStatus.OK)
    }
} 