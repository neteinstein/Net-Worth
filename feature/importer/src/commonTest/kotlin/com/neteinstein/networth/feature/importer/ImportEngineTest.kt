package com.neteinstein.networth.feature.importer

import com.neteinstein.networth.core.domain.calc.balanceOf
import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.core.domain.model.CalendarMonth
import com.neteinstein.networth.core.domain.model.FamilyDatabase
import com.neteinstein.networth.core.domain.model.ImportRecord
import com.neteinstein.networth.core.domain.model.Member
import com.neteinstein.networth.core.domain.model.MemberRole
import com.neteinstein.networth.core.domain.model.MerchantRule
import com.neteinstein.networth.core.domain.model.RuleMatch
import com.neteinstein.networth.core.domain.model.Transaction
import com.neteinstein.networth.core.domain.text.Descriptions
import com.neteinstein.networth.core.domain.text.Rules
import com.neteinstein.networth.core.testing.SequentialIds
import com.neteinstein.networth.core.testing.designFamily
import com.neteinstein.networth.feature.importer.domain.AccountMatcher
import com.neteinstein.networth.feature.importer.domain.Coverage
import com.neteinstein.networth.feature.importer.domain.ImportPlanner
import com.neteinstein.networth.feature.importer.domain.KnownMerchants
import com.neteinstein.networth.feature.importer.domain.ReviewQueue
import com.neteinstein.networth.feature.importer.domain.model.ImportRequest
import com.neteinstein.networth.feature.importer.domain.model.MonthCoverage
import com.neteinstein.networth.feature.importer.domain.model.ParsedHolding
import com.neteinstein.networth.feature.importer.domain.model.ParsedRow
import com.neteinstein.networth.feature.importer.domain.model.ParsedScheduledPayment
import com.neteinstein.networth.feature.importer.domain.model.ParsedSection
import com.neteinstein.networth.feature.importer.domain.model.ParsedStatement
import com.neteinstein.networth.feature.importer.domain.model.ReviewChoice
import com.neteinstein.networth.feature.importer.domain.model.SectionTarget
import com.neteinstein.networth.feature.importer.domain.model.StatementFormat
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportEngineTest {
    private val family = designFamily()
    private val pedro = family.members.first { it.name == "Pedro" }
    private val maria = family.members.first { it.name == "Maria" }
    private val pedroAccount = family.accounts.first { it.name == "Pedro" }
    private val joint = family.accounts.first { it.type == AccountType.SAVINGS && it.ownerIds.size == 2 }
    private val groceries = family.categories.first { it.name == "Groceries" }
    private val other = family.categories.first { it.name == "Other" }

    private fun d(day: Int, month: Int = 9) = LocalDate(2026, month, day)

    private val september =
        ParsedSection(
            label = "360° - DO",
            accountNumber = "000123456789",
            iban = "PT50000700000012345678901",
            periodStart = d(1),
            periodEnd = d(30),
            openingBalanceMinor = 100_000,
            closingBalanceMinor = 92_250,
            rows =
                listOf(
                    ParsedRow(d(2), "Compra Mb Cartão ****111* Pingo Doce Lisboa", -2_550, balanceMinor = 97_450),
                    ParsedRow(d(3), "Trf Imediata C/Cartao (Mbway)-App Terceiros P/ Ana Lima", -1_000, balanceMinor = 96_450),
                    ParsedRow(d(5), "Trf Cred Sepa+ Online 123 P/ Poupanca Familia", -5_000, balanceMinor = 91_450),
                    ParsedRow(d(9), "Trf Cred Sepa+ De Empresa", 800, balanceMinor = 92_250),
                ),
        )

    private fun statement(vararg sections: ParsedSection) =
        ParsedStatement(
            fileName = "extrato.pdf",
            format = StatementFormat.PDF,
            bank = "novobanco",
            sections = sections.toList(),
            holdings = listOf(ParsedHolding("Hipoteca Casa", AccountType.MORTGAGE, 8_000_000, d(30), "0999888777")),
            scheduled = listOf(ParsedScheduledPayment("Hipoteca Casa", d(2, 10), -50_000, "000123456789")),
        )

    private fun apply(
        db: FamilyDatabase,
        request: ImportRequest,
        now: Long = 5_000,
    ) = ImportPlanner(SequentialIds("imp$now")).apply(db, request, now, d(1, 10), "Pedro")

    @Test
    fun createsAccountWithSeveralOwnersAndRecordsEverything() {
        val target = SectionTarget(0, null, "Conta conjunta", AccountType.CURRENT, listOf(pedro.id, maria.id))
        val result = apply(family, ImportRequest(statement(september), listOf(target)))
        val db = result.database
        val account = db.accounts.first { it.name == "Conta conjunta" }
        assertEquals(listOf(pedro.id, maria.id), account.ownerIds)
        assertEquals("novobanco", account.bank)
        assertEquals("000123456789", account.accountNumber)
        assertEquals("PT50000700000012345678901", account.iban)
        assertEquals(4, db.transactions.count { it.accountId == account.id })
        assertEquals(92_250, db.balanceOf(account))
        assertEquals(100_000, db.balanceOf(account, d(31, 8)))

        val record = db.imports.single()
        assertEquals(account.id, record.accountId)
        assertEquals(d(1), record.periodStart)
        assertEquals(d(30), record.periodEnd)
        assertEquals(4, record.rowsRead)
        assertEquals("PDF", record.format)
        assertEquals("Pedro", record.importedBy)

        val mortgage = db.accounts.first { it.type == AccountType.MORTGAGE }
        assertEquals("Hipoteca Casa", mortgage.name)
        assertEquals(listOf(pedro.id, maria.id), mortgage.ownerIds)
        assertEquals(8_000_000, db.balanceOf(mortgage))
        assertEquals(account.id, db.scheduledPayments.single().accountId)

        val summary = result.summary
        assertEquals(listOf("Conta conjunta"), summary.accountNames)
        assertEquals(4, summary.rowsRead)
        assertEquals(0, summary.duplicatesSkipped)
        assertEquals(4, summary.imported)
        assertEquals(3, summary.needReview)
        assertTrue(summary.unreconciledAccounts.isEmpty())
    }

    @Test
    fun overlappingImportsSkipDuplicatesAndUpdateOwners() {
        val first =
            apply(
                family,
                ImportRequest(statement(september), listOf(SectionTarget(0, pedroAccount.id, "", AccountType.CURRENT, emptyList()))),
                now = 5_000,
            )
        val weekly = september.copy(periodStart = d(1), periodEnd = d(7), rows = september.rows.take(3))
        val second =
            apply(
                first.database,
                ImportRequest(
                    statement(weekly),
                    listOf(SectionTarget(0, pedroAccount.id, "", AccountType.CURRENT, listOf(pedro.id, maria.id))),
                ),
                now = 6_000,
            )
        assertEquals(3, second.summary.duplicatesSkipped)
        assertEquals(0, second.summary.imported)
        assertEquals(listOf(first.database.imports.single().id), second.summary.overlappedImportIds)
        assertEquals(4, second.database.transactions.count { it.accountId == pedroAccount.id })
        assertEquals(listOf(pedro.id, maria.id), second.database.account(pedroAccount.id)!!.ownerIds)
        // Statement details fill in what the account didn't know yet.
        assertEquals("000123456789", second.database.account(pedroAccount.id)!!.accountNumber)
        // Scheduled payment and holding are updated, not duplicated.
        assertEquals(1, second.database.scheduledPayments.size)
        assertEquals(1, second.database.accounts.count { it.type == AccountType.MORTGAGE })
    }

    @Test
    fun appliesRulesPairsTransfersAndFlagsUnreconciledSections() {
        val withRule = family.copy(merchantRules = listOf(MerchantRule("r1", "PINGO", RuleMatch.PREFIX, groceries.id, hits = 2)))
        val jointSection =
            ParsedSection(
                label = "Poupança",
                periodStart = d(1),
                periodEnd = d(30),
                openingBalanceMinor = 0,
                closingBalanceMinor = 9_999,
                rows = listOf(ParsedRow(d(6), "Trf Cred Sepa+ De Pedro", 5_000)),
            )
        val result =
            apply(
                withRule,
                ImportRequest(
                    statement(september, jointSection),
                    listOf(
                        SectionTarget(0, pedroAccount.id, "", AccountType.CURRENT, listOf(pedro.id)),
                        SectionTarget(1, joint.id, "", AccountType.SAVINGS, listOf(pedro.id, maria.id)),
                    ),
                ),
            )
        val txs = result.database.transactions
        assertEquals(groceries.id, txs.first { it.description.contains("Pingo") }.categoryId)
        assertEquals(3, result.database.merchantRules.single().hits)
        val transferOut = txs.first { it.amountMinor == -5_000L }
        val transferIn = txs.first { it.amountMinor == 5_000L }
        assertTrue(transferOut.isInternalTransfer && transferIn.isInternalTransfer)
        assertEquals(2, result.summary.internalTransfers)
        assertEquals(1, result.summary.autoCategorised)
        assertEquals(listOf(joint.name), result.summary.unreconciledAccounts)
        assertFalse(ImportPlanner.reconciles(jointSection))
    }

    @Test
    fun transfersToAHouseholdMemberNamedInFullAreInternal() {
        val db = family.copy(members = family.members + Member("ana", "Ana Lima", MemberRole.ADULT))
        val result =
            apply(db, ImportRequest(statement(september), listOf(SectionTarget(0, pedroAccount.id, "", AccountType.CURRENT, emptyList()))))
        assertTrue(result.database.transactions.first { it.description.contains("Ana Lima") }.isInternalTransfer)
    }

    @Test
    fun fingerprintsTellIdenticalRowsApartWithoutBalances() {
        val occurrences = HashMap<String, Int>()
        val row = ParsedRow(d(2), "Café", -100)
        val a = ImportPlanner.fingerprint("acc", row, occurrences)
        val b = ImportPlanner.fingerprint("acc", row, occurrences)
        assertTrue(a != b)
        assertEquals("acc|id|X1", ImportPlanner.fingerprint("acc", row.copy(externalId = "X1"), occurrences))
        assertTrue(ImportPlanner.reconciles(september.copy(openingBalanceMinor = null)))
    }

    @Test
    fun coverageMarksFullPartialAndMissingMonths() {
        val db =
            family.copy(
                imports =
                    listOf(
                        ImportRecord("i1", pedroAccount.id, "a.pdf", "PDF", d(1, 8), d(1, 9), 10, 0, 0, "Pedro"),
                        ImportRecord("i2", pedroAccount.id, "b.csv", "CSV", d(22), d(28), 5, 0, 0, "Pedro"),
                    ),
                transactions = listOf(Transaction("t", joint.id, d(15, 7), -100, "x")),
            )
        val coverage = Coverage.of(db, CalendarMonth(2026, 9), count = 3)
        val pedroRow = coverage.first { it.account.id == pedroAccount.id }
        assertEquals(
            listOf(MonthCoverage.MISSING, MonthCoverage.FULL, MonthCoverage.PARTIAL),
            pedroRow.months.map { it.second },
        )
        assertEquals(2, pedroRow.missingCount)
        assertEquals(MonthCoverage.PARTIAL, coverage.first { it.account.id == joint.id }.months.first().second)
        assertTrue(coverage.none { it.account.type == AccountType.MORTGAGE })
    }

    @Test
    fun matchesAccountsByIbanNumberOrIbanDigits() {
        val db =
            family.copy(
                accounts =
                    family.accounts.map {
                        when (it.id) {
                            pedroAccount.id -> it.copy(iban = "PT50000700000012345678901")
                            joint.id -> it.copy(accountNumber = "0001 2345 0000")
                            else -> it
                        }
                    },
            )
        assertEquals(pedroAccount.id, AccountMatcher.match(db, ParsedSection("x", iban = "pt50000700000012345678901"))?.id)
        assertEquals(joint.id, AccountMatcher.match(db, ParsedSection("x", accountNumber = "000123450000"))?.id)
        assertEquals(pedroAccount.id, AccountMatcher.match(db, ParsedSection("x", accountNumber = "0012345678"))?.id)
        assertNull(AccountMatcher.match(db, ParsedSection("x", accountNumber = "123")))
    }

    @Test
    fun rulesMatchPrefixContainsAndPhone() {
        val rules =
            listOf(
                MerchantRule("1", "DECATHLON", RuleMatch.PREFIX, "sport"),
                MerchantRule("2", "ASSOC PAIS", RuleMatch.CONTAINS, "school"),
                MerchantRule("3", "9** *** 412", RuleMatch.PHONE, "gifts"),
                MerchantRule("4", "", RuleMatch.CONTAINS, "never"),
            )
        assertEquals("sport", Rules.find(rules, "COMPRA 4417 DECATHLON MATOSINHOS")?.categoryId)
        assertEquals("school", Rules.find(rules, "TRF ASSOC PAIS ESC BASICA")?.categoryId)
        assertEquals("gifts", Rules.find(rules, "MB WAY P/ 9** *** 412")?.categoryId)
        assertNull(Rules.find(rules, "LIDL"))
        assertEquals("9** *** 412" to RuleMatch.PHONE, Rules.learn(Descriptions.describe("MB WAY P/ 9** *** 412")))
        assertEquals(
            "ANA LIMA" to RuleMatch.PHONE,
            Rules.learn(Descriptions.describe("Trf Imediata C/Cartao (Mbway)-App Terceiros P/ Ana Lima")),
        )
        assertEquals("PINGO" to RuleMatch.PREFIX, Rules.learn(Descriptions.describe("Compra Mb Cartão ****1* Pingo Doce Lisboa")))
        assertNull(Rules.learn(Descriptions.describe("Levantamento Mb Cartão **** 1*")))
    }

    @Test
    fun reviewQueueSuggestsFromHistoryAndLearnsRules() {
        val past =
            listOf(
                Transaction("p1", pedroAccount.id, d(1, 8), -1_000, "COMPRA DECATHLON PORTO", categoryId = groceries.id),
                Transaction("p2", pedroAccount.id, d(2, 8), -1_000, "COMPRA DECATHLON PORTO", categoryId = groceries.id),
                Transaction("p3", pedroAccount.id, d(3, 8), -1_000, "COMPRA DECATHLON MAIA", categoryId = other.id),
            )
        val waiting =
            listOf(
                Transaction("w1", pedroAccount.id, d(12), -8_640, "COMPRA 4417 DECATHLON MATOSINHOS"),
                Transaction("w2", pedroAccount.id, d(14), -3_500, "MB WAY P/ 9** *** 412"),
                Transaction("w3", pedroAccount.id, d(10), -990, "Manutencao Conta Pacote"),
                Transaction("w4", pedroAccount.id, d(9), -2_000, "COMPRA DECATHLON LISBOA"),
                Transaction("in", pedroAccount.id, d(9), 2_000, "Salary"),
            )
        val db = family.copy(transactions = past + waiting)
        val queue = ReviewQueue.build(db)
        assertEquals(listOf("w2", "w1", "w3", "w4"), queue.map { it.transaction.id })
        val decathlon = queue.first { it.transaction.id == "w1" }
        assertEquals(groceries.id, decathlon.suggestedCategoryId)
        assertEquals(2 to 3, decathlon.history)
        assertEquals(groceries.id, decathlon.choiceIds.first())
        assertEquals("DECATHLON", decathlon.ruleKey)
        val mbWay = queue.first { it.transaction.id == "w2" }
        assertTrue(mbWay.needsNote)
        assertNull(mbWay.suggestedCategoryId)
        assertEquals(other.id, queue.first { it.transaction.id == "w3" }.suggestedCategoryId)
        assertTrue(ReviewQueue.knownMerchants(db) >= 1)

        val (updated, filed) =
            ReviewQueue.apply(
                db,
                listOf(
                    ReviewChoice("w1", other.id, remember = true),
                    ReviewChoice("w2", groceries.id, note = " Birthday gift ", remember = true),
                ),
                SequentialIds("rule"),
                now = 9_000,
            )
        assertEquals(3, filed) // w1, w2 and w4 through the learned DECATHLON rule
        val txs = updated.transactions.associateBy { it.id }
        assertEquals(other.id, txs.getValue("w4").categoryId)
        assertEquals("Birthday gift", txs.getValue("w2").note)
        assertEquals(setOf("DECATHLON", "9** *** 412"), updated.merchantRules.map { it.pattern }.toSet())
        assertEquals(RuleMatch.PREFIX, updated.merchantRules.first { it.pattern == "DECATHLON" }.match)
    }

    @Test
    fun knownPortugueseBrandsSuggestACategory() {
        fun sub(description: String) = KnownMerchants.subcategoryFor(Descriptions.describe(description))
        assertEquals("Supermarket", sub("Compra Mb Cartão ****1* Pingo Doce Lisboa"))
        assertEquals("Supermarket", sub("COMPRA 4417 CONTINENTE BOM DIA BRAGA"))
        assertEquals("Supermarket", sub("Compra Mercadona Gaia"))
        assertEquals("Supermarket", sub("Compra Intermarché Évora"))
        assertEquals("Fuel", sub("COMPRA GALP MATOSINHOS"))
        assertEquals("Takeaway", sub("COMPRA UBER EATS"))
        assertEquals("Public transport", sub("COMPRA UBER"))
        assertEquals("Adults", sub("COMPRA H&M COLOMBO"))
        assertEquals("Outings", sub("COMPRA NOS CINEMAS NORTESHOPPING"))
        assertEquals("Misc", sub("COMPRA CLINICA VETERINARIA DO PORTO"))
        assertEquals("Doctor & dentist", sub("COMPRA CLINICA DENTARIA"))
        assertEquals("Electricity", sub("Cobrança SDD EDP Comercial ADC 123"))
        assertNull(sub("COMPRA LIDLAND"))
        assertNull(sub("Pagamento Baixo Valor"))
        assertTrue(KnownMerchants.size > 500)

        val waiting = Transaction("w", pedroAccount.id, d(12), -4_250, "COMPRA 4417 PINGO DOCE PORTO")
        val item = ReviewQueue.build(family.copy(transactions = listOf(waiting))).single()
        val groceriesCategory = family.categories.first { c -> c.subcategories.any { it.name == "Supermarket" } }
        assertEquals(groceriesCategory.id, item.suggestedCategoryId)
        assertNull(item.history)

        // A household that renamed its subcategories into Portuguese still gets the suggestion.
        val renamed =
            family.categories.map { c ->
                c.copy(
                    subcategories = c.subcategories.map { s ->
                        if (s.name ==
                            "Supermarket"
                        ) {
                            s.copy(name = "Supermercado")
                        } else {
                            s
                        }
                    },
                )
            }
        val inPortuguese = ReviewQueue.build(family.copy(categories = renamed, transactions = listOf(waiting))).single()
        assertEquals(groceriesCategory.id, inPortuguese.suggestedCategoryId)
        assertEquals(listOf("Misc", "Diversos"), KnownMerchants.namesOf("Misc"))
        assertEquals(listOf("Unknown"), KnownMerchants.namesOf("Unknown"))
    }
}
