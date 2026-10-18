package com.neteinstein.networth.feature.importer

/**
 * Synthetic statements laid out like the real PDF extracts of each Portuguese bank the importer knows
 * (novobanco, Banco Best, ActivoBank). Names, account numbers and amounts are invented.
 */
internal object BankStatementFixtures {
    val novobanco =
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

    val best =
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

    /**
     * ActivoBank "Extrato Combinado": `mm.dd` dates without the year, space-separated thousands, `#S#…#E#`
     * markers around the IBAN, underscore rules, carried balances between pages, and a points programme with
     * its own "SALDO FINAL" after the account. One description names another bank.
     */
    val activoBank =
        listOf(
            line(
                0,
                800,
                30.0 to "21/10/29 EXT. N. 2021/010 DEPOSITO A ORDEM: 11122233344",
                330.0 to "NIB: #S#002300001112223334455#E#",
            ),
            line(0, 790, 30.0 to "EXTRATO COMBINADO"),
            line(0, 780, 30.0 to "MOEDA BASE: EURO", 330.0 to "#S#IBAN: PT50 0023 0000 1112 2233 3445 5#E#"),
            line(0, 770, 330.0 to "BIC/SWIFT: ACTVPTPL"),
            line(0, 700, 30.0 to "AGENDA DE ORDENS DE PAGAMENTO PERMANENTES E/OU PONTUAIS:"),
            line(0, 690, 30.0 to "DATA", 90.0 to "MONTANTE", 160.0 to "MOEDA", 200.0 to "DESTINO", 330.0 to "OBSERVACOES"),
            line(0, 680, 30.0 to "2021/11/02", 90.0 to "550.00", 160.0 to "EUR", 200.0 to "001800000999888777666", 330.0 to "RENDA CASA"),
            line(0, 660, 30.0 to "__________ RESUMO DAS CONTAS __________"),
            line(0, 650, 30.0 to "CONTA SIMPLES", 500.0 to "5 328.24"),
            line(1, 800, 30.0 to "21/10/29 EXT. N. 2021/010 DEPOSITO A ORDEM: 11122233344", 330.0 to "PAG: 002"),
            line(1, 780, 30.0 to "__________ CONTA SIMPLES N. 11122233344 MOEDA: EUR__________"),
            line(1, 770, 30.0 to "EXTRATO DE 2021/10/01 A 2021/10/29"),
            line(1, 760, 30.0 to "DATA LANC."),
            line(1, 750, 30.0 to "DATA VALOR", 95.0 to "DESCRITIVO", 380.0 to "DEBITO", 440.0 to "CREDITO", 500.0 to "SALDO"),
            line(1, 740, 95.0 to "SALDO INICIAL", 500.0 to "3 076.12"),
            line(
                1,
                730,
                30.0 to "10.01",
                60.0 to "10.01",
                95.0 to "COMPRA 1234 MERCEARIA EXEMPLO CONTACTLESS",
                380.0 to "22.00",
                500.0 to "3 054.12",
            ),
            line(
                1,
                720,
                30.0 to "10.08",
                60.0 to "10.07",
                95.0 to "IVAUCHER 1234 TXN 02/10/2021 48.70EUR",
                440.0 to "24.35",
                500.0 to "3 078.47",
            ),
            line(
                1,
                710,
                30.0 to "10.11",
                60.0 to "10.11",
                95.0 to "PAG SERV 12345/678901234 NOVO BANCO S.A.",
                380.0 to "88.23",
                500.0 to "2 990.24",
            ),
            line(1, 700, 95.0 to "A TRANSPORTAR", 500.0 to "2 990.24"),
            line(2, 800, 30.0 to "21/10/29 EXT. N. 2021/010 DEPOSITO A ORDEM: 11122233344", 330.0 to "PAG: 003"),
            line(2, 780, 30.0 to "DATA LANC."),
            line(2, 770, 30.0 to "DATA VALOR", 95.0 to "DESCRITIVO", 380.0 to "DEBITO", 440.0 to "CREDITO", 500.0 to "SALDO"),
            line(2, 760, 95.0 to "TRANSPORTE", 500.0 to "2 990.24"),
            line(2, 750, 30.0 to "10.25 10.25", 95.0 to "TRANSFERENCIA - VENCIMENTO", 440.0 to "2 383.00", 500.0 to "5 373.24"),
            line(2, 740, 30.0 to "10.29", 60.0 to "10.29", 95.0 to "TRF P/ ESCOLA EXEMPLO LDA", 380.0 to "45.00", 500.0 to "5 328.24"),
            line(2, 730, 95.0 to "SALDO FINAL", 500.0 to "5 328.24"),
            line(2, 720, 95.0 to "SALDO DISPONIVEL", 500.0 to "5 228.24"),
            line(2, 700, 30.0 to "__________ PROGRAMAS DE BENEFICIOS __________"),
            line(2, 690, 30.0 to "SALDO MES ANTERIOR", 500.0 to "3 700.00"),
            line(2, 680, 30.0 to "SALDO FINAL", 500.0 to "3 500.00"),
        )

