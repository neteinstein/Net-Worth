package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.Syncable

/**
 * Combines two copies of the same database (e.g. this device's and the one in Drive) record by
 * record: the newer `updatedAt` wins, records only one side has are kept, and a tombstone removes
 * a record unless it was edited after the deletion. The result is the same whichever side is passed
 * first, and lists come back sorted by id so equal content compares equal.
 */
object DatabaseMerger {
    fun merge(a: FamilyDatabase, b: FamilyDatabase): FamilyDatabase {
        val tombstones = (a.tombstones.keys + b.tombstones.keys).associateWith { maxOf(a.tombstones[it] ?: 0L, b.tombstones[it] ?: 0L) }

        fun <T : Syncable> merge(x: List<T>, y: List<T>): List<T> = (x + y)
            .groupBy { it.id }
            .values
            .map { versions -> versions.reduce(::newer) }
            .filter { record -> (tombstones[record.id] ?: Long.MIN_VALUE) < record.updatedAt }
            .sortedBy { it.id }

        val latest = newerRoot(a, b)
        return FamilyDatabase(
            id = latest.id,
            createdAt = minOf(a.createdAt, b.createdAt),
            updatedAt = latest.updatedAt,
            updatedBy = latest.updatedBy,
            settings = listOf(a.settings, b.settings).reduce { x, y ->
                if (y.updatedAt > x.updatedAt || (y.updatedAt == x.updatedAt && y.toString() > x.toString())) y else x
            },
            members = merge(a.members, b.members),
            accounts = merge(a.accounts, b.accounts),
            balanceSnapshots = merge(a.balanceSnapshots, b.balanceSnapshots),
            scheduledPayments = merge(a.scheduledPayments, b.scheduledPayments),
            categories = merge(a.categories, b.categories),
            transactions = merge(a.transactions, b.transactions),
            imports = merge(a.imports, b.imports),
            merchantRules = merge(a.merchantRules, b.merchantRules),
            savedFilters = merge(a.savedFilters, b.savedFilters),
            tombstones = tombstones,
        )
    }

    /** Sorts every collection by id — the canonical form [merge] returns. */
    fun normalize(database: FamilyDatabase): FamilyDatabase = merge(database, database)

    private fun <T : Syncable> newer(x: T, y: T): T = when {
        y.updatedAt > x.updatedAt -> y
        y.updatedAt < x.updatedAt -> x
        // Same timestamp from two devices: pick deterministically so both sides agree.
        y.toString() > x.toString() -> y
        else -> x
    }

    private fun newerRoot(a: FamilyDatabase, b: FamilyDatabase): FamilyDatabase = when {
        b.updatedAt > a.updatedAt -> b
        b.updatedAt < a.updatedAt -> a
        b.updatedBy > a.updatedBy -> b
        else -> a
    }
}
