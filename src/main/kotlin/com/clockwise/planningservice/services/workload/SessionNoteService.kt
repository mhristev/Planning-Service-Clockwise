package com.clockwise.planningservice.services.workload

import com.clockwise.planningservice.dto.workload.SessionNoteRequest
import com.clockwise.planningservice.dto.workload.SessionNoteResponse
import com.clockwise.planningservice.domains.workload.SessionNote
import com.clockwise.planningservice.repositories.workload.SessionNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class SessionNoteService(private val sessionNoteRepository: SessionNoteRepository) {

    suspend fun createNote(request: SessionNoteRequest): SessionNoteResponse {
        // Check if a note already exists for this work session
        val existingNote = sessionNoteRepository.findSingleByWorkSessionId(request.workSessionId)
        if (existingNote != null) {
            throw IllegalStateException("A session note already exists for work session ${request.workSessionId}. Use updateNote to modify it.")
        }
        
        val note = SessionNote(
            workSessionId = request.workSessionId,
            content = request.content
        )
        
        val savedNote = sessionNoteRepository.save(note)
        return toSessionNoteResponse(savedNote)
    }

    suspend fun updateNote(request: SessionNoteRequest): SessionNoteResponse {
        val existingNote = sessionNoteRepository.findSingleByWorkSessionId(request.workSessionId)
            ?: throw IllegalArgumentException("No session note found for work session ${request.workSessionId}")
        
        val updatedNote = existingNote.copy(
            content = request.content,
            updatedAt = OffsetDateTime.now()
        )
        
        val savedNote = sessionNoteRepository.save(updatedNote)
        return toSessionNoteResponse(savedNote)
    }

    suspend fun createOrUpdateNote(request: SessionNoteRequest): SessionNoteResponse {
        val existingNote = sessionNoteRepository.findSingleByWorkSessionId(request.workSessionId)
        
        return if (existingNote != null) {
            // Update existing note
            val updatedNote = existingNote.copy(
                content = request.content,
                updatedAt = OffsetDateTime.now()
            )
            val savedNote = sessionNoteRepository.save(updatedNote)
            toSessionNoteResponse(savedNote)
        } else {
            // Create new note
            val note = SessionNote(
                workSessionId = request.workSessionId,
                content = request.content
            )
            val savedNote = sessionNoteRepository.save(note)
            toSessionNoteResponse(savedNote)
        }
    }

    suspend fun getNoteByWorkSessionId(workSessionId: String): SessionNoteResponse? {
        val note = sessionNoteRepository.findSingleByWorkSessionId(workSessionId)
        return note?.let { toSessionNoteResponse(it) }
    }

    suspend fun getSessionNoteByWorkSessionId(workSessionId: String): SessionNote? {
        return sessionNoteRepository.findSingleByWorkSessionId(workSessionId)
    }

    fun getNotesByWorkSessionId(workSessionId: String): Flow<SessionNoteResponse> {
        return sessionNoteRepository.findByWorkSessionId(workSessionId)
            .map { toSessionNoteResponse(it) }
    }

    private fun toSessionNoteResponse(sessionNote: SessionNote): SessionNoteResponse {
        return SessionNoteResponse(
            id = sessionNote.id,
            workSessionId = sessionNote.workSessionId,
            content = sessionNote.content,
            createdAt = sessionNote.createdAt
        )
    }

    fun toResponse(sessionNote: SessionNote?): SessionNoteResponse? {
        return sessionNote?.let {
            SessionNoteResponse(
                id = it.id,
                workSessionId = it.workSessionId,
                content = it.content,
                createdAt = it.createdAt
            )
        }
    }
}