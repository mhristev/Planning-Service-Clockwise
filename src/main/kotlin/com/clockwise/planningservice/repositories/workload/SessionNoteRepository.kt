package com.clockwise.planningservice.repositories.workload

import com.clockwise.planningservice.domains.workload.SessionNote
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SessionNoteRepository : CoroutineCrudRepository<SessionNote, String> {
    
    fun findByWorkSessionId(workSessionId: String): Flow<SessionNote>
    
    suspend fun findSingleByWorkSessionId(workSessionId: String): SessionNote?
} 