package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.ShiftRequest
import com.clockwise.planningservice.dto.ShiftResponse
import com.clockwise.planningservice.services.ShiftService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

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