    /** An ActivoBank period across the new year, with a value date before it starts. */
    val activoBankNewYear =
        listOf(
            line(0, 800, 30.0 to "19/12/31 EXT. N. 2020/001 DEPOSITO A ORDEM: 11122233344"),
            line(0, 790, 330.0 to "BIC/SWIFT: ACTVPTPL"),
            line(0, 780, 30.0 to "CONTA SIMPLES N. 11122233344 MOEDA: EUR"),
            line(0, 770, 30.0 to "EXTRATO DE 2019/12/16 A 2020/01/15"),
            line(0, 760, 30.0 to "DATA DATA"),
            line(0, 750, 30.0 to "LANC.VALOR", 95.0 to "DESCRITIVO", 380.0 to "DEBITO", 440.0 to "CREDITO", 500.0 to "SALDO"),
            line(0, 740, 95.0 to "SALDO INICIAL", 500.0 to "100.00"),
            line(0, 730, 30.0 to "12.16", 60.0 to "12.14", 95.0 to "COMPRA 1234 LIVRARIA EXEMPLO", 380.0 to "10.00", 500.0 to "90.00"),
            line(0, 720, 30.0 to "12.30", 60.0 to "12.30", 95.0 to "TRF MB WAY DE AMIGO EXEMPLO", 440.0 to "5.50", 500.0 to "95.50"),
            line(0, 710, 30.0 to "1.02", 60.0 to "1.02", 95.0 to "PAG BXVAL- 1234 PORTAGEM", 380.0 to "1 000.00", 500.0 to "-904.50"),
            line(0, 700, 95.0 to "SALDO FINAL", 500.0 to "-904.50"),
        )

