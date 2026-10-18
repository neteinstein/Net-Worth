package com.neteinstein.networth.feature.importer

import com.neteinstein.networth.core.domain.model.AccountType
import com.neteinstein.networth.feature.importer.domain.ImportPlanner
import com.neteinstein.networth.feature.importer.domain.parse.ParseException
import com.neteinstein.networth.feature.importer.domain.parse.bank.BancoBestProfile
import com.neteinstein.networth.feature.importer.domain.parse.bank.GenericPdfProfile
import com.neteinstein.networth.feature.importer.domain.parse.bank.NovobancoProfile
import com.neteinstein.networth.feature.importer.domain.parse.bank.PdfStatements
import com.neteinstein.networth.feature.importer.domain.parse.bank.accountTypeFor
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Profiles read the synthetic statements in [BankStatementFixtures]. */
class StatementProfilesTest {
    @Test
    fun novobancoSectionsRowsBalancesAndHoldings() {
        val statement = PdfStatements.parse("extrato.pdf", BankStatementFixtures.novobanco)
        assertEquals("novobanco", statement.bank)
        assertEquals(2, statement.sections.size)
        val current = statement.sections[0]
        assertEquals("360° - DO", current.label)
        assertEquals("000123456789", current.accountNumber)
        assertEquals("PT50000700000012345678901", current.iban)
        assertEquals(AccountType.CURRENT, current.suggestedType)
        assertEquals(LocalDate(2026, 9, 1), current.periodStart)
        assertEquals(LocalDate(2026, 10, 1), current.periodEnd)
        assertEquals(100_000, current.openingBalanceMinor)
        assertEquals(297_273, current.closingBalanceMinor)
        assertEquals(listOf(-2_550L, -177L, 200_000L), current.rows.map { it.amountMinor })
        assertEquals("Compra Mb Cartão ****111* Mercearia Lisboa", current.rows[0].description)
        assertEquals("Trf Cred Sepa+ De Empresa Exemplo Lda", current.rows[2].description)
        assertEquals("Loja Exemplo 1,77 Eur Cartão ****111*", current.rows[1].description)
        assertTrue(ImportPlanner.reconciles(current))

        val savings = statement.sections[1]
        assertEquals(AccountType.SAVINGS, savings.suggestedType)
        assertNull(savings.iban)
        assertEquals(0, savings.rows.size)

        val mortgage = statement.holdings.single()
        assertEquals(AccountType.MORTGAGE, mortgage.type)
        assertEquals(8_000_000, mortgage.balanceMinor)
        assertEquals("0999888777", mortgage.accountNumber)
        assertEquals(LocalDate(2026, 10, 1), mortgage.date)

        val scheduled = statement.scheduled.single()
        assertEquals(-50_000, scheduled.amountMinor)
        assertEquals(LocalDate(2026, 10, 2), scheduled.date)
        assertEquals("000123456789", scheduled.accountNumber)
    }

    @Test
    fun bancoBestSectionsAndFunds() {
        val statement = PdfStatements.parse("best.pdf", BankStatementFixtures.best)
        assertEquals("Banco Best", statement.bank)
        val (current, deposit) = statement.sections
        assertEquals("111122223333", current.accountNumber)
        assertEquals("PT50006500001111222233312", current.iban)
        assertEquals(AccountType.CURRENT, current.suggestedType)
        assertEquals(listOf(-10_000L), current.rows.map { it.amountMinor })
        assertEquals("COBRANÇA SDD LUZ ADC 123456789", current.rows.single().description)
        assertEquals(LocalDate(2015, 10, 1), current.periodStart)
        assertEquals(AccountType.TERM_DEPOSIT, deposit.suggestedType)
        assertEquals(listOf(200L), deposit.rows.map { it.amountMinor })
        assertTrue(ImportPlanner.reconciles(deposit))
        val fund = statement.holdings.single()
        assertEquals(AccountType.INVESTMENT, fund.type)
        assertEquals(150_000, fund.balanceMinor)
        assertEquals(LocalDate(2015, 11, 1), fund.date)
    }

    @Test
    fun genericProfileUsesColumnsWhenThereIsNoBalance() {
        val lines = BankStatementFixtures.generic
        val statement = PdfStatements.parse("other.pdf", lines)
        val section = statement.sections.single()
        assertEquals("DE89370400440532013000", section.iban)
        assertEquals(listOf(-320L, 150_000L, -400L), section.rows.map { it.amountMinor })
        assertTrue(GenericPdfProfile.matches(lines))
        assertTrue(NovobancoProfile.matches(BankStatementFixtures.novobanco))
        assertTrue(BancoBestProfile.matches(BankStatementFixtures.best))
    }

