# AGENTS.md — Net Worth

Guidance for AI agents (and humans) working in this repo. Read it fully before changing anything.
CI/CD topology lives in [CI_CD.md](CI_CD.md), not here.

## What this is

**Net Worth** — a family-finance app (Kotlin Multiplatform: Android, iOS and Web/Wasm, one shared Compose
Multiplatform UI). A household (e.g. 2 adults, 3 kids) imports bank extracts, sees where the money went, gets
spike alerts and projections. The scope is the design canvas snapshotted in [`docs/design/`](docs/design/README.md).

All family data lives in **one JSON file in Google Drive** that the family shares
([`docs/database.md`](docs/database.md)). First launch: sign in with Google, then **Start fresh** (create the
file) or **Open a Google Drive file** (Google Picker). Setup of the Google Cloud project:
[`docs/google-cloud-setup.md`](docs/google-cloud-setup.md).

Delivery is phased: phase 1 = Drive database + sync, onboarding, Overview, Manage, Categories, Settings.
Phase 2 = Import + Review & learn (CSV/OFX/XLSX/PDF incl. novobanco, Banco Best and ActivoBank statements, account owners,
months-uploaded coverage; see [`docs/importing.md`](docs/importing.md)). Phase 3 = Spending, Filters (+ a transaction
list to re-file and tag movements) and Alerts, with the artboards' bottom bar. Phase 4 = Future + Year ahead
(projections, what-ifs, emergency fund, 12-month forecast, 5-year table). All 10 artboards are now implemented.
Since then: screenshot tests, editable projection assumptions, and budgets vs spending (Spending rows, Alerts).

## Stack

| Concern | Choice |
| --- | --- |
| Language / UI | Kotlin 2.4, Compose Multiplatform 1.12 (Material 3 `1.9.0`) |
| Architecture | Clean architecture (domain / data / ui) + MVVM, Coroutines + `StateFlow` |
| DI | Koin 4.2 — one shared `di/AppModule.kt` |
| Data | One JSON file in Google Drive (`drive.file` scope) + local cache; Ktor 3 client; kotlinx-serialization-json; kotlinx-datetime |
| Sign-in | Android: Credential Manager + Identity AuthorizationClient; iOS: OAuth PKCE via ASWebAuthenticationSession; Web: Google Identity Services + Picker |
| Navigation | Navigation 3 (JetBrains KMP build), type-safe `@Serializable` routes |
| Android | AGP 9.4, compileSdk/targetSdk 37, minSdk 32, R8 full mode on release |
| iOS | `iosArm64` + `iosSimulatorArm64` only (no `iosX64`), static `ComposeApp.framework`, hand-authored Xcode project, iOS 17+ |
| Web | `wasmJs` browser executable (in `composeApp`) |
| Tests | `kotlin.test` in `commonTest`, `kotlinx-coroutines-test`; run on the JVM; screenshot tests with Roborazzi + Robolectric |
| Quality | ktlint (`.editorconfig`), Android lint (`checkDependencies = true`), Kover coverage (≥ 80 % gate) |
| Build | Gradle 9.8 Kotlin DSL, convention plugins in `build-logic/`, versions only in `gradle/libs.versions.toml` |

## Module layout (and why)

