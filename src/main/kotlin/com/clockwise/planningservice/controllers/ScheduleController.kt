package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.ScheduleRequest
import com.clockwise.planningservice.dto.ScheduleResponse
import com.clockwise.planningservice.services.ScheduleService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@RestController
@RequestMapping("/v1")
class ScheduleController(private val scheduleService: ScheduleService) {

    @PostMapping("/schedules")
    suspend fun createSchedule(@RequestBody request: ScheduleRequest): ResponseEntity<ScheduleResponse> {
        val response = scheduleService.createSchedule(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/schedules/{id}")
    suspend fun getScheduleById(@PathVariable id: String): ResponseEntity<ScheduleResponse> {
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
        @RequestBody request: ScheduleRequest
    ): ResponseEntity<ScheduleResponse> {
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
    suspend fun publishSchedule(@PathVariable id: String): ResponseEntity<ScheduleResponse> {
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
    suspend fun getRestaurantSchedules(@PathVariable id: String): ResponseEntity<List<ScheduleResponse>> {
        val schedules = scheduleService.getRestaurantSchedules(id).toList()
        return ResponseEntity.ok(schedules)
    }

    @GetMapping("/restaurants/{id}/schedules/current")
    suspend fun getCurrentRestaurantSchedule(@PathVariable id: String): ResponseEntity<ScheduleResponse> {
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
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) weekStart: LocalDateTime
    ): ResponseEntity<ScheduleResponse> {
        val schedule = scheduleService.getScheduleByWeekStart(id, weekStart)
        return if (schedule != null) {
            ResponseEntity.ok(schedule)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    // dont use that!!!!
    @PutMapping("/schedules/{id}/published")
    suspend fun updatePublishedSchedule(
        @PathVariable id: String,
        @RequestBody request: ScheduleRequest
    ): ResponseEntity<ScheduleResponse> {
        return try {
            val schedule = scheduleService.updatePublishedSchedule(id, request)
            ResponseEntity.ok(schedule)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @PostMapping("/schedules/{id}/draft")
    suspend fun revertToDraft(@PathVariable id: String): ResponseEntity<ScheduleResponse> {
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
} 