    @Test
    fun activoBankRowsGetTheYearFromThePeriod() {
        val lines = BankStatementFixtures.activoBank
        assertTrue(NovobancoProfile.matches(lines), "a description names novobanco")
        val statement = PdfStatements.parse("activo.pdf", lines)
        assertEquals("ActivoBank", statement.bank)
        val account = statement.sections.single()
        assertEquals("CONTA SIMPLES", account.label)
        assertEquals("11122233344", account.accountNumber)
        assertEquals("PT50002300001112223334455", account.iban)
        assertEquals(AccountType.CURRENT, account.suggestedType)
        assertEquals(LocalDate(2021, 10, 1), account.periodStart)
        assertEquals(LocalDate(2021, 10, 29), account.periodEnd)
        assertEquals(307_612, account.openingBalanceMinor)
        assertEquals(532_824, account.closingBalanceMinor)
        assertEquals(listOf(-2_200L, 2_435L, -8_823L, 238_300L, -4_500L), account.rows.map { it.amountMinor })
        assertEquals(
            listOf(
                LocalDate(2021, 10, 1),
                LocalDate(2021, 10, 8),
                LocalDate(2021, 10, 11),
                LocalDate(2021, 10, 25),
                LocalDate(2021, 10, 29),
            ),
            account.rows.map { it.date },
        )
        assertEquals(LocalDate(2021, 10, 7), account.rows[1].valueDate)
        assertEquals(LocalDate(2021, 10, 25), account.rows[3].valueDate)
        assertEquals("IVAUCHER 1234 TXN 02/10/2021 48.70EUR", account.rows[1].description)
        assertEquals("TRANSFERENCIA - VENCIMENTO", account.rows[3].description)
        assertTrue(ImportPlanner.reconciles(account))

        val order = statement.scheduled.single()
        assertEquals("RENDA CASA", order.description)
        assertEquals(LocalDate(2021, 11, 2), order.date)
        assertEquals(-55_000, order.amountMinor)
        assertEquals("11122233344", order.accountNumber)
    }

    @Test
    fun activoBankPeriodAcrossTheNewYear() {
        val account = PdfStatements.parse("activo.pdf", BankStatementFixtures.activoBankNewYear).sections.single()
        assertEquals(
            listOf(LocalDate(2019, 12, 16), LocalDate(2019, 12, 30), LocalDate(2020, 1, 2)),
            account.rows.map { it.date },
        )
        assertEquals(LocalDate(2019, 12, 14), account.rows[0].valueDate)
        assertEquals(listOf(-1_000L, 550L, -100_000L), account.rows.map { it.amountMinor })
        assertEquals(-90_450, account.closingBalanceMinor)
        assertNull(account.iban)
        assertTrue(ImportPlanner.reconciles(account))
    }

    @Test
    fun millenniumSharesTheExtratoCombinado() {
        val statement = PdfStatements.parse("millennium.pdf", BankStatementFixtures.millennium)
        assertEquals("Millennium bcp", statement.bank)
        val account = statement.sections.single()
        assertEquals("CONTA CORRENTE", account.label)
        assertEquals("12345678901", account.accountNumber)
        assertEquals("PT50003300001234567890105", account.iban)
        assertEquals(LocalDate(2021, 12, 1), account.periodStart)
        assertEquals(listOf(76_796L, -15_106L), account.rows.map { it.amountMinor })
        assertEquals(LocalDate(2021, 12, 20), account.rows[1].date)
        assertEquals(214_030, account.closingBalanceMinor)
        assertTrue(ImportPlanner.reconciles(account))
    }

    @Test
    fun santanderTakesTheYearFromThePeriod() {
        val statement = PdfStatements.parse("santander.pdf", BankStatementFixtures.santander)
        assertEquals("Santander", statement.bank)
        val account = statement.sections.single()
        assertEquals("Conta à Ordem", account.label)
        assertEquals("000001234567001", account.accountNumber)
        assertEquals("PT50001800000123456700195", account.iban)
        assertEquals(LocalDate(2022, 12, 1), account.periodStart)
        assertEquals(LocalDate(2022, 12, 30), account.periodEnd)
        assertEquals(120_000, account.openingBalanceMinor)
        assertEquals(130_000, account.closingBalanceMinor)
        assertEquals(listOf(-15_025L, 30_000L, -4_975L), account.rows.map { it.amountMinor })
        assertEquals(listOf(LocalDate(2022, 12, 5), LocalDate(2022, 12, 20), LocalDate(2022, 12, 29)), account.rows.map { it.date })
        assertEquals(LocalDate(2023, 1, 2), account.rows[2].valueDate)
        assertTrue(ImportPlanner.reconciles(account))
    }