```
androidApp/            com.android.application — manifest, MainActivity, Application (Koin + Firebase init),
                       resources, R8 rules. NO shared code here.
composeApp/            KMP library (com.android.kotlin.multiplatform.library) — the app shell:
                       App(), navigation, di/AppModule.kt + di/InitKoin.kt, AppViewModel,
                       iosMain/MainViewController.kt, wasmJsMain/Main.kt (web entry point + index.html).
                       Produces the iOS framework `ComposeApp` and the web bundle.
core/domain/           Pure Kotlin: FamilyDatabase models, Money/MoneyFormatter/MoneyParser, CalendarMonth,
                       calculators (totals, averages, spikes, balances, projections), bank description
                       normaliser (text/Descriptions: merchant keys, movement kinds), repository interfaces
                       (FamilyDatabaseRepository, AuthRepository, LocalStore, AppClock), shared use cases, seeds.
core/database/         The Drive-backed FamilyDatabaseRepository: JSON schema (schema/SchemaV1.kt), codec,
                       record-level merger, local cache; expect/actual LocalStore (files / localStorage).
core/claude/           Optional Claude auto-categorisation of imports: ClaudeCategorizer (Messages API over Ktor, with the
                       user's own key) and the device-local LocalClaudeKeyRepository.
core/google/           Google sign-in per platform (GoogleAuthPlatform actuals), DriveClient (Ktor), OAuth/PKCE
                       helpers, generated GoogleConfig (client ids from local.properties / env).
core/designsystem/     Theme from the design (ink/blue palette, Bricolage Grotesque + Instrument Sans + IBM Plex
                       Mono, finance colours), components (HeroCard, SectionCard, chips, charts…), line icons.
core/testing/          Fakes for feature tests (FakeDatabaseRepository, FakeAuthRepository, designFamily()) and
                       the screenshot() helper (androidMain). commonTest dependency only.
feature/onboarding/    Sign in → Start fresh / Open a Google Drive file.
feature/overview/      Overview artboard.
feature/importer/      Import extract + Review & learn artboards: pure-Kotlin statement parsers (PDF incl. RC4/AES,
                       XLSX, OFX, CSV) with bank profiles, the import planner (dedupe, transfers, rules, snapshots,
                       owners), month coverage, the review queue, and a file picker per platform.
feature/spending/      Spending + Filters artboards and the transaction list (re-file, tag for a person, mark as
                       transfer). The filter being edited lives in an in-memory ActiveFilterRepository.
feature/alerts/        Alerts artboard: spikes (subcategories, accounts, last year, seasonal toggle), new recurring
                       payments, 3-month rises, large payments, and what went well.
feature/future/        Future + Year ahead artboards: savings projections (all / joint / kids, 2–10 years, with and
                       without return), what-ifs, emergency fund, 10-year spend; 12-month forecast from last year's
                       months × price rise, expected peaks, and a 5-year table from the household's assumptions.
feature/manage/        Manage + Categories artboards (accounts, rules, alert rules, projection assumptions, categories
                       and their monthly budgets).
feature/settings/      Appearance (device-local), database file, sharing, household members, sign out.
iosApp/                Hand-authored Xcode project wrapping ComposeApp.framework (SwiftUI host).
build-logic/           Convention plugins: networth.kmp.library, networth.kmp.compose, networth.android.application.
docs/                  Authoritative source material (see "Source content").
```

Why it is split this way:

- **AGP 9+ forbids `org.jetbrains.kotlin.multiplatform` and `com.android.application` in the same
  module.** So the shared UI/logic is a KMP *library* (`composeApp`) and the installable Android app is
  a thin separate module (`androidApp`).
- **One module per feature** keeps build times and ownership local. A feature depends on
  `core:designsystem` + `core:domain` (never on `core:database`/`core:google`/`core:claude`, never on another feature). `composeApp` is the only module that sees every feature
  (it wires them into navigation and DI).
- **commonMain-first.** All code defaults to `src/commonMain/kotlin/<package>/`. Use
  `androidMain`/`iosMain`/`wasmJsMain` or `expect`/`actual` only for what a platform genuinely cannot
  share. Current `expect`/`actual`s: dynamic colour availability/scheme and system-bar icon tint
  (`core/designsystem/.../ui/theme/Platform.kt`), `Scope.platformLocalStore()` (`core/database`),
  `Scope.googleAuthPlatform()` (`core/google`), `rememberFilePicker()` / `rememberFileSharer()`
  (`core/designsystem/.../ui/files`: statements for the importer, rules files for Manage; Android shares through
  the library's `FileProvider`) and `LocalAppLocale` (`core/designsystem/.../ui/locale`, the in-app language
  switch).

Inside each feature module:

