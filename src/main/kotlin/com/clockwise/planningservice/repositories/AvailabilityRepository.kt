package com.clockwise.planningservice.repositories

import com.clockwise.planningservice.domains.Availability
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
interface AvailabilityRepository : CoroutineCrudRepository<Availability, String> {

    fun findByEmployeeId(employeeId: String): Flow<Availability>

    fun findByBusinessUnitId(businessUnitId: String): Flow<Availability>

    @Query("""
        SELECT * FROM availabilities a WHERE 
        a.business_unit_id = :businessUnitId
        AND a.start_time <= :endDate AND a.end_time >= :startDate
    """)
    fun findByBusinessUnitIdAndDateRange(
        businessUnitId: String,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime
    ): Flow<Availability>
} 