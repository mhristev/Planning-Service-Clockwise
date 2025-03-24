package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.ScheduleRequest
import com.clockwise.planningservice.dto.ScheduleResponse
import com.clockwise.planningservice.services.ScheduleService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1")
class ScheduleController(private val scheduleService: ScheduleService) {

    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createSchedule(@RequestBody request: ScheduleRequest): ScheduleResponse {
        return scheduleService.createSchedule(request)
    }

    @GetMapping("/schedules/{id}")
    suspend fun getScheduleById(@PathVariable id: String): ScheduleResponse {
        return scheduleService.getScheduleById(id)
    }

    @PutMapping("/schedules/{id}")
    suspend fun updateSchedule(
        @PathVariable id: String,
        @RequestBody request: ScheduleRequest
    ): ScheduleResponse {
        return scheduleService.updateSchedule(id, request)
    }

    @PostMapping("/schedules/{id}/publish")
    suspend fun publishSchedule(@PathVariable id: String): ScheduleResponse {
        return scheduleService.publishSchedule(id)
    }

    @GetMapping("/restaurants/{id}/schedules")
    fun getRestaurantSchedules(@PathVariable id: String): Flow<ScheduleResponse> {
        return scheduleService.getRestaurantSchedules(id)
    }

    @GetMapping("/restaurants/{id}/schedules/current")
    suspend fun getCurrentRestaurantSchedule(@PathVariable id: String): ScheduleResponse? {
        return scheduleService.getCurrentRestaurantSchedule(id)
    }

    @PutMapping("/schedules/{id}/published")
    suspend fun updatePublishedSchedule(
        @PathVariable id: String,
        @RequestBody request: ScheduleRequest
    ): ScheduleResponse {
        return scheduleService.updatePublishedSchedule(id, request)
    }
} 