    /** Millennium bcp "Extrato Combinado": ActivoBank's layout with "CONTA: <number>" in the page header. */
    val millennium =
        listOf(
            line(0, 800, 30.0 to "21/12/31", 120.0 to "CONTA: 12345678901", 300.0 to "NIB: 003300001234567890105"),
            line(0, 790, 30.0 to "EXTRATO COMBINADO"),
            line(0, 780, 30.0 to "N. 2021/012"),
            line(0, 770, 30.0 to "MOEDA BASE: EURO", 300.0 to "IBAN: PT50 0033 0000 1234 5678 9010 5"),
            line(0, 760, 300.0 to "BIC/SWIFT: BCOMPTPL"),
            line(0, 740, 30.0 to "RESUMO DO EXTRATO"),
            line(0, 730, 30.0 to "DEPOSITOS A ORDEM", 500.0 to "2 140.30"),
            line(0, 720, 30.0 to "EMPRESTIMOS", 500.0 to "50 000.00-"),
            line(0, 700, 30.0 to "CONTA CORRENTE N. 12345678901 MOEDA: EUR"),
            line(0, 690, 30.0 to "EXTRATO DE 2021/12/01 A 2021/12/31"),
            line(0, 680, 30.0 to "DATA DATA"),
            line(0, 670, 30.0 to "LANC. VALOR", 95.0 to "DESCRITIVO", 380.0 to "DÉBITO", 440.0 to "CRÉDITO", 500.0 to "SALDO"),
            line(0, 660, 95.0 to "SALDO INICIAL", 500.0 to "1 523.40"),
            line(
                0,
                650,
                30.0 to "12.01",
                60.0 to "12.01",
                95.0 to "COB PAG SERVICOS ENTIDADE 10001",
                440.0 to "767.96",
                500.0 to "2 291.36",
            ),
            line(0, 640, 30.0 to "12.20", 60.0 to "12.20", 95.0 to "TRF P/ FORNECEDOR EXEMPLO LDA", 380.0 to "151.06", 500.0 to "2 140.30"),
            line(0, 630, 95.0 to "SALDO FINAL", 500.0 to "2 140.30"),
        )

    /**
     * Santander "Extrato": account and period in the page header, `dd-mm` dates, one amount column signed by the
     * balance, a value date in the next year.
     */
    val santander =
        listOf(
            line(
                0,
                800,
                30.0 to "EXTRATO Nº 12",
                110.0 to "CONTA Nº 0000.01234567001",
                240.0 to "PERÍODO DE 2022-12-01 A 2022-12-30",
                420.0 to "PÁGINA 001/002",
            ),
            line(0, 780, 30.0 to "N.I.B.: 0018 0000 01234567001 95"),
            line(0, 770, 30.0 to "IBAN: PT50 0018 0000 0123 4567 0019 5"),
            line(0, 760, 30.0 to "BIC/SWIFT: TOTAPTPL"),
            line(
                1,
                800,
                30.0 to "EXTRATO Nº 12",
                110.0 to "CONTA Nº 0000.01234567001",
                240.0 to "PERÍODO DE 2022-12-01 A 2022-12-30",
                420.0 to "PÁGINA 002/002",
            ),
            line(1, 780, 30.0 to "Detalhe de Movimentos da Conta à Ordem"),
            line(1, 770, 30.0 to "Moeda: EUR"),
            line(1, 760, 30.0 to "Data", 70.0 to "Data"),
            line(1, 750, 30.0 to "Mov.", 70.0 to "Valor", 120.0 to "Descritivo de Movimento", 400.0 to "Montante", 500.0 to "Saldo"),
            line(1, 740, 120.0 to "Saldo Inicial", 440.0 to "EUR", 500.0 to "1.200,00"),
            line(1, 730, 30.0 to "05-12", 70.0 to "06-12", 120.0 to "PAG.CTA.CARTAO 1234", 400.0 to "150,25", 500.0 to "1.049,75"),
            line(1, 720, 30.0 to "20-12", 70.0 to "20-12", 120.0 to "DEPÓSITO NUMERÁRIO", 400.0 to "300,00", 500.0 to "1.349,75"),
            line(1, 710, 30.0 to "29-12", 70.0 to "02-01", 120.0 to "TRF P/ EXEMPLO", 400.0 to "49,75", 500.0 to "1.300,00"),
            line(1, 700, 120.0 to "Saldo Contabilístico Final EUR", 500.0 to "1.300,00"),
            line(1, 690, 120.0 to "Saldo Disponível Final EUR", 500.0 to "1.300,00"),
            line(1, 600, 30.0 to "Banco Santander Totta, S.A."),
        )

