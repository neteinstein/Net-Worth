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
