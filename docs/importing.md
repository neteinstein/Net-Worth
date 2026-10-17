# Importing bank statements

How `feature/importer` turns a bank file into family data. Everything runs in common Kotlin, so Android,
iOS and the web behave the same and every step is unit-tested on the JVM.

## Formats

| Format | Reader | Notes |
| --- | --- | --- |
| PDF | `parse/pdf/` → `parse/bank/` | Own PDF text extractor: object scanning (no xref needed), object streams, FlateDecode/LZW/ASCIIHex/ASCII85/RunLength, Standard security handler with an empty user password (RC4 and AES-128; AES-256 is rejected with a message), Type 1/TrueType/Type 3/Type 0 fonts, ToUnicode CMaps, and the glyph names bank generators use (`C2c` = hex WinAnsi code, `32` = decimal). Text is grouped into lines and cells by position. |
| XLSX | `parse/Xlsx.kt` | Own ZIP reader + inflate; first worksheet, shared strings, date-formatted cells become dates. |
| OFX/QFX | `parse/Ofx.kt` | 1.x SGML and 2.x XML; one section per bank or card statement; `FITID` is the duplicate key. |
| CSV | `parse/TableStatement.kt` | Delimiter sniffing, UTF-8 or Windows-1252, PT/EN headers in any order, preamble IBAN. |

## PDF bank profiles

`PdfStatements.profiles` picks the first profile whose `matches()` is true, else `GenericPdfProfile`.

- **novobanco "Extrato Integrado"** — one section per `CONTA <name> nº <number> de dd.mm.yyyy a dd.mm.yyyy`;
  `SALDO ANTERIOR` / `SALDO CONTABILÍSTICO`; IBAN on page 1; "Agenda de movimentos previstos" → scheduled
  payments; "Crédito Habitação" detail → mortgage balance.
- **Banco Best "Extrato Integrado"** — sections from the `N.º Conta | Designação | Data Início | Data Fim` line
  under "Detalhe de movimentos"; `Saldo inicial` / `Saldo contabilístico`; NIB → IBAN; investment funds from
  "Detalhe dos ativos".
- **Generic** — rows that start with a date and end with amounts; first IBAN in the file.

All profiles share `MovementTable`: a row starts with a date, its trailing amount cells are the movement and
running balance, wrapped description lines are joined by column position, and the **sign comes from the
change in balance** (or the Débito/Crédito column when there is no balance). A section whose movements don't
add up from the opening to the closing balance is reported after the import.

To support another bank: add a profile with synthetic fixture lines in `StatementProfilesTest` that copy the
real layout with invented names, numbers and amounts. **Never commit a real statement.**

## What an import does (`ImportPlanner`)

1. Each section goes to an existing account (pre-selected by IBAN/account number via `AccountMatcher`) or a new
   one. The chosen **owners** (any number of household members, added inline if needed) replace the account's
   owners.
2. Rows already imported are skipped by fingerprint (account, date, amount, running balance, description; OFX
   uses `FITID`), so overlapping weekly and monthly extracts never count twice.
3. Merchant rules file what they can; opposite movements between the family's own accounts within 3 days (or a
   transfer to a household member named in full) become internal transfers.
4. Opening/closing balances, mortgage/fund balances and scheduled payments are stored; one `ImportRecord` per
   account records the period, which drives the **months uploaded** grid (`Coverage`: full / partly / missing
   for the last 12 months).

Uncategorised outflows wait in **Review & learn** (`ReviewQueue`): suggestions come from past choices for the
same merchant; "Always file X this way" creates a rule that also files other waiting movements.