    /** BPI "Extracto de Conta": `dd/mm` dates, one signed column, a balance printed on the IBAN's line. */
    val bpi =
        listOf(
            line(0, 800, 30.0 to "BPI", 300.0 to "EXTRACTO DE CONTA"),
            line(0, 790, 30.0 to "Conta", 80.0 to "1-1234567-001-001"),
            line(0, 780, 30.0 to "Extracto", 80.0 to "012/2021"),
            line(0, 770, 30.0 to "Período", 80.0 to "De 01/12/2021 a 31/12/2021"),
            line(0, 750, 30.0 to "DEPÓSITOS À ORDEM"),
            line(
                0,
                740,
                30.0 to "DATA",
                70.0 to "DATA",
                120.0 to "DESCRIÇÃO DO MOVIMENTO",
                380.0 to "MOEDA",
                440.0 to "VALOR",
                500.0 to "SALDO",
            ),
            line(0, 730, 30.0 to "MOV", 70.0 to "VAL"),
            line(0, 720, 30.0 to "CONTA A ORDEM BPI Nº: 1-1234567-001-001", 380.0 to "EUR"),
            line(0, 710, 30.0 to "NIB: 0010 0000 12345670001 87"),
            line(0, 700, 30.0 to "IBAN: PT50 0010 0000 1234 5670 0018 7", 500.0 to "2 500,00"),
            line(0, 690, 120.0 to "SALDO ANTERIOR CONTABILISTICO", 500.0 to "2 500,00"),
            line(
                0,
                680,
                30.0 to "22/12",
                70.0 to "22/12",
                120.0 to "AMORTIZAÇÃO DE CAPITAL - 001234567-830-004",
                440.0 to "-1 075,08",
                500.0 to "1 424,92",
            ),
            line(
                0,
                670,
                30.0 to "22/12",
                70.0 to "22/12",
                120.0 to "JUROS DE EMPRESTIMO - 001234567-830-004",
                440.0 to "-24,92",
                500.0 to "1 400,00",
            ),
            line(0, 660, 30.0 to "28/12", 70.0 to "28/12", 120.0 to "TRF CR SEPA+ DE EXEMPLO", 440.0 to "600,00", 500.0 to "2 000,00"),
            line(0, 650, 120.0 to "SALDO ACTUAL CONTABILISTICO", 500.0 to "2 000,00"),
            line(0, 640, 120.0 to "SALDO ACTUAL DISPONIVEL", 500.0 to "2 000,00"),
            line(0, 600, 30.0 to "BANCO BPI, SA.", 300.0 to "SWIFT BBPIPTPL"),
        )

    /** Banco Montepio extract: ISO dates, amounts suffixed with + or -, and a currency column after the balance. */
    val montepio =
        listOf(
            line(0, 800, 30.0 to "Banco Montepio"),
            line(0, 790, 30.0 to "Dt. Emissão : 2021-12-31", 250.0 to "Nr.Ext.: 12/2021"),
            line(0, 780, 30.0 to "Período : 2021-12-01 a 2021-12-31"),
            line(0, 770, 30.0 to "NIB (Nº identif. Bancária) : 0036 0123 01234567890 12", 330.0 to "BIC : MPIOPTPL"),
            line(0, 760, 30.0 to "IBAN (NIB internacional) : PT50 0036 0123 0123 4567 8901 2", 330.0 to "Moeda: EUR - EURO"),
            line(0, 740, 30.0 to "DEPÓSITO À ORDEM - MOVIMENTOS - 123.10.004567-8 - CONTA ORDENADO"),
            line(0, 730, 30.0 to "MOEDA: EUR"),
            line(
                0,
                720,
                30.0 to "DT.MOVIM",
                80.0 to "DT.VALOR",
                130.0 to "DESCRIÇÃO",
                400.0 to "MONTANTE",
                470.0 to "SALDO",
                540.0 to "MOEDA",
            ),
            line(0, 710, 130.0 to "Saldo Inicial", 470.0 to "1.000,00", 540.0 to "EUR"),
            line(
                0,
                700,
                30.0 to "2021-12-01",
                80.0 to "2021-12-01",
                130.0 to "FECHO TPA 01234567 001",
                400.0 to "101,87+",
                470.0 to "1.101,87+",
                540.0 to "EUR",
            ),
            line(
                0,
                690,
                30.0 to "2021-12-02",
                80.0 to "2021-12-01",
                130.0 to "COMISSAO TPA 01234567",
                400.0 to "1,87-",
                470.0 to "1.100,00+",
                540.0 to "EUR",
            ),
        )

