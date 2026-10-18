package com.neteinstein.networth.feature.importer.domain

import com.neteinstein.networth.core.domain.model.Account
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.BalanceSnapshot
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.ImportRecord
import com.neteinstein.networth.core.domain.model.ScheduledPayment
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.model.upsert
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.text.Descriptions
import com.neteinstein.networth.core.domain.text.MovementKind
import com.neteinstein.networth.core.domain.text.Rules
import com.neteinstein.networth.feature.importer.domain.model.ImportRequest
import com.neteinstein.networth.feature.importer.domain.model.ImportSummary
import com.neteinstein.networth.feature.importer.domain.model.ParsedRow
import com.neteinstein.networth.feature.importer.domain.model.ParsedSection
import com.neteinstein.networth.feature.importer.domain.parse.Values
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.math.abs

/**
 * Applies a parsed statement to the family database. Pure: the same input always gives the same
 * database, so it runs inside [com.neteinstein.networth.core.domain.usecase.EditDatabaseUseCase]
 * (and again if a sync merge retries it).
 *
 * In order: resolve or create each section's account (and set its owners); drop movements already
 * imported (overlapping periods are normal: weekly + monthly extracts); file the rest with merchant
 * rules; pair internal transfers between the family's own accounts; record balances, holdings,
 * scheduled payments and one [ImportRecord] per account.
 */
