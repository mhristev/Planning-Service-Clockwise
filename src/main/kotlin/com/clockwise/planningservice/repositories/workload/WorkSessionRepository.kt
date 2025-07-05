package com.clockwise.planningservice.repositories.workload

import com.clockwise.planningservice.domains.workload.WorkSession
import com.clockwise.planningservice.domains.workload.WorkSessionStatus
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.OffsetDateTime

interface WorkSessionRepository : CoroutineCrudRepository<WorkSession, String> {
    
    fun findByUserIdAndStatus(userId: String, status: WorkSessionStatus): Flow<WorkSession>
    
    fun findByUserIdAndShiftId(userId: String, shiftId: String): Flow<WorkSession>
    
    fun findByUserIdAndClockInTimeBetween(
        userId: String, 
        startTime: OffsetDateTime, 
        endTime: OffsetDateTime
    ): Flow<WorkSession>
    
    @Query("SELECT * FROM work_sessions WHERE user_id = :userId AND shift_id = :shiftId AND status = 'ACTIVE' LIMIT 1")
    suspend fun findActiveByUserIdAndShiftId(userId: String, shiftId: String): WorkSession?
    
    @Query("SELECT COUNT(*) > 0 FROM work_sessions WHERE shift_id = :shiftId")
    suspend fun existsByShiftId(shiftId: String): Boolean
    
    suspend fun findByShiftId(shiftId: String): WorkSession?
    
    @Query("""
        SELECT ws.* FROM work_sessions ws
        INNER JOIN shifts s ON ws.shift_id = s.id
        INNER JOIN schedules sc ON s.schedule_id = sc.id
        WHERE sc.business_unit_id = :businessUnitId
        AND ws.confirmed = false
        ORDER BY ws.created_at DESC
    """)
    fun findUnconfirmedByBusinessUnitId(businessUnitId: String): Flow<WorkSession>
    
    @Query("SELECT * FROM work_sessions WHERE confirmed = :confirmed")
    fun findByConfirmed(confirmed: Boolean): Flow<WorkSession>
    
    @Query("SELECT * FROM work_sessions WHERE confirmed_by = :confirmedBy")
    fun findByConfirmedBy(confirmedBy: String): Flow<WorkSession>
} 