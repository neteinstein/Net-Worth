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

## Several files at once

The picker accepts many files (e.g. a year of monthly extracts). `ImportViewModel` reads them one after the
other: each is confirmed (or imported straight away when "ask which account" is off and every section is
matched), unreadable files are reported by name and skipped, and the summaries are listed at the end. With
"Use the same accounts and owners for the next files" on (the default), the account, owners and left-out
sections chosen for a file are reused for later files of the same account (`accountKey`: IBAN, else account
number, else bank + format + section name), which are then imported without asking.

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
5. **Claude (optional).** When this device has a Claude API key (Settings → Claude), the outflows no rule
   filed are sent to the Anthropic Messages API (`core:claude` → `ClaudeCategorizer`, model `claude-opus-5-5`,
   low effort, structured output constrained to the household's categories) with their description and amount.
   Claude's choices are applied to movements that are still uncategorised (`CategoriseWithClaudeUseCase`);
   movements it isn't sure about are left out, and no rules are learned. A failure (refused key, no credit,
   offline) never undoes the import: the summary card says why, and the movements wait in Review & learn. The
   key is device-local (`LocalClaudeKeyRepository` → `LocalStore`), never in the family database.

Uncategorised outflows wait in **Review & learn** (`ReviewQueue`): suggestions come from past choices for the
same merchant, else from `KnownMerchants` (several hundred Portuguese brands and shop words — Continente,
Mercadona, Galp, Via Verde, EDP, Farmácia… — mapped to the default subcategories), else from the movement kind
(fees, mortgage). "Always file X this way" creates a rule that also files other waiting movements.

Rules are edited in **Manage → Merchant rules** (text, how it matches, category and subcategory). Saving either
changes only future imports or also re-files every past outflow/refund the rule matches (internal transfers
are never touched).

## Merchant rules files

**Manage → Merchant rules → Export** writes the household's rules in this format (`merchant-rules.json`, in
category order) and hands it to the share sheet on Android/iOS or downloads it on the web, so another family —
or another device's household file — can import it.

**Manage → Merchant rules → Import** reads a JSON file of rules, shows what it would do (new rules, rules whose
category changes, rules already there, lines left out) and imports them for future imports only or also re-files
the past, as the rule editor does. Categories are named, not referenced by id, so one file works for any household:

```json
{
  "rules": [
    {"pattern": "LIDL", "match": "PREFIX", "category": "Groceries", "subcategory": "Supermarket"},
    {"pattern": "BOLT.EUO", "match": "CONTAINS", "category": "Transport", "subcategory": "Public transport"},
    {"pattern": "COLEGIO", "category": "School"}
  ]
}
```

- `match` is `PREFIX` (the cleaned merchant name starts with it; the default), `CONTAINS` (anywhere in the bank
  description) or `PHONE` (an MB WAY phone or person), as in `Rules`.
- `category` and `subcategory` are matched by name, ignoring case and ’ vs '. A rule whose category or
  subcategory the household doesn't have is left out and named in the dialog.
- A rule with the same text and match as an existing one changes that rule's category (keeping its hits).

[`docs/merchant-rules/pt-brands.json`](merchant-rules/pt-brands.json) holds 50 rules for national and
international brands seen in novobanco statements (supermarkets, fuel, utilities, Bolt/Uber, bank fees…), in the
default categories. Rules learned from a family's own statements (schools, local shops, people paid by MB WAY)
are personal data: keep them in a local file and import it, never commit them.