class ImportPlanner(
    private val ids: IdGenerator,
) {
    data class Result(
        val database: FamilyDatabase,
        val summary: ImportSummary,
    )

    fun apply(
        database: FamilyDatabase,
        request: ImportRequest,
        now: Long,
        today: LocalDate,
        importedBy: String,
    ): Result {
        val statement = request.statement
        var accounts = database.accounts
        var snapshots = database.balanceSnapshots
        var records = database.imports
        val existing = database.transactions
        val known = existing.mapNotNull { tx -> tx.fingerprint?.let { it to tx } }.toMap()
        val added = ArrayList<Transaction>()
        val accountNames = ArrayList<String>()
        val targetAccounts = HashMap<Int, String>() // section index → account id
        val overlapped = LinkedHashSet<String>()
        val unreconciled = ArrayList<String>()
        val sectionAccounts = HashMap<String, String>() // account number → account id
        var rowsRead = 0
        var duplicates = 0
        var rules = database.merchantRules
        var firstAccountId: String? = null

        for (target in request.targets) {
            val section = statement.sections.getOrNull(target.sectionIndex) ?: continue
            val account =
                resolveAccount(accounts, target.accountId, section, statement.bank, target.name, target.type, target.ownerIds, now)
            accounts = accounts.upsert(account)
            accountNames += account.name
            targetAccounts[target.sectionIndex] = account.id
            if (firstAccountId == null) firstAccountId = account.id
            section.accountNumber?.let { sectionAccounts[it] = account.id }
            if (!reconciles(section)) unreconciled += account.name

            val importId = ids.newId()
            var sectionDuplicates = 0
            val occurrences = HashMap<String, Int>()
            for (row in section.rows) {
                rowsRead++
                val fingerprint = fingerprint(account.id, row, occurrences)
                val previous = known[fingerprint] ?: added.firstOrNull { it.fingerprint == fingerprint }
                if (previous != null) {
                    sectionDuplicates++
                    previous.importId?.let { if (it != importId) overlapped += it }
                    continue
                }
                val info = Descriptions.describe(row.description)
                val rule = if (row.amountMinor < 0 || info.kind == MovementKind.REFUND) Rules.find(rules, row.description, info) else null
                if (rule != null) {
                    rules = rules.upsert(rule.copy(hits = rule.hits + 1, updatedAt = now))
                }
                added +=
                    Transaction(
                        id = ids.newId(),
                        accountId = account.id,
                        date = row.date,
                        amountMinor = row.amountMinor,
                        description = row.description,
                        valueDate = row.valueDate,
                        categoryId = rule?.categoryId,
                        subcategoryId = rule?.subcategoryId,
                        importId = importId,
                        fingerprint = fingerprint,
                        updatedAt = now,
                    )
            }
            duplicates += sectionDuplicates

            val start = section.start ?: today
            val end = section.end ?: start
            records +=
                ImportRecord(
                    id = importId,
                    accountId = account.id,
                    fileName = statement.fileName,
                    format = statement.format.label,
                    periodStart = start,
                    periodEnd = end,
                    rowsRead = section.rows.size,
                    duplicatesSkipped = sectionDuplicates,
                    importedAt = now,
                    importedBy = importedBy,
                    updatedAt = now,
                )

            // Balances: before the first day and at the last movement, so history and today both add up.
            section.openingBalanceMinor?.let { opening ->
                section.start?.let { snapshots = snapshot(snapshots, account.id, it.minus(DatePeriod(days = 1)), opening, now) }
            }
            section.closingBalanceMinor?.let { closing ->
                snapshots = snapshot(snapshots, account.id, section.rows.maxOfOrNull { it.date } ?: end, closing, now)
            }
        }

        // Mortgages, loans and funds: one balance per statement, matched by contract number.
        val primaryOwners = request.targets.firstOrNull()?.ownerIds.orEmpty()
        for (holding in statement.holdings) {
            val number = holding.accountNumber
            val current = accounts.firstOrNull { number != null && Values.digits(it.accountNumber.orEmpty()) == number }
            val account =
                current?.let {
                    if (it.ownerIds.isEmpty() &&
                        primaryOwners.isNotEmpty()
                    ) {
                        it.copy(ownerIds = primaryOwners, updatedAt = now)
                    } else {
                        it
                    }
                }
                    ?: Account(
                        id = ids.newId(),
                        name = Descriptions.clean(holding.label) ?: holding.label,
                        type = holding.type,
                        ownerIds = primaryOwners,
                        bank = statement.bank,
                        accountNumber = number,
                        updatedAt = now,
                    )
            accounts = accounts.upsert(account)
            snapshots = snapshot(snapshots, account.id, holding.date, holding.balanceMinor, now)
        }

        // Scheduled payments (e.g. next mortgage instalment), replacing the same one from an earlier import.
        var scheduled = database.scheduledPayments
        for (payment in statement.scheduled) {
            val accountId = payment.accountNumber?.let { sectionAccounts[it] } ?: firstAccountId ?: continue
            val same = scheduled.firstOrNull {
                it.accountId == accountId && it.date == payment.date && it.description == payment.description
            }
            scheduled =
                scheduled.upsert(
                    ScheduledPayment(same?.id ?: ids.newId(), accountId, payment.description, payment.date, payment.amountMinor, now),
                )
        }

        val ownAccountIds = accounts.map { it.id }.toSet()
        val withTransfers = markInternalTransfers(existing, added, ownAccountIds, database, now)
        val newOnes = withTransfers.filter { tx -> added.any { it.id == tx.id } }
        val internal = newOnes.count { it.isInternalTransfer }
        val needReview = newOnes.count { it.needsReview }
        val updated =
            database.copy(
                accounts = accounts,
                balanceSnapshots = snapshots,
                scheduledPayments = scheduled,
                transactions = withTransfers,
                imports = records,
                merchantRules = rules,
            )
        val sections = request.targets.mapNotNull { statement.sections.getOrNull(it.sectionIndex) }
        return Result(
            updated,
            ImportSummary(
                fileName = statement.fileName,
                accountNames = accountNames,
                periodStart = sections.mapNotNull { it.start }.minOrNull(),
                periodEnd = sections.mapNotNull { it.end }.maxOrNull(),
                rowsRead = rowsRead,
                duplicatesSkipped = duplicates,
                imported = added.size,
                autoCategorised = newOnes.count { it.categoryId != null && !it.isInternalTransfer },
                internalTransfers = internal,
                needReview = needReview,
                overlappedImportIds = overlapped.toList(),
                unreconciledAccounts = unreconciled,
                sectionAccountIds = targetAccounts,
            ),
        )
    }

    private fun resolveAccount(
        accounts: List<Account>,
        accountId: String?,
        section: ParsedSection,
        bank: String?,
        name: String,
        type: AccountType,
        ownerIds: List<String>,
        now: Long,
    ): Account {
        val existing = accountId?.let { id -> accounts.firstOrNull { it.id == id } }
        if (existing != null) {
            return existing.copy(
                ownerIds = ownerIds.ifEmpty { existing.ownerIds },
                bank = existing.bank ?: bank,
                accountNumber = existing.accountNumber ?: section.accountNumber,
                iban = existing.iban ?: section.iban,
                updatedAt = now,
            )
        }
        return Account(
            id = ids.newId(),
            name = name.trim().ifEmpty { section.label },
            type = type,
            ownerIds = ownerIds,
            bank = bank,
            accountNumber = section.accountNumber,
            iban = section.iban,
            updatedAt = now,
        )
    }

    private fun snapshot(
        snapshots: List<BalanceSnapshot>,
        accountId: String,
        date: LocalDate,
        amount: Long,
        now: Long,
    ): List<BalanceSnapshot> {
        val same = snapshots.firstOrNull { it.accountId == accountId && it.date == date }
        return snapshots.upsert(BalanceSnapshot(same?.id ?: ids.newId(), accountId, date, amount, now))
    }

    companion object {
        /** Days apart two legs of a transfer between own accounts may be booked. */
        const val TRANSFER_WINDOW_DAYS = 3

        /**
         * Identifies a movement across files: account, date, amount, running balance and the
         * description's letters and digits. Without a balance (CSV/OFX exports), identical movements
         * on the same day are told apart by their order. OFX's FITID is used when present.
         */
        fun fingerprint(
            accountId: String,
            row: ParsedRow,
            occurrences: MutableMap<String, Int>,
        ): String {
            row.externalId?.let { return "$accountId|id|$it" }
            val description = row.description.uppercase().filter { it.isLetterOrDigit() }
            val base = "$accountId|${row.date}|${row.amountMinor}|${row.balanceMinor ?: ""}|$description"
            if (row.balanceMinor != null) return base
            val n = occurrences[base] ?: 0
            occurrences[base] = n + 1
            return if (n == 0) base else "$base#$n"
        }

        fun reconciles(section: ParsedSection): Boolean {
            val opening = section.openingBalanceMinor ?: return true
            val closing = section.closingBalanceMinor ?: return true
            return opening + section.rows.sumOf { it.amountMinor } == closing
        }

        /**
         * Marks movements between the family's own accounts: an outflow and an inflow of the same
         * amount within [TRANSFER_WINDOW_DAYS], at least one of them new; and transfers to or from a
         * household member named in full. Internal transfers are never spending or income.
         */
        fun markInternalTransfers(
            existing: List<Transaction>,
            added: List<Transaction>,
            ownAccountIds: Set<String>,
            database: FamilyDatabase,
            now: Long,
        ): List<Transaction> {
            val all = (existing + added).toMutableList()
            val index = all.withIndex().associate { it.value.id to it.index }
            val paired = HashSet<String>()
            val members = database.members.map { it.name.uppercase().split(' ').filter { w -> w.length > 1 } }.filter { it.size >= 2 }
            for (tx in added) {
                if (tx.id in paired || all[index.getValue(tx.id)].isInternalTransfer) continue
                val match =
                    all.firstOrNull { other ->
                        other.id != tx.id &&
                            other.id !in paired &&
                            other.accountId != tx.accountId &&
                            other.accountId in ownAccountIds &&
                            other.amountMinor == -tx.amountMinor &&
                            !other.isInternalTransfer &&
                            abs(other.date.toEpochDays() - tx.date.toEpochDays()) <= TRANSFER_WINDOW_DAYS
                    }
                if (match != null) {
                    paired += tx.id
                    paired += match.id
                    all[index.getValue(tx.id)] =
                        all[index.getValue(tx.id)].copy(isInternalTransfer = true, categoryId = null, subcategoryId = null, updatedAt = now)
                    all[index.getValue(match.id)] =
                        all[
                            index.getValue(
                                match.id,
                            ),
                        ].copy(isInternalTransfer = true, categoryId = null, subcategoryId = null, updatedAt = now)
                    continue
                }
                val info = Descriptions.describe(tx.description)
                val counterparty = info.counterparty?.uppercase()
                val toMember =
                    counterparty != null &&
                        (
                            info.kind == MovementKind.TRANSFER_IN ||
                                info.kind == MovementKind.TRANSFER_OUT ||
                                info.kind == MovementKind.MB_WAY_PERSON
                        ) &&
                        members.any { words -> words.all { counterparty.split(' ').contains(it) } }
                if (toMember) all[index.getValue(tx.id)] = all[index.getValue(tx.id)].copy(isInternalTransfer = true, updatedAt = now)
            }
            return all
        }
    }
}
