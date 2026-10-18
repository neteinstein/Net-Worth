# The family database file

One JSON file per family, stored in the Drive of whoever chose **Start fresh** (created with
`appProperties.netWorthDatabase = "1"`) and shared with the rest of the family through Drive.

**Start fresh** asks who picks the file's name and place:

- **Choose for me** (default): `Net Worth – <family>.networth.json` in the root of My Drive.
- **Let me choose**: a name the user types (made Drive-safe and given the `.networth.json` ending; left blank,
  the default name is used) and a folder picked with the Google Picker in folder mode, or My Drive. Picking
  a folder through the Picker is what lets the app write into it under the `drive.file` scope.

## Why a JSON file

- The same `kotlinx-serialization` code on Android, iOS and the web (SQLite has no practical Wasm story;
  a Google Sheet can be broken by anyone who edits a cell).
- Small: years of transactions are a few MB, so the whole file can be downloaded and uploaded.
- Drive keeps revisions, which gives the family free backups and undo.
- Records carry ids and timestamps, so two people's edits merge record by record.

## Format

Defined in `core/database/.../schema/SchemaV1.kt` (the on-disk contract) and mapped to the domain models
in `core/domain`. Top level:

```json
{
  "format": "net-worth-family-database",
  "schemaVersion": 1,
  "id": "…", "createdAt": 0, "updatedAt": 0, "updatedBy": "pedro@example.com",
  "settings": { "householdName": "…", "currency": "EUR", "alerts": {…}, "assumptions": {…}, "updatedAt": 0 },
  "members": [], "accounts": [], "balanceSnapshots": [], "scheduledPayments": [],
  "categories": [], "transactions": [], "imports": [], "merchantRules": [], "savedFilters": [],
  "tombstones": { "<deleted record id>": 1760000000000 }
}
```

- Amounts are integers in minor units (cents); transaction amounts are signed (negative = money out).
- Dates are ISO-8601 (`2026-09-30`); timestamps are epoch milliseconds.
- Enum values are stored by name. Unknown values and unknown fields are tolerated, so an older app can still
  open a file written by a newer one of the same schema version.
- A file with a higher `schemaVersion` is refused ("update the app"). Schema changes bump the version and
  add a step to `FamilyDatabaseCodec.migrate`.

## Sync and merge (`DriveFamilyDatabaseRepository`)

1. On start the local copy (`LocalStore`, entry `database-cache.json`) is shown immediately, then synced.
2. Edits apply locally at once and are uploaded 2 s later, so a burst of edits is one upload.
3. Before uploading, the Drive file's `version` is compared with the last one synced. If someone else changed
   it, their copy is downloaded and merged first (`DatabaseMerger`): per record, the newer `updatedAt` wins;
   a tombstone removes a record unless it was edited after the deletion; settings merge as one record.
4. Offline, edits stay in the local copy marked *pending* and upload on the next successful sync.

Drive has no compare-and-swap on content, so two uploads in the same second can still race; the next sync
merges again, which limits the damage to that one round of edits.
