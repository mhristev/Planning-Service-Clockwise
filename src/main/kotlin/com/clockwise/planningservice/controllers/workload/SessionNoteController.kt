package com.clockwise.planningservice.controllers.workload

import com.clockwise.planningservice.dto.workload.SessionNoteRequest
import com.clockwise.planningservice.dto.workload.SessionNoteResponse
import com.clockwise.planningservice.services.workload.SessionNoteService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/session-notes")
class SessionNoteController(private val sessionNoteService: SessionNoteService) {

    /**
     * Create a new session note. Fails if a note already exists for the work session.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createNote(
        @RequestBody request: SessionNoteRequest,
        authentication: Authentication?
    ): ResponseEntity<SessionNoteResponse> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        try {
            val note = async { sessionNoteService.createNote(request) }
            ResponseEntity(note.await(), HttpStatus.CREATED)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    /**
     * Update an existing session note. Fails if no note exists for the work session.
     */
    @PutMapping
    suspend fun updateNote(
        @RequestBody request: SessionNoteRequest,
        authentication: Authentication?
    ): ResponseEntity<SessionNoteResponse> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        try {
            val note = async { sessionNoteService.updateNote(request) }
            ResponseEntity(note.await(), HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Create or update a session note. Creates if none exists, updates if one exists.
     * This is the recommended endpoint for managing session notes.
     */
    @PutMapping("/upsert")
    suspend fun createOrUpdateNote(
        @RequestBody request: SessionNoteRequest,
        authentication: Authentication?
    ): ResponseEntity<SessionNoteResponse> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        val note = async { sessionNoteService.createOrUpdateNote(request) }
        ResponseEntity(note.await(), HttpStatus.OK)
    }

    /**
     * Get the single session note for a work session.
     */
    @GetMapping("/work-session/{workSessionId}")
    suspend fun getNoteByWorkSessionId(
        @PathVariable workSessionId: String,
        authentication: Authentication?
    ): ResponseEntity<SessionNoteResponse> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        val note = async { sessionNoteService.getNoteByWorkSessionId(workSessionId) }
        val result = note.await()
        if (result != null) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * DEPRECATED: Get notes by work session ID (returns Flow for backwards compatibility).
     * This endpoint is kept for backwards compatibility but will only return one note per work session.
     */
    @GetMapping("/work-session/{workSessionId}/all")
    @Deprecated("Use GET /work-session/{workSessionId} instead")
    suspend fun getNotesByWorkSessionId(
        @PathVariable workSessionId: String,
        authentication: Authentication?
    ): ResponseEntity<Flow<SessionNoteResponse>> = coroutineScope {
        val authenticatedUserId = authentication?.name ?: "anonymous"
        val notes = async { sessionNoteService.getNotesByWorkSessionId(workSessionId) }
        ResponseEntity(notes.await(), HttpStatus.OK)
    }
} 