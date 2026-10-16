# AGENTS.md — Net Worth

Guidance for AI agents (and humans) working in this repo. Read it fully before changing anything.
CI/CD topology lives in [CI_CD.md](CI_CD.md), not here.

## What this is

**Net Worth** — a Kotlin Multiplatform app for Android, iOS and Web (Wasm) with a single shared
Compose Multiplatform UI. The product scope is still being defined: the dashboard currently renders
**placeholder demo data** (`feature/dashboard/.../data/SampleAccounts.kt`) from an in-memory
repository. Do not treat that data, or the account categories, as product requirements.

## Stack

| Concern | Choice |
| --- | --- |
| Language / UI | Kotlin 2.4, Compose Multiplatform 1.12 (Material 3 `1.9.0`) |
| Architecture | Clean architecture (domain / data / ui) + MVVM, Coroutines + `StateFlow` |
| DI | Koin 4.2 — one shared `di/AppModule.kt` |
| Navigation | Navigation 3 (JetBrains KMP build), type-safe `@Serializable` routes |
| Android | AGP 9.4, compileSdk/targetSdk 37, minSdk 32, R8 full mode on release |
| iOS | `iosArm64` + `iosSimulatorArm64` only (no `iosX64`), static `ComposeApp.framework`, hand-authored Xcode project, iOS 17+ |
| Web | `wasmJs` browser executable (in `composeApp`) |
| Tests | `kotlin.test` in `commonTest`, `kotlinx-coroutines-test`; run on the JVM |
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
core/designsystem/     Theme (light/dark, dynamic colour, finance colours), reusable components.
feature/dashboard/     Net-worth overview: domain / data / ui.
feature/settings/      Appearance settings (theme mode, dynamic colour): domain / data / ui.
iosApp/                Hand-authored Xcode project wrapping ComposeApp.framework (SwiftUI host).
build-logic/           Convention plugins: networth.kmp.library, networth.kmp.compose, networth.android.application.
docs/                  Authoritative source material (see "Source content").
```

Why it is split this way:

- **AGP 9+ forbids `org.jetbrains.kotlin.multiplatform` and `com.android.application` in the same
  module.** So the shared UI/logic is a KMP *library* (`composeApp`) and the installable Android app is
  a thin separate module (`androidApp`).
- **One module per feature** keeps build times and ownership local. A feature depends on
  `core:*` modules, never on another feature. `composeApp` is the only module that sees every feature
  (it wires them into navigation and DI).
- **commonMain-first.** All code defaults to `src/commonMain/kotlin/<package>/`. Use
  `androidMain`/`iosMain`/`wasmJsMain` or `expect`/`actual` only for what a platform genuinely cannot
  share. Current `expect`/`actual`s (all in `core/designsystem/.../ui/theme/Platform.kt`): dynamic colour
  availability/scheme and system-bar icon tint.

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
<feature>/src/commonTest/kotlin/...                             Tests mirror the main package layout.
```

Package root is `com.neteinstein.networth`; a module's namespace is derived from its Gradle path
(`:feature:dashboard` → `com.neteinstein.networth.feature.dashboard`, `:composeApp` → `…networth.composeapp`)
and its generated Compose resources class is `<namespace>.generated.resources.Res`.

## Conventions

### MVVM

- A **screen** composable (`DashboardScreen`) gets its ViewModel via `koinViewModel()`, collects
  `state` with `collectAsStateWithLifecycle()`, and passes state + callbacks down. That is all it does.
- A **content** composable (`DashboardContent`) is stateless: `state` + lambdas in, UI out. It is
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
- `AppModuleTest` resolves the graph; extend it when you add bindings.

### Testing

- All shared tests go in `commonTest` with `kotlin.test`. They run on the JVM through the Android host
  test (`testAndroidHostTest`), aliased as **`testDebugUnitTest`** in every KMP module.
- **New domain/data/ViewModel code arrives with tests.** CI can verify those on every PR without an
  emulator or a Mac.
- ViewModel tests: `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest`, `resetMain()` in
  `@AfterTest`, and collect `state` in `backgroundScope` before asserting (state uses `WhileSubscribed`).
- Coverage excludes composables, theme, navigation and generated code (see root `build.gradle.kts`);
  everything else counts toward the 80 % Kover gate.

### Commits & PRs

- [Conventional Commits](https://www.conventionalcommits.org/): `feat(dashboard): …`, `fix(settings): …`,
  `docs(agents): …`, `build(deps): …`, `ci: …`, `test(dashboard): …`, `refactor(designsystem): …`.
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

## Build & verify

```bash
cp androidApp/google-services.json.ci androidApp/google-services.json   # once; the real file is git-ignored

./gradlew testDebugUnitTest                 # all shared unit tests (JVM)
./gradlew ktlintCheck                       # style (ktlintFormat to auto-fix)
./gradlew :androidApp:lintDebug             # Android lint incl. all library modules
./gradlew :koverLog :koverVerify            # merged coverage + 80 % gate (report: :koverHtmlReport)
./gradlew :androidApp:assembleDebug         # debug APK
./gradlew :androidApp:assembleRelease       # R8-minified release APK (unsigned unless signing env vars are set)
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
  `https://maven-central.storage-download.googleapis.com/maven2/`. Never commit such mirrors.
- **`github.com` / `codeload.github.com` may be blocked**, so `wasmJsBrowserDistribution`/web tests fail
  while downloading Yarn/Binaryen/npm packages. `compileKotlinWasmJs` still works offline-from-GitHub.
- **No macOS / Xcode / simulator / emulator.** iOS code (`iosMain`, Swift, `project.pbxproj`) cannot be
  compiled; nothing can be run on a device.

What *can* usually be checked in a sandbox: `testDebugUnitTest`, `ktlintCheck`, `:androidApp:lintDebug`,
`:koverVerify`, `:androidApp:assembleDebug/assembleRelease`, `compileKotlinWasmJs`.
**CI on the PR is the real build signal** — especially for iOS and the web bundle. Say in the PR's test
plan exactly which of these you ran.

## Source content

There is **no authoritative source document yet** — product scope will be provided later. When it is:

- Commit the source files (spec, spreadsheet, PDF, design export) under `docs/` and record here the
  exact file and sheet/section that is canonical.
- Copy content verbatim; **do not "clean up", paraphrase, reorder or invent** content that is not in the
  source. If something looks wrong, flag it in the PR instead of fixing it silently.
- If an origin link (Google Sheets, Figma, …) is unreachable from a sandboxed agent, note that here and
  rely on the committed snapshot in `docs/`.
- If deriving from it becomes a recurring task, add a skill under `.claude/skills/`.

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
    rules to `androidApp/proguard-rules.pro` and verify with `:androidApp:assembleRelease`.
11. `w: KLIB loader: The same 'unique_name=…' found in more than one library` during metadata compilation
    is upstream noise (JetBrains vs. Google KMP artifacts) — not something to fix here.
12. ktlint ignores generated sources (`**/generated/**`, `build/`); composables are exempt from
    function-naming via `.editorconfig`.
13. Persistence doesn't exist yet: accounts and settings are in-memory and reset on restart.