```
<feature>/src/commonMain/kotlin/com/neteinstein/networth/feature/<name>/
  domain/model/        Plain Kotlin models. NO Compose types, ever (no Color, no @Composable, no Painter).
  domain/repository/   Repository interfaces.
  domain/usecase/      One use case per action/stream, invoked via `operator fun invoke`.
  data/                Repository implementations + data sources.
  ui/screens/          <Name>Screen (pulls VM), <Name>Content (stateless), <Name>ViewModel, <Name>UiState.
  ui/components/       Feature-specific composables.
<feature>/src/commonMain/composeResources/values/strings.xml   All user-facing strings.
<feature>/src/commonMain/composeResources/values-pt/strings.xml  Their Portuguese (pt-PT) translation.
<feature>/src/commonTest/kotlin/...                             Tests mirror the main package layout.
```

Package root is `com.neteinstein.networth`; a module's namespace is derived from its Gradle path
(`:feature:overview` → `com.neteinstein.networth.feature.overview`, `:composeApp` → `…networth.composeapp`)
and its generated Compose resources class is `<namespace>.generated.resources.Res`.

## Conventions

### MVVM

- A **screen** composable (`OverviewScreen`) gets its ViewModel via `koinViewModel()`, collects
  `state` with `collectAsStateWithLifecycle()`, and passes state + callbacks down. That is all it does.
- A **content** composable (`OverviewContent`) is stateless: `state` + lambdas in, UI out. It is
  what `@Preview`s and future UI tests target. Add a preview for every content composable.
- ViewModels expose **one immutable `StateFlow<XUiState>`** (a `data class`); UI events are plain
  method calls (`onThemeModeSelected(mode)`). Use `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`.
- **No business logic in composables** — formatting helpers that are pure functions (e.g.
  `MoneyFormatter`) live outside composables and are unit-tested.
- ViewModels talk to **use cases**, use cases talk to **repository interfaces**.

### Koin

- Every binding lives in `composeApp/src/commonMain/kotlin/com/neteinstein/networth/di/AppModule.kt`.
  Do not create per-platform or per-feature modules.
- The bootstrap is separate: `di/InitKoin.kt` (`initKoin(config)` + a no-arg overload for Swift).
  Callers: `NetWorthApplication` (Android), `iOSApp.init()` (iOS), `Main.kt` (web).
- Prefer `factoryOf(::UseCase)` / `viewModelOf(::ViewModel)`. **Do not use `singleOf(::X)` when `X`'s
  constructor has default arguments** — Koin will try to inject them. Use `single<Interface> { X() }`.
- Platform-touching bindings (`LocalStore`, `GoogleAuthPlatform`) sit in `platformBindings` (same file,
  included by `appModule`) so `AppModuleTest` can override them with fakes on the JVM.
- `AppModuleTest` resolves the graph; extend it when you add bindings.

### Testing

- All shared tests go in `commonTest` with `kotlin.test`. They run on the JVM through the Android host
  test (`testAndroidHostTest`), aliased as **`testDebugUnitTest`** in every KMP module.
- **New domain/data/ViewModel code arrives with tests.** CI can verify those on every PR without an
  emulator or a Mac.
- ViewModel tests: `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest`, `resetMain()` in
  `@AfterTest`, and collect `state` in `backgroundScope` before asserting (state uses `WhileSubscribed`).
- Feature tests use `core:testing` (`FakeDatabaseRepository.ready(designFamily())`, `FakeAuthRepository`).
- Code that launches work into an injected `CoroutineScope` (e.g. the debounced Drive upload) is tested
  with the `TestScope` itself as that scope, so `advanceUntilIdle()` drives it.
- Coverage excludes composables, theme, navigation and generated code (see root `build.gradle.kts`);
  everything else counts toward the 80 % Kover gate.