    /** Montepio's online "Consulta de Movimentos": no account heading, newest first, one signed column. */
    val montepioOnline =
        listOf(
            line(0, 800, 30.0 to "Banco Montepio", 300.0 to "Net24 Empresas"),
            line(0, 790, 30.0 to "Consulta de Movimentos"),
            line(0, 780, 30.0 to "Nº Conta", 120.0 to "123.10.004567-8"),
            line(0, 750, 30.0 to "Data Mov.", 80.0 to "Data Valor", 130.0 to "Descrição", 400.0 to "Importância", 470.0 to "Saldo Cont."),
            line(0, 740, 30.0 to "2021-12-02", 80.0 to "2021-12-01", 130.0 to "COMISSAO TPA", 400.0 to "-1,87", 470.0 to "1.100,00"),
            line(0, 730, 30.0 to "2021-12-01", 80.0 to "2021-12-01", 130.0 to "TR-EXEMPLO LDA", 400.0 to "101,87", 470.0 to "1.101,87"),
        )

    /** Caixa Geral de Depósitos "Extracto": account and IBAN per section, ISO dates, one signed column. */
    val cgd =
        listOf(
            line(0, 800, 30.0 to "Caixa Geral de Depósitos"),
            line(0, 780, 30.0 to "Depósitos à Ordem"),
            line(0, 770, 30.0 to "CONTA EXTRACTO 0123.004567.890"),
            line(0, 760, 30.0 to "IBAN PT50003501230000456789012", 400.0 to "NIB 003501230000456789012"),
            line(0, 750, 30.0 to "SWIFT/BIC CGDIPTPL", 450.0 to "Moeda EUR"),
            line(
                0,
                740,
                30.0 to "Data Mov.",
                80.0 to "Data Valor",
                130.0 to "Descrição",
                420.0 to "Valor",
                480.0 to "Saldo Contabilístico",
            ),
            line(0, 730, 130.0 to "Saldo Anterior", 480.0 to "5.000,00"),
            line(0, 720, 30.0 to "2021-12-01", 80.0 to "2021-12-01", 130.0 to "TRF EXEMPLO SILVA", 420.0 to "22,60", 480.0 to "5.022,60"),
            line(
                0,
                710,
                30.0 to "2021-12-02",
                80.0 to "2021-11-30",
                130.0 to "COB PAG SERV 10001",
                420.0 to "-1.022,60",
                480.0 to "4.000,00",
            ),
            line(0, 700, 130.0 to "Saldo contabilístico", 480.0 to "4.000,00"),
            line(0, 690, 130.0 to "Saldo disponível", 480.0 to "4.000,00"),
        )