    @Test
    fun bpiReadsTheSignedColumnAndTheAccountIban() {
        val statement = PdfStatements.parse("bpi.pdf", BankStatementFixtures.bpi)
        assertEquals("BPI", statement.bank)
        val account = statement.sections.single()
        assertEquals("CONTA A ORDEM BPI", account.label)
        assertEquals("11234567001001", account.accountNumber)
        assertEquals("PT50001000001234567000187", account.iban)
        assertEquals(LocalDate(2021, 12, 31), account.periodEnd)
        assertEquals(250_000, account.openingBalanceMinor)
        assertEquals(200_000, account.closingBalanceMinor)
        assertEquals(listOf(-107_508L, -2_492L, 60_000L), account.rows.map { it.amountMinor })
        assertEquals(LocalDate(2021, 12, 22), account.rows[0].date)
        assertTrue(ImportPlanner.reconciles(account))
    }

    @Test
    fun montepioIgnoresTheCurrencyColumn() {
        val statement = PdfStatements.parse("montepio.pdf", BankStatementFixtures.montepio)
        assertEquals("Montepio", statement.bank)
        val account = statement.sections.single()
        assertEquals("CONTA ORDENADO", account.label)
        assertEquals("123100045678", account.accountNumber)
        assertEquals("PT50003601230123456789012", account.iban)
        assertEquals(LocalDate(2021, 12, 1), account.periodStart)
        assertEquals(listOf(10_187L, -187L), account.rows.map { it.amountMinor })
        assertEquals(110_000, account.closingBalanceMinor)
        assertTrue(ImportPlanner.reconciles(account))
    }

    @Test
    fun onlineMovementListsFallBackToTheGenericReaderButKeepTheBank() {
        val statement = PdfStatements.parse("net24.pdf", BankStatementFixtures.montepioOnline)
        assertEquals("Montepio", statement.bank)
        assertEquals(listOf(-187L, 10_187L), statement.sections.single().rows.map { it.amountMinor })
    }

    @Test
    fun cgdReadsEachAccountWithItsIban() {
        val statement = PdfStatements.parse("cgd.pdf", BankStatementFixtures.cgd)
        assertEquals("CGD", statement.bank)
        val account = statement.sections.single()
        assertEquals("CONTA EXTRACTO", account.label)
        assertEquals("0123004567890", account.accountNumber)
        assertEquals("PT50003501230000456789012", account.iban)
        assertEquals(500_000, account.openingBalanceMinor)
        assertEquals(400_000, account.closingBalanceMinor)
        assertEquals(listOf(2_260L, -102_260L), account.rows.map { it.amountMinor })
        assertEquals(LocalDate(2021, 11, 30), account.rows[1].valueDate)
        assertTrue(ImportPlanner.reconciles(account))
    }

    @Test
    fun creditoAgricolaTakesTheSignFromTheDebitCreditColumn() {
        val statement = PdfStatements.parse("ca.pdf", BankStatementFixtures.creditoAgricola)
        assertEquals("Crédito Agrícola", statement.bank)
        val account = statement.sections.single()
        assertEquals("12345678901", account.accountNumber)
        assertEquals(LocalDate(2022, 12, 1), account.periodStart)
        assertEquals(LocalDate(2022, 12, 31), account.periodEnd)
        assertEquals(listOf(-3_690L, 10_000L), account.rows.map { it.amountMinor })
        assertEquals(listOf("SEPA DD-EXEMPLO SA", "TRF DE EXEMPLO"), account.rows.map { it.description })
        assertEquals(136_310, account.closingBalanceMinor)
    }

    @Test
    fun novobancoBusinessExtractHasNoAccountHeading() {
        val statement = PdfStatements.parse("empresa.pdf", BankStatementFixtures.novobancoBusiness)
        assertEquals("novobanco", statement.bank)
        val account = statement.sections.single()
        assertEquals("000123450002", account.accountNumber)
        assertEquals(LocalDate(2022, 12, 2), account.periodStart)
        assertEquals(LocalDate(2023, 1, 2), account.periodEnd)
        assertEquals(listOf(-737L, -3_908L), account.rows.map { it.amountMinor })
        assertEquals("COM Comissão Garantia Bancária", account.rows[1].description)
        assertTrue(ImportPlanner.reconciles(account))
    }

    @Test
    fun emptyPdfIsNotAStatement() {
        assertFailsWith<ParseException> { PdfStatements.parse("x.pdf", listOf(line(0, 800, 50.0 to "Hello"))) }
    }

    @Test
    fun guessesAccountTypesFromProductNames() {
        assertEquals(AccountType.MORTGAGE, accountTypeFor("Crédito Habitação"))
        assertEquals(AccountType.LOAN, accountTypeFor("Crédito Pessoal"))
        assertEquals(AccountType.TERM_DEPOSIT, accountTypeFor("DP STANDARD RENOVAVEL"))
        assertEquals(AccountType.INVESTMENT, accountTypeFor("Carteira de Fundos"))
        assertEquals(AccountType.SAVINGS, accountTypeFor("Conta Poupança"))
        assertEquals(AccountType.CURRENT, accountTypeFor("Conta Ordenado"))
    }
}
