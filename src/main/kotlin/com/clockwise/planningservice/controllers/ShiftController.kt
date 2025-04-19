package com.clockwise.planningservice.controllers

import com.clockwise.planningservice.dto.ShiftRequest
import com.clockwise.planningservice.dto.ShiftResponse
import com.clockwise.planningservice.services.ShiftService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1")
class ShiftController(private val shiftService: ShiftService) {

    @PostMapping("/shifts")
    suspend fun createShift(@RequestBody request: ShiftRequest): ResponseEntity<ShiftResponse> {
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
    suspend fun getShiftById(@PathVariable id: String): ResponseEntity<ShiftResponse> {
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
        @RequestBody request: ShiftRequest
    ): ResponseEntity<ShiftResponse> {
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
    suspend fun deleteShift(@PathVariable id: String): ResponseEntity<Void> {
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
    suspend fun getScheduleShifts(@PathVariable id: String): ResponseEntity<List<ShiftResponse>> {
        val shifts = shiftService.getScheduleShifts(id).toList()
        return ResponseEntity.ok(shifts)
    }

    @GetMapping("/users/{id}/shifts")
    suspend fun getEmployeeShifts(@PathVariable id: String): ResponseEntity<List<ShiftResponse>> {
        val shifts = shiftService.getEmployeeShifts(id).toList()
        return ResponseEntity.ok(shifts)
    }
} 