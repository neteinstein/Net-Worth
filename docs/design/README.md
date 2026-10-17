# Design source — "Net Worth — family finances"

**Canonical source for the product.** Snapshot of the design canvas
<https://claude.ai/artifact/5UT36cAcTMDJc86mtEzQFT> (artifact version `1792185519-b404`, taken 2026-10-16),
committed so sandboxed agents can read it without access to claude.ai.

`canvas.json` lists the artboards and their order. Each `*.dc.html` is one 390 px phone screen: the markup is
the layout and copy, and the `<script type="text/x-dc">` block holds the example data and interactions.

| Artboard | File | Implemented in |
| --- | --- | --- |
| Overview | `Main.dc.html` | `feature/overview` (phase 1) |
| Import extract | `Import.dc.html` | phase 2 (`feature/importer`) |
| Review & learn | `Review.dc.html` | phase 2 (`feature/importer`) |
| Spending by category | `Spending.dc.html` | phase 3 |
| Alerts & insights | `Alerts.dc.html` | phase 3 |
| Future projections | `Future.dc.html` | phase 4 |
| Year ahead forecast | `Forecast.dc.html` | phase 4 |
| Manage | `Manage.dc.html` | `feature/manage` (phase 1) |
| Categories | `Categories.dc.html` | `feature/manage` (phase 1) |
| Filters | `Filters.dc.html` | phase 3 |

Rules when implementing from it:

- Copy layout, copy text, colours, type and the category taxonomy as drawn. The **figures, names and
  bracketed placeholders** (`[Partner]`, `€4,850`, "146 merchants") are example data, never seed data.
- The default categories and subcategories come from `Categories.dc.html`
  (`core/domain/.../seed/DefaultCategories.kt`). Its budgets, seasonal peaks and rule counts are examples
  and are not seeded.
- Screens the design does not cover (Google sign-in, "Start fresh / Open a Google Drive file", Settings)
  follow the same visual language; call them out in the PR that adds them.
