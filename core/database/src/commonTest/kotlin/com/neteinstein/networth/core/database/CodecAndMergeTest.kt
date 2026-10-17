package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.BalanceSnapshot
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.ImportRecord
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.RuleMatch
import com.neteinstein.networth.core.domain.model.SavedFilter
import com.neteinstein.networth.core.domain.model.ScheduledPayment
import com.neteinstein.networth.core.domain.model.SeasonalMark
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.model.TransactionFilter
import com.neteinstein.networth.core.domain.model.TransactionTypeFilter
import com.neteinstein.networth.core.domain.repository.DatabaseError
import com.neteinstein.networth.core.domain.repository.DatabaseException
import com.neteinstein.networth.core.domain.seed.HouseholdSetup
import com.neteinstein.networth.core.domain.seed.toDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CodecAndMergeTest {
    private fun sample(): FamilyDatabase {
        val base = HouseholdSetup("Vicente", listOf("Pedro", "Maria"), listOf("Inês")).toDatabase(SequentialIds(), 100, "pedro@x")
        val account = base.accounts.first()
        val date = LocalDate(2026, 9, 2)
        return base.copy(
            settings = base.settings.copy(alerts = base.settings.alerts.copy(seasonalMarks = listOf(SeasonalMark("c", 9, 2026)))),
            balanceSnapshots = listOf(BalanceSnapshot("b1", account.id, date, 4_394_37, 5)),
            scheduledPayments = listOf(ScheduledPayment("s1", account.id, "Bhp - Aquisicao C/ Hipo", date, -733_59, 5)),
            transactions = listOf(
                Transaction(
                    "t1",
                    account.id,
                    date,
                    -733_59,
                    "Pagamento Prestação Ch N° 55",
                    valueDate = date,
                    note = "n",
                    categoryId = base.categories.first().id,
                    subcategoryId = base.categories.first().subcategories.first().id,
                    memberIds = listOf("m"),
                    tags = listOf("house"),
                    isInternalTransfer = false,
                    importId = "i1",
                    fingerprint = "fp",
                    updatedAt = 5,
                ),
            ),
            imports = listOf(ImportRecord("i1", account.id, "extrato.pdf", "PDF", date, date, 60, 0, 7, "pedro@x", 7)),
            merchantRules = listOf(MerchantRule("r1", "CONTINENTE", RuleMatch.PREFIX, "c", null, 3, 9)),
            savedFilters = listOf(
                SavedFilter("sf1", "Kids’ costs", TransactionFilter(type = TransactionTypeFilter.BOTH, minMinor = 1, text = "x"), 9),
            ),
            tombstones = mapOf("gone" to 50L),
        )
    }

    @Test
    fun roundTripsEveryField() {
        val database = sample()

        val text = FamilyDatabaseCodec.encode(database)

        assertTrue(text.contains("\"format\": \"net-worth-family-database\""))
        assertTrue(text.contains("\"schemaVersion\": 1"))
        assertEquals(database, FamilyDatabaseCodec.decode(text))
    }

    @Test
    fun rejectsOtherFilesAndNewerSchemas() {
        fun errorOf(text: String) = assertFailsWith<DatabaseException> { FamilyDatabaseCodec.decode(text) }.error

        assertEquals(DatabaseError.NOT_A_DATABASE, errorOf("not json"))
        assertEquals(DatabaseError.NOT_A_DATABASE, errorOf("""{"hello":"world"}"""))
        assertEquals(DatabaseError.NOT_A_DATABASE, errorOf("""{"format":"net-worth-family-database"}"""))
        assertEquals(DatabaseError.NOT_A_DATABASE, errorOf("""{"format":"net-worth-family-database","schemaVersion":1}"""))
        assertEquals(DatabaseError.NEWER_SCHEMA, errorOf("""{"format":"net-worth-family-database","schemaVersion":99}"""))
    }

    @Test
    fun toleratesUnknownFieldsAndEnumValues() {
        val text = FamilyDatabaseCodec.encode(sample())
            .replace("\"type\": \"CURRENT\"", "\"type\": \"CRYPTO_WALLET\"")
            .replaceFirst("{", "{\n  \"addedInV7\": true,")

        val decoded = FamilyDatabaseCodec.decode(text)

        assertEquals(AccountType.CURRENT, decoded.accounts.first().type)
    }

    @Test
    fun mergeKeepsNewestRecordsFromBothSides() {
        val base = sample()
        val category = base.categories.first()
        val local = base.copy(
            updatedAt = 300,
            categories = base.categories.map { if (it.id == category.id) it.copy(name = "Home", updatedAt = 300) else it },
        )
        val remote = base.copy(
            updatedAt = 200,
            updatedBy = "maria@x",
            categories = base.categories.map { if (it.id == category.id) it.copy(name = "House", updatedAt = 200) else it },
            transactions = base.transactions + base.transactions.first().copy(id = "t2", updatedAt = 210),
        )

        val merged = DatabaseMerger.merge(local, remote)

        assertEquals("Home", merged.category(category.id)?.name)
        assertEquals(listOf("t1", "t2"), merged.transactions.map { it.id })
        assertEquals(300, merged.updatedAt)
        assertEquals("pedro@x", merged.updatedBy)
        assertEquals(merged, DatabaseMerger.merge(remote, local))
    }

    @Test
    fun tombstonesRemoveUnlessEditedLater() {
        val base = sample()
        val deletedEarly = base.copy(transactions = emptyList(), tombstones = base.tombstones + ("t1" to 4L))
        val deletedLate = base.copy(transactions = emptyList(), tombstones = base.tombstones + ("t1" to 6L))

        assertEquals(listOf("t1"), DatabaseMerger.merge(base, deletedEarly).transactions.map { it.id })
        assertTrue(DatabaseMerger.merge(base, deletedLate).transactions.isEmpty())
        assertEquals(6L, DatabaseMerger.merge(base, deletedLate).tombstones["t1"])
    }

    @Test
    fun settingsMergeAsOneRecord() {
        val base = sample()
        val renamed = base.copy(settings = base.settings.copy(householdName = "Vicentes", updatedAt = 500))

        assertEquals("Vicentes", DatabaseMerger.merge(base, renamed).settings.householdName)
        assertEquals("Vicentes", DatabaseMerger.merge(renamed, base).settings.householdName)
    }

    @Test
    fun normalizeSortsById() {
        val base = sample()
        val shuffled = base.copy(categories = base.categories.reversed())

        assertEquals(DatabaseMerger.normalize(base), DatabaseMerger.normalize(shuffled))
    }
}
