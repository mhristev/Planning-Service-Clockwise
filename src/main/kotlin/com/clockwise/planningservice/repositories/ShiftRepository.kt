package com.clockwise.planningservice.repositories

import com.clockwise.planningservice.domains.Shift
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
interface ShiftRepository : CoroutineCrudRepository<Shift, String> {

    fun findByScheduleId(scheduleId: String): Flow<Shift>

    fun findByEmployeeId(employeeId: String): Flow<Shift>

    fun findByScheduleIdAndEmployeeId(scheduleId: String, employeeId: String): Flow<Shift>

    suspend fun deleteByScheduleId(scheduleId: String): Int
    
    @Query("""
        SELECT s.* FROM shifts s
        INNER JOIN schedules sc ON s.schedule_id = sc.id
        WHERE sc.business_unit_id = :businessUnitId
        AND s.start_time >= :startDate
        AND s.start_time <= :endDate
        ORDER BY s.start_time ASC
    """)
    fun findByBusinessUnitIdAndDateRange(
        businessUnitId: String,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime
    ): Flow<Shift>
}