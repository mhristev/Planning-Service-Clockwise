package com.clockwise.planningservice

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AvailabilityRepository : CoroutineCrudRepository<Availability, String> {

    fun findByEmployeeId(employeeId: String): Flow<Availability>

    @Query("SELECT * FROM availabilities a WHERE a.employee_id IN " +
            "(SELECT user_id FROM restaurant_employees WHERE restaurant_id = :restaurantId)")
    fun findByRestaurantId(restaurantId: String): Flow<Availability>

    @Query("""
        SELECT * FROM availabilities a WHERE 
        a.employee_id IN (SELECT user_id FROM restaurant_employees WHERE restaurant_id = :restaurantId)
        AND a.start_time >= :startDate AND a.end_time <= :endDate
    """)
    fun findByRestaurantIdAndDateRange(
        restaurantId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<Availability>
}


@Repository
interface ScheduleRepository : CoroutineCrudRepository<Schedule, String> {

    fun findByRestaurantId(restaurantId: String): Flow<Schedule>

    @Query("""
        SELECT * FROM schedules 
        WHERE restaurant_id = :restaurantId 
        AND week_start <= CURRENT_DATE 
        AND week_start + INTERVAL '7 days' > CURRENT_DATE
        ORDER BY week_start DESC 
        LIMIT 1
    """)
    suspend fun findCurrentScheduleByRestaurantId(restaurantId: String): Schedule?

    @Query("UPDATE schedules SET status = :status, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    suspend fun updateStatus(id: String, status: ScheduleStatus): Boolean
}


@Repository
interface ShiftRepository : CoroutineCrudRepository<Shift, String> {

    fun findByScheduleId(scheduleId: String): Flow<Shift>

    fun findByEmployeeId(employeeId: String): Flow<Shift>

    suspend fun deleteByScheduleId(scheduleId: String): Int
}