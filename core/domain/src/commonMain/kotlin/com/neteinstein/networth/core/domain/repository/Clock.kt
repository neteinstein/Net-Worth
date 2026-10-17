package com.neteinstein.networth.core.domain.repository

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Time and ids behind an interface so domain logic stays deterministic in tests. */
interface AppClock {
    fun nowMillis(): Long

    fun today(): LocalDate
}

object SystemAppClock : AppClock {
    override fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    override fun today(): LocalDate =
        Instant.fromEpochMilliseconds(nowMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date
}

fun interface IdGenerator {
    fun newId(): String
}

object UuidGenerator : IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    override fun newId(): String = Uuid.random().toString()
}
