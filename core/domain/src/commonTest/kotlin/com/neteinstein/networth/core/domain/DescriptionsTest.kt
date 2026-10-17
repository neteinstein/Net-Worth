package com.neteinstein.networth.core.domain

import com.neteinstein.networth.core.domain.text.Descriptions
import com.neteinstein.networth.core.domain.text.MovementKind
import kotlin.test.Test
import kotlin.test.assertEquals

class DescriptionsTest {
    @Test
    fun describesPortugueseMovements() {
        fun kind(raw: String) = Descriptions.describe(raw).kind
        assertEquals(MovementKind.LOW_VALUE_CARD, kind("Pagamento Baixo Valor Cartão **** ****999*"))
        assertEquals(MovementKind.CASH_WITHDRAWAL, kind("Levantamento Mb Cartão **** 999*"))
        assertEquals(MovementKind.MORTGAGE, kind("Pagamento Prestação Ch N° 55 Processo 1"))
        assertEquals(MovementKind.TAX, kind("Imp Selo S/ Com"))
        assertEquals(MovementKind.FEE, kind("Manutencao Conta Pacote"))
        assertEquals(MovementKind.INTEREST, kind("JUROS 0001 A"))
        assertEquals(MovementKind.REFUND, kind("DEVOLUÇÃO COMPRA CARTÃO 1234567* LOJA"))
        assertEquals(MovementKind.DIRECT_DEBIT, kind("Cobrança Sdd Meo, Sa Adc 1234"))
        assertEquals(MovementKind.SERVICE_PAYMENT, kind("Pag Serv Colegio Exemplo App 678 55555"))
        assertEquals(MovementKind.TRANSFER_IN, kind("TRANSF.INTERBANCARIA SEPA DE Empresa"))
        assertEquals(MovementKind.TRANSFER_OUT, kind("P/ Eng Pedro App 111222333"))
        assertEquals(MovementKind.CARD_PURCHASE, kind("Compra Mbway Cartão **** 999* Uber Rides Portugal"))
        assertEquals(MovementKind.OTHER, kind("Loja Exemplo 1,77 Eur Cartão ****999*"))
        val purchase = Descriptions.describe("COMPRA MB CARTÃO 1234567* FERRAGENS SILVA & FILHOS LDA 1000-001 LISBOA")
        assertEquals("Ferragens Silva & Filhos Lda Lisboa", purchase.merchant)
        assertEquals("FERRAGENS", purchase.key)
        assertEquals("Ana Lima", Descriptions.describe("Trf Imediata C/Cartao (Mbway)-App Terceiros P/ Ana Lima").counterparty)
        assertEquals("9** *** 412", Descriptions.describe("MB WAY P/ 9** *** 412").phone)
        assertEquals("MEO SA", Descriptions.keyOf("MEO, SA"))
        assertEquals("", Descriptions.keyOf(""))
    }
}
