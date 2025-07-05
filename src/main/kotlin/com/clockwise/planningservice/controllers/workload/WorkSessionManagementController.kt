package com.clockwise.planningservice.controllers.workload

import com.clockwise.planningservice.dto.workload.*
import com.clockwise.planningservice.services.workload.WorkSessionService
import com.clockwise.planningservice.services.workload.SessionNoteService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/v1/work-sessions/management")
class WorkSessionManagementController(
    private val workSessionService: WorkSessionService,
    private val sessionNoteService: SessionNoteService
) {

    private fun extractUserInfo(authentication: Authentication): Map<String, Any?> {
        val jwt = authentication.principal as Jwt
        return mapOf(
            "userId" to jwt.getClaimAsString("sub"),
            "email" to jwt.getClaimAsString("email"),
            "firstName" to jwt.getClaimAsString("given_name"),
            "lastName" to jwt.getClaimAsString("family_name"),
            "roles" to jwt.getClaimAsStringList("roles")
        )
    }

    /**
     * Get all unconfirmed work sessions for a business unit
     * Available to managers and admins only
     */
    @GetMapping("/business-units/{businessUnitId}/unconfirmed")
    suspend fun getUnconfirmedWorkSessions(
        @PathVariable businessUnitId: String,
        authentication: Authentication
    ): ResponseEntity<UnconfirmedWorkSessionsResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} requested unconfirmed work sessions for business unit: $businessUnitId" }

        val unconfirmedSessions = async { 
            workSessionService.getUnconfirmedWorkSessionsWithShiftInfo(businessUnitId) 
        }
        
        val sessions = unconfirmedSessions.await()
        
        // Add session notes to each work session
        val sessionsWithNotes = sessions.map { session ->
            val sessionNote = session.id?.let { sessionId ->
                try {
                    sessionNoteService.getNoteByWorkSessionId(sessionId)
                } catch (e: Exception) {
                    null
                }
            }
            
            session.copy(sessionNote = sessionNote)
        }
        
        val response = UnconfirmedWorkSessionsResponse(
            businessUnitId = businessUnitId,
            totalUnconfirmed = sessionsWithNotes.size,
            workSessions = sessionsWithNotes
        )
        
        ResponseEntity.ok(response)
    }

    /**
     * Confirm a work session
     * Available to managers and admins only
     */
    @PostMapping("/confirm")
    suspend fun confirmWorkSession(
        @RequestBody request: WorkSessionConfirmationRequest,
        authentication: Authentication
    ): ResponseEntity<WorkSessionResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} confirming work session: ${request.workSessionId}" }

        try {
            val confirmedSession = async { 
                workSessionService.confirmWorkSession(request.workSessionId, userInfo["userId"] as String) 
            }
            ResponseEntity.ok(confirmedSession.await())
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Work session not found: ${request.workSessionId}" }
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error(e) { "Error confirming work session: ${request.workSessionId}" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Modify work session times
     * Available to managers and admins only
     */
    @PutMapping("/modify")
    suspend fun modifyWorkSession(
        @RequestBody request: WorkSessionModificationRequest,
        authentication: Authentication
    ): ResponseEntity<WorkSessionResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} modifying work session: ${request.workSessionId}" }

        try {
            val modifiedSession = async { 
                workSessionService.modifyWorkSession(
                    workSessionId = request.workSessionId,
                    newClockInTime = request.newClockInTime,
                    newClockOutTime = request.newClockOutTime,
                    modifiedBy = userInfo["userId"] as String
                )
            }
            ResponseEntity.ok(modifiedSession.await())
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Work session not found: ${request.workSessionId}" }
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error(e) { "Error modifying work session: ${request.workSessionId}" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Modify and confirm work session in one operation
     * Available to managers and admins only
     */
    @PutMapping("/modify-and-confirm")
    suspend fun modifyAndConfirmWorkSession(
        @RequestBody request: WorkSessionModifyAndConfirmRequest,
        authentication: Authentication
    ): ResponseEntity<WorkSessionResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} modifying and confirming work session: ${request.workSessionId}" }

        try {
            val modifiedSession = async { 
                workSessionService.modifyAndConfirmWorkSession(
                    workSessionId = request.workSessionId,
                    newClockInTime = request.newClockInTime,
                    newClockOutTime = request.newClockOutTime,
                    modifiedBy = userInfo["userId"] as String
                )
            }
            ResponseEntity.ok(modifiedSession.await())
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Work session not found: ${request.workSessionId}" }
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error(e) { "Error modifying and confirming work session: ${request.workSessionId}" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Update session note for a work session
     * Available to managers and admins only
     */
    @PutMapping("/session-note")
    suspend fun updateSessionNote(
        @RequestBody request: SessionNoteRequest,
        authentication: Authentication
    ): ResponseEntity<SessionNoteResponse> = coroutineScope {
        val userInfo = extractUserInfo(authentication)
        logger.info { "User ${userInfo["email"]} updating session note for work session: ${request.workSessionId}" }

        try {
            val updatedNote = async { 
                sessionNoteService.createOrUpdateNote(request)
            }
            ResponseEntity.ok(updatedNote.await())
        } catch (e: Exception) {
            logger.error(e) { "Error updating session note for work session: ${request.workSessionId}" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
} 