- **Screenshot tests.** Every content composable is rendered on the JVM (Roborazzi + Robolectric native
  graphics) in light and dark: `<feature>/src/androidHostTest/.../ScreenshotTest.kt` calls
  `core:testing`'s `screenshot(name) { …Content(preview state) }`. `testDebugUnitTest` compares them with
  the committed PNGs in `src/androidHostTest/screenshots/` (1 % tolerance). After an intended UI change run
  `./gradlew testDebugUnitTest -PrecordScreenshots`, **look at the new PNGs** and commit them with the
  change. Goldens are recorded on Linux (as in CI). A new screen gets a screenshot test with its previews.

### Commits & PRs

- [Conventional Commits](https://www.conventionalcommits.org/): `feat(overview): …`, `fix(settings): …`,
  `docs(agents): …`, `build(deps): …`, `ci: …`, `test(overview): …`, `refactor(designsystem): …`.
  Scope = module or area. The PR title must follow the same format (checked in CI).
- Fill in `.github/pull_request_template.md`. The **Test plan must say what you could not run and why**.

### UI / platform guidelines

- Edge-to-edge everywhere: Android `enableEdgeToEdge()`, iOS `.ignoresSafeArea()`; insets are handled
  by `Scaffold`/`WindowInsets` in Compose. Never hard-code status-bar padding.
- Theme: `NetWorthTheme` (light/dark/system + Android 12+ dynamic colour, animated transitions).
  Use `MaterialTheme.colorScheme` / `NetWorthTheme.financeColors` — never raw colours in features.
- Android: SplashScreen API, adaptive + themed (monochrome) icon, predictive back enabled, backups
  disabled for financial data. iOS: privacy manifest (`PrivacyInfo.xcprivacy`), 120 Hz enabled.
- Constrain wide layouts (`widthIn(max = 640.dp)`) so tablets/desktop browsers look intentional.

### Languages

- The app ships in **English** (`values/`) and **European Portuguese** (`values-pt/`). **Every feature supports
  both languages:** every string added to or changed in a `values/strings.xml` gets its translation in the same
  module's `values-pt/strings.xml` in the same change (same names, same placeholders, `one`/`other` plurals).
  A new module with strings starts with both files. This is enforced: `./gradlew verifyTranslations` (run by
  CI's lint job) fails on a missing or extra key, a placeholder mismatch or a missing plural form.
- Write pt-PT, not pt-BR ("ficheiro", "ecrã", "telemóvel", "guardar"; formal "você" forms as elsewhere in the
  app), and keep product names (MB WAY, Google Drive, Net Worth) untranslated.
- A new screen's screenshot test should also render it in Portuguese when its layout could break with longer
  text (see `settings_pt` in `feature/settings`).
- The language is a device-local setting (Settings → Language: System / English / Português), stored with the
  appearance settings and applied by `AppLocaleEnvironment` in `App()`. No user-facing text in Kotlin code:
  domain errors carry an enum (e.g. `ParseFailure`) that the UI maps to a string resource.
- Money is still formatted locale-independently (`MoneyFormatter`); family data (category names, account
  names) is stored as typed and is not translated.

## Build & verify

```bash
cp androidApp/google-services.json.ci androidApp/google-services.json   # once; the real file is git-ignored

./gradlew testDebugUnitTest                 # all shared unit tests (JVM)
./gradlew ktlintCheck                       # style (ktlintFormat to auto-fix)
./gradlew verifyTranslations                # every values/strings.xml matched by values-pt/strings.xml
./gradlew :androidApp:lintGithubDebug       # Android lint incl. all library modules
./gradlew :koverLog :koverVerify            # merged coverage + 80 % gate (report: :koverHtmlReport)
./gradlew :androidApp:assembleGithubDebug   # debug APK (flavor "github" — see CI_CD.md → Flavors)
./gradlew :androidApp:assembleGithubRelease # R8-minified release APK (debug-signed unless KEYSTORE_* env vars are set)
./gradlew :androidApp:bundlePlaystoreRelease # Play Store app bundle (CI uploads it to the internal track — CI_CD.md)
./scripts/verify-obfuscation.sh             # asserts R8 obfuscated the app and kept reflective names
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # web dev server
./gradlew :composeApp:wasmJsBrowserDistribution     # web bundle → composeApp/build/dist/wasmJs/productionExecutable
open iosApp/iosApp.xcodeproj                # iOS (macOS + Xcode only); the Run Script builds the framework
```

Fast compile-only checks: `./gradlew compileAndroidMain compileKotlinWasmJs` (iOS compilation needs macOS).

### Cloud / sandboxed agents

Agent containers frequently **cannot build the whole project**. Known limits:

- **No Android SDK preinstalled.** It can be installed from `dl.google.com` with `sdkmanager`
  (`platforms;android-37.0`, `build-tools;37.0.0`), then `sdk.dir=…` in `local.properties`.
- **Maven Central may rate-limit** (`429` from `repo.maven.apache.org`). Workaround used successfully:
  a *local, uncommitted* Gradle init script in `~/.gradle/init.d/` that rewrites that URL to
  `https://maven-central.storage-download.googleapis.com/maven2/`. Never commit such mirrors. Robolectric
  downloads its `android-all` jars by itself: point it at the same mirror from that script with
  `tasks.withType<Test>().configureEach { systemProperty("robolectric.dependency.repo.url", mirror) }`.
- **`github.com` / `codeload.github.com` may be blocked**, so `wasmJsBrowserDistribution`/web tests fail
  while downloading Yarn/Binaryen/npm packages. `compileKotlinWasmJs` still works offline-from-GitHub.
- **No macOS / Xcode / simulator / emulator.** Nothing can be run on a device, and Swift/`project.pbxproj`
  can't be built. Kotlin `iosMain` code **can be type-checked** on Linux with klib cross-compilation:
  `./gradlew compileKotlinIosSimulatorArm64 -Pkotlin.native.enableKlibsCrossCompilation=true` (downloads the
  Kotlin/Native toolchain once; linking the framework still needs macOS).

What *can* usually be checked in a sandbox: `testDebugUnitTest`, `ktlintCheck`, `verifyTranslations`, `:androidApp:lintGithubDebug`,
`:koverVerify`, `:androidApp:assembleGithubDebug/assembleGithubRelease`, `compileKotlinWasmJs`, and the iOS klib
cross-compile above. Real Google sign-in/Drive calls can't be exercised (no OAuth client ids, no device).
**CI on the PR is the real build signal** — especially for iOS and the web bundle. Say in the PR's test
plan exactly which of these you ran.

## Source content

- **Product/UI:** [`docs/design/`](docs/design/README.md) — snapshot of the claude.ai design canvas
  "Net Worth — family finances" (10 artboards + `canvas.json`). Canonical for layout, copy, palette, type and
  the default category taxonomy. Its figures and names are **example data**, never seed data.
- **Bank statement samples** (for the importer) are real personal documents: **never commit them**.
  Build synthetic fixtures with the same layout and invented values (see `StatementProfilesTest`).
- Copy verbatim; **do not "clean up", paraphrase, reorder or invent** content that is not in the source. If
  something looks wrong, flag it in the PR instead of fixing it silently. Screens the design doesn't cover
  (sign-in, Start fresh, Settings) reuse its visual language and are called out in the PR.
- If an origin link is unreachable from a sandboxed agent, rely on the committed snapshot.

## Gotchas

1. **AGP 9 + KMP:** library modules use `com.android.kotlin.multiplatform.library` configured via
   `kotlin { android { … } }` (the old `androidLibrary { }` block is deprecated). There is no
   `testDebugUnitTest` natively — the convention plugin registers it as an alias of `testAndroidHostTest`.
2. **AGP 9 has built-in Kotlin.** Do not apply `org.jetbrains.kotlin.android` to `androidApp`.
3. **Swift name for `initKoin()` is `InitKoinKt.doInitKoin()`** — Kotlin/Native prefixes top-level
   functions starting with `init` with `do`. Keep the no-arg overload; Swift can't use default args.
   `MainViewController()` is `MainViewControllerKt.MainViewController()`.
4. **The Xcode project is hand-authored** (`iosApp/iosApp.xcodeproj/project.pbxproj`, objectVersion 56,
   classic groups). No CocoaPods, no xcodegen, don't regenerate it from Xcode. Adding a Swift file means
   adding a `PBXFileReference`, a `PBXBuildFile`, the group child and the Sources-phase entry, with new
   unique 24-hex IDs. The "Compile Kotlin Framework" Run Script calls
   `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`; it needs `ENABLE_USER_SCRIPT_SANDBOXING = NO`.
   Simulator `x86_64` is excluded because there is no `iosX64` target. Team ID goes in the git-ignored
   `iosApp/Configuration/Local.xcconfig`.
5. **Version catalog is the only place for versions.** No inline versions in any build file (including
   `build-logic`). Before bumping, check KMP/CMP compatibility (see `.claude/skills/bump-dependencies`).
   The CMP `compose.*` accessors are deprecated — use catalog coordinates. Material 3 for CMP is versioned
   separately (`composeMaterial3`); `material-icons-core` is frozen at 1.7.3 upstream.
6. **Navigation 3 routes** must be `@Serializable`, implement `Route : NavKey`, *and* be registered in
   `navSavedStateConfiguration` — iOS/web have no reflection and will crash restoring the back stack
   otherwise. ViewModels are currently scoped to the host, not per entry
   (`lifecycle-viewmodel-navigation3` is not stable yet).
7. **Compose resources are per module** (`<namespace>.generated.resources.Res`). Android packaging needs
   `androidResources.enable = true`, which `networth.kmp.compose` sets.
8. **`RepositoriesMode.FAIL_ON_PROJECT_REPOS`** clashes with Kotlin/Wasm's Node/Yarn/Binaryen downloads;
   those repositories are declared in `settings.gradle.kts` and KGP's own are disabled in the root build.
9. **`google-services.json` is git-ignored.** Copy `androidApp/google-services.json.ci` for local builds;
   CI does the same, or writes the `GOOGLE_SERVICES_JSON` secret. Never commit real Firebase configs or
   `GoogleService-Info.plist`.
10. **R8:** release is minified + resource-shrunk. If you add reflection/serialization-by-name, add keep
    rules to `androidApp/proguard-rules.pro`, add the kept class to `scripts/verify-obfuscation.sh`, and
    verify with `:androidApp:assembleGithubRelease :androidApp:bundlePlaystoreRelease && ./scripts/verify-obfuscation.sh`
    (CI runs both).
    Releases (signing, versioning, Play upload) are described in [CI_CD.md](CI_CD.md).
11. `w: KLIB loader: The same 'unique_name=…' found in more than one library` during metadata compilation
    is upstream noise (JetBrains vs. Google KMP artifacts) — not something to fix here.
12. ktlint ignores generated sources (`**/generated/**`, `build/`); composables are exempt from
    function-naming via `.editorconfig`.
13. **Persistence:** family data = the Drive JSON file + its local cache (`core:database`). Appearance is
    device-local (`LocalSettingsRepository` → `LocalStore`), and so is the Claude API key
    (`LocalClaudeKeyRepository`): a personal secret never goes into the shared family file. Never store family
    data anywhere else.
14. **Schema changes** to `core/database/.../schema/SchemaV1.kt` are a file-format change shared by every
    device: add fields with defaults; renames/removals need a `SCHEMA_VERSION` bump and a migration in
    `FamilyDatabaseCodec`. Every syncable record needs `id` + `updatedAt`; deletions go through
    `withTombstone()` or the merge brings them back.
15. **Google OAuth client ids** come from `local.properties`/env into the generated `GoogleConfig`
    (`:core:google:generateGoogleConfig`). Empty ids are valid builds: sign-in then reports "not configured".
    Only ever request `drive.file` (non-sensitive); the Picker flow on mobile requires it alone.
16. Fonts in `core/designsystem/src/commonMain/composeResources/font/` are OFL (see
    `core/designsystem/FONTS-LICENSE.txt`); keep that notice when adding or replacing fonts.
