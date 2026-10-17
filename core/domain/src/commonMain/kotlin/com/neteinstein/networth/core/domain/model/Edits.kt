package com.neteinstein.networth.core.domain.model

/** Replaces the record with the same id, or appends it. */
fun <T : Syncable> List<T>.upsert(item: T): List<T> {
    val index = indexOfFirst { it.id == item.id }
    return if (index < 0) this + item else toMutableList().also { it[index] = item }
}

/** Removes the record with [id] and leaves a tombstone so other devices drop it too on merge. */
fun FamilyDatabase.withTombstone(id: String, now: Long): FamilyDatabase = copy(tombstones = tombstones + (id to now))
