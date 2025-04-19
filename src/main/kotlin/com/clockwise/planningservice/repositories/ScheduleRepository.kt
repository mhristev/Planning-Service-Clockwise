package com.clockwise.planningservice.repositories

import com.clockwise.planningservice.domain.ScheduleStatus
import com.clockwise.planningservice.domains.Schedule
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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

    @Query("""
        SELECT * FROM schedules 
        WHERE restaurant_id = :restaurantId 
        AND DATE(week_start) = DATE(:weekStart)
        LIMIT 1
    """)
    suspend fun findByRestaurantIdAndWeekStart(restaurantId: String, weekStart: LocalDateTime): Schedule?

    @Query("UPDATE schedules SET status = :status, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    suspend fun updateStatus(id: String, status: ScheduleStatus): Boolean
} 