    /**
     * Crédito Agrícola online "Consulta de Movimentos de Contas D.O.": the period on two lines after the account,
     * a "D"/"C" column for the sign, "EUR" after the amounts, and movement details under each row.
     */
    val creditoAgricola =
        listOf(
            line(0, 800, 30.0 to "Consulta de Movimentos de Contas D.O.", 330.0 to "Data de Impressão: 10-01-2023 às 11:27"),
            line(0, 780, 30.0 to "Número de Conta: 12345678901"),
            line(0, 770, 30.0 to "De: 01/12/2022"),
            line(0, 760, 30.0 to "A: 31/12/2022"),
            line(
                0,
                740,
                30.0 to "Data de",
                80.0 to "Data Valor",
                130.0 to "Descrição",
                260.0 to "Descritivo",
                380.0 to "Montante",
                440.0 to "Déb./",
                480.0 to "Saldo",
            ),
            line(0, 730, 30.0 to "Movimento", 440.0 to "Créd.", 480.0 to "Contabilístico"),
            line(
                0,
                720,
                30.0 to "30/12/2022",
                80.0 to "30/12/2022",
                130.0 to "SEPA DD-EXEMPLO SA",
                260.0 to "-",
                380.0 to "36,90 EUR",
                440.0 to "D",
                480.0 to "1.263,10 EUR",
            ),
            line(0, 710, 30.0 to "Dados do movimento"),
            line(0, 700, 30.0 to "Nome Ordenante", 130.0 to "EXEMPLO SA"),
            line(
                0,
                690,
                30.0 to "31/12/2022",
                80.0 to "31/12/2022",
                130.0 to "TRF DE EXEMPLO",
                260.0 to "-",
                380.0 to "100,00 EUR",
                440.0 to "C",
                480.0 to "1.363,10 EUR",
            ),
            line(0, 600, 30.0 to "Caixa Central - Caixa Central de Crédito Agrícola Mútuo, CRL"),
            line(0, 590, 30.0 to "Documento obtido na Internet em www.creditoagricola.pt"),
        )

    /** novobanco's business extract: "DEPOSITO A ORDEM DO - NORMAL" instead of "CONTA … nº …", and an "Origem" column. */
    val novobancoBusiness =
        listOf(
            line(0, 800, 30.0 to "novobanco"),
            line(0, 790, 30.0 to "Extrato nº 1/2023"),
            line(0, 780, 30.0 to "Conta à Ordem nº 0001 2345 0002"),
            line(0, 760, 30.0 to "Data Extrato Atual 02/01/2023"),
            line(0, 750, 30.0 to "Data Extrato Anterior 02/12/2022"),
            line(0, 700, 30.0 to "MOVIMENTOS DE CONTA"),
            line(0, 690, 30.0 to "DEPOSITO A ORDEM DO - NORMAL"),
            line(
                0,
                680,
                30.0 to "Data",
                75.0 to "Data Valor",
                120.0 to "Origem",
                160.0 to "Descritivo",
                400.0 to "Débito",
                460.0 to "Crédito",
                520.0 to "Saldo (Euros)",
            ),
            line(0, 670, 30.0 to "02/12/22", 160.0 to "SALDO ANTERIOR", 520.0 to "7.967,50"),
            line(
                0,
                660,
                30.0 to "13/12/22",
                75.0 to "13/12/22",
                120.0 to "COM",
                160.0 to "Manutenção Conta 13/11/2022 A 12/12/2022",
                400.0 to "7,37",
                520.0 to "7.960,13",
            ),
            line(
                0,
                650,
                30.0 to "29/12/22",
                75.0 to "29/12/22",
                120.0 to "COM",
                160.0 to "Comissão Garantia Bancária",
                400.0 to "39,08",
                520.0 to "7.921,05",
            ),
            line(0, 640, 30.0 to "TOTAL", 400.0 to "46,45", 460.0 to "0,00"),
            line(0, 630, 160.0 to "SALDO CONTABILÍSTICO", 520.0 to "7.921,05"),
            line(0, 600, 30.0 to "NOVO BANCO, S.A., com sede na Av. da Liberdade"),
        )

    val generic =
        listOf(
            line(0, 800, 50.0 to "Some Bank · IBAN: DE89 3704 0044 0532 0130 00"),
            line(0, 790, 50.0 to "Date", 120.0 to "Description", 380.0 to "Debit", 450.0 to "Credit"),
            line(0, 780, 50.0 to "2026-09-02", 120.0 to "Coffee", 380.0 to "3,20"),
            line(0, 770, 50.0 to "2026-09-03", 120.0 to "Salary", 450.0 to "1.500,00"),
            line(0, 760, 50.0 to "2026-09-04", 120.0 to "Refund", 380.0 to "-4,00"),
        )
}
