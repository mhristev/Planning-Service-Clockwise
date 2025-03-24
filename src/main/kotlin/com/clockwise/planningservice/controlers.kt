package com.clockwise.planningservice

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime
//import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

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


@RestController
@RequestMapping("/v1")
class ShiftController(private val shiftService: ShiftService) {

    @PostMapping("/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createShift(@RequestBody request: ShiftRequest): ShiftResponse {
        return shiftService.createShift(request)
    }

    @GetMapping("/shifts/{id}")
    suspend fun getShiftById(@PathVariable id: String): ShiftResponse {
        return shiftService.getShiftById(id)
    }

    @PutMapping("/shifts/{id}")
    suspend fun updateShift(
        @PathVariable id: String,
        @RequestBody request: ShiftRequest
    ): ShiftResponse {
        return shiftService.updateShift(id, request)
    }

    @DeleteMapping("/shifts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteShift(@PathVariable id: String) {
        shiftService.deleteShift(id)
    }

    @GetMapping("/schedules/{id}/shifts")
    fun getScheduleShifts(@PathVariable id: String): Flow<ShiftResponse> {
        return shiftService.getScheduleShifts(id)
    }

    @GetMapping("/users/{id}/shifts")
    fun getEmployeeShifts(@PathVariable id: String): Flow<ShiftResponse> {
        return shiftService.getEmployeeShifts(id)
    }
}