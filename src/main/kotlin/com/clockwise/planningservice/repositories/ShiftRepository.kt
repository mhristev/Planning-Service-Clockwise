package com.clockwise.planningservice.repositories

import com.clockwise.planningservice.domains.Shift
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ShiftRepository : CoroutineCrudRepository<Shift, String> {

    fun findByScheduleId(scheduleId: String): Flow<Shift>

    fun findByEmployeeId(employeeId: String): Flow<Shift>

    suspend fun deleteByScheduleId(scheduleId: String): Int
} 