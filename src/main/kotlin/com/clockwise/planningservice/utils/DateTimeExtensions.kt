package com.clockwise.planningservice.utils

import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
 * Converts a ZonedDateTime to OffsetDateTime
 */
fun ZonedDateTime.toOffsetDateTime(): OffsetDateTime {
    return OffsetDateTime.of(this.toLocalDateTime(), this.offset)
} 