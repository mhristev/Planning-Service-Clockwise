package com.clockwise.planningservice.domains.workload

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("session_notes")
data class SessionNote(
    @Id
    val id: String? = null,
    val workSessionId: String,
    val content: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
) 