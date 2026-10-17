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

/**
 * Synthetic statements with the same layout as the real novobanco and Banco Best extracts
 * (names, numbers and amounts are invented).
 */
class StatementProfilesTest {
    private val novobanco =
        listOf(
            line(0, 800, 50.0 to "Conta à Ordem nº 0001 2345 6789"),
            line(0, 790, 50.0 to "IBAN PT50 0007 0000 0012 3456 7890 1"),
            line(0, 780, 50.0 to "Data Extrato Atual 01.10.2026"),
            line(0, 700, 50.0 to "AGENDA DE MOVIMENTOS PREVISTOS"),
            line(0, 690, 50.0 to "Descritivo", 300.0 to "Data", 360.0 to "Moeda", 420.0 to "Montante (Euros)"),
            line(0, 680, 50.0 to "Hipoteca Casa", 300.0 to "02.10.2026", 360.0 to "EUR", 420.0 to "500,00"),
            line(0, 100, 50.0 to "O Novo Banco, S.A. é uma entidade"),
            line(1, 800, 50.0 to "CONTA 360° - DO nº 0001 2345 6789 de 01.09.2026 a 01.10.2026"),
            line(
                1,
                790,
                30.0 to "Data",
                70.0 to "Valor",
                110.0 to "Descritivo",
                380.0 to "Débito",
                450.0 to "Crédito",
                510.0 to "Saldo (Euros)",
            ),
            line(1, 780, 30.0 to "01.09.26", 110.0 to "SALDO ANTERIOR", 510.0 to "1.000,00"),
            line(
                1,
                770,
                30.0 to "02.09.26",
                70.0 to "02.09.26",
                110.0 to "Compra Mb Cartão ****111* Mercearia",
                380.0 to "25,50",
                510.0 to "974,50",
            ),
            line(1, 760, 110.0 to "Lisboa"),
            line(
                1,
                750,
                30.0 to "03.09.26",
                70.0 to "03.09.26",
                110.0 to "Loja Exemplo 1,77 Eur Cartão ****111*",
                380.0 to "1,77",
                510.0 to "972,73",
            ),
            line(1, 740, 300.0 to "Processado por Computador"),
            line(1, 100, 50.0 to "Extrato Integrado nº 1/2026 Página 2|3"),
            line(2, 800, 50.0 to "CONTA 360° - DO nº 0001 2345 6789 de 01.09.2026 a 01.10.2026"),
            line(
                2,
                790,
                57.0 to "Data",
                97.0 to "Valor",
                138.0 to "Descritivo",
                400.0 to "Débito",
                470.0 to "Crédito",
                546.0 to "Saldo (Euros)",
            ),
            line(
                2,
                780,
                57.0 to "24.09.26",
                97.0 to "24.09.26",
                138.0 to "Trf Cred Sepa+ De Empresa Exemplo",
                470.0 to "2.000,00",
                546.0 to "2.972,73",
            ),
            line(2, 770, 138.0 to "Lda"),
            line(2, 760, 57.0 to "TOTAL", 400.0 to "27,27", 470.0 to "2.000,00"),
            line(2, 750, 57.0 to "01.10.26", 138.0 to "SALDO CONTABILÍSTICO", 546.0 to "2.972,73"),
            line(2, 740, 50.0 to "CONTA 360° POUPANCA nº 0001 2345 0000 de 01.09.2026 a 01.10.2026"),
            line(2, 730, 57.0 to "01.09.26", 138.0 to "SALDO ANTERIOR", 546.0 to "0,00"),
            line(2, 720, 57.0 to "01.10.26", 138.0 to "SALDO CONTABILÍSTICO", 546.0 to "0,00"),
            line(2, 700, 50.0 to "CRÉDITO HABITAÇÃO"),
            line(2, 690, 50.0 to "Hipoteca Casa", 200.0 to "0999888777", 260.0 to "EUR", 300.0 to "100.000,00", 520.0 to "80.000,00"),
            line(2, 680, 50.0 to "TOTAL em Euros", 520.0 to "80.000,00"),
            line(2, 600, 50.0 to "AVISOS DE LANÇAMENTO"),
        )

    @Test
    fun novobancoSectionsRowsBalancesAndHoldings() {
        val statement = PdfStatements.parse("extrato.pdf", novobanco)
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

    private val best =
        listOf(
            line(0, 800, 50.0 to "EXTRATO INTEGRADO N.º 11"),
            line(0, 790, 50.0 to "Data do extrato atual: 01-11-15"),
            line(1, 800, 50.0 to "Detalhe dos ativos"),
            line(1, 790, 50.0 to "Depósitos à ordem"),
            line(1, 780, 50.0 to "N.º Conta", 120.0 to "Designação", 300.0 to "Saldo (Euros)"),
            line(1, 770, 50.0 to "111122223333", 120.0 to "CONTA + ORDENADO", 300.0 to "EUR", 400.0 to "900,00", 500.0 to "900,00"),
            line(1, 760, 50.0 to "NIB: 0065.0000.11112222333.12"),
            line(1, 740, 50.0 to "Fundos de investimento"),
            line(1, 730, 50.0 to "N.º Carteira", 120.0 to "Designação", 500.0 to "Montante (Euros)"),
            line(1, 720, 50.0 to "60000111222", 120.0 to "CARTEIRA DE FUNDOS", 500.0 to "1.500,00"),
            line(1, 710, 50.0 to "FUNDO EXEMPLO", 300.0 to "10,0", 500.0 to "1.500,00"),
            line(1, 600, 50.0 to "Detalhe de movimentos"),
            line(1, 590, 50.0 to "Depósitos à ordem"),
            line(1, 580, 50.0 to "N.º Conta", 120.0 to "Designação", 250.0 to "Data Início", 320.0 to "Data Fim", 390.0 to "Moeda Orig."),
            line(1, 570, 50.0 to "111122223333", 120.0 to "CONTA + ORDENADO", 250.0 to "01-10-15", 320.0 to "01-11-15", 390.0 to "EUR"),
            line(
                1,
                560,
                50.0 to "Data",
                100.0 to "Data Valor",
                160.0 to "Descritivo",
                380.0 to "Débito",
                450.0 to "Crédito",
                510.0 to "Saldo Moeda Orig.",
            ),
            line(1, 550, 160.0 to "Saldo inicial", 510.0 to "1.000,00"),
            line(1, 540, 50.0 to "05-10-15", 100.0 to "05-10-15", 160.0 to "COBRANÇA SDD LUZ ADC", 380.0 to "100,00", 510.0 to "900,00"),
            line(1, 530, 160.0 to "123456789"),
            line(1, 520, 160.0 to "Total de débitos", 380.0 to "100,00"),
            line(1, 510, 160.0 to "Saldo contabilístico", 510.0 to "900,00"),
            line(2, 800, 50.0 to "Depósitos a prazo e contas poupança"),
            line(2, 790, 50.0 to "N.º Conta", 120.0 to "Designação", 250.0 to "Data Início", 320.0 to "Data Fim", 390.0 to "Moeda Orig."),
            line(
                2,
                780,
                50.0 to "000044445555",
                120.0 to "DEPOSITO A MEDIDA RENOVAVEL",
                250.0 to "01-10-15",
                320.0 to "01-11-15",
                390.0 to "EUR",
            ),
            line(
                2,
                770,
                50.0 to "Data",
                100.0 to "Data Valor",
                160.0 to "Descritivo",
                380.0 to "Débito",
                450.0 to "Crédito",
                510.0 to "Saldo",
            ),
            line(2, 760, 50.0 to "01-10-15", 160.0 to "Saldo inicial", 510.0 to "1.000,00"),
            line(2, 750, 50.0 to "29-10-15", 100.0 to "29-10-15", 160.0 to "JUROS", 450.0 to "2,00", 510.0 to "1.002,00"),
            line(2, 740, 50.0 to "01-11-15", 160.0 to "Saldo contabilístico", 510.0 to "1.002,00"),
            line(2, 700, 50.0 to "Avisos de lançamento"),
            line(2, 690, 50.0 to "Importância", 300.0 to "771,10"),
            line(2, 100, 50.0 to "BEST - Banco Electrónico de Serviço Total, S.A."),
        )

    @Test
    fun bancoBestSectionsAndFunds() {
        val statement = PdfStatements.parse("best.pdf", best)
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
        val lines =
            listOf(
                line(0, 800, 50.0 to "Some Bank · IBAN: DE89 3704 0044 0532 0130 00"),
                line(0, 790, 50.0 to "Date", 120.0 to "Description", 380.0 to "Debit", 450.0 to "Credit"),
                line(0, 780, 50.0 to "2026-09-02", 120.0 to "Coffee", 380.0 to "3,20"),
                line(0, 770, 50.0 to "2026-09-03", 120.0 to "Salary", 450.0 to "1.500,00"),
                line(0, 760, 50.0 to "2026-09-04", 120.0 to "Refund", 380.0 to "-4,00"),
            )
        val statement = PdfStatements.parse("other.pdf", lines)
        val section = statement.sections.single()
        assertEquals("DE89370400440532013000", section.iban)
        assertEquals(listOf(-320L, 150_000L, -400L), section.rows.map { it.amountMinor })
        assertTrue(GenericPdfProfile.matches(lines))
        assertTrue(NovobancoProfile.matches(novobanco))
        assertTrue(BancoBestProfile.matches(best))
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
