---
name: new-feature-module
description: Scaffold a new feature module (feature/<name>) in the Net Worth KMP app with domain/data/ui layers, Koin wiring, Navigation 3 route, strings and commonTest tests. Use when asked to add a new feature, screen area or feature module.
---

# Add a feature module

Follow AGENTS.md conventions. Replace `<name>` (lowercase, e.g. `budgets`) and `<Name>` (PascalCase).

1. **Register the module** in `settings.gradle.kts` under "Features": `include(":feature:<name>")`.
2. **Build file** `feature/<name>/build.gradle.kts` — copy `feature/settings/build.gradle.kts` exactly
   (`networth.kmp.compose` plugin + `implementation(projects.core.designsystem)`). Never add versions here;
   new libraries go into `gradle/libs.versions.toml` first.
3. **Sources** under `feature/<name>/src/commonMain/kotlin/com/neteinstein/networth/feature/<name>/`:
   - `domain/model/` — plain data classes/enums. No Compose imports.
   - `domain/repository/<Name>Repository.kt` — interface exposing `Flow`s and `suspend` mutations.
   - `domain/usecase/` — one class per action with `operator fun invoke`.
   - `data/` — implementation(s) of the repository.
   - `ui/screens/<Name>UiState.kt`, `<Name>ViewModel.kt` (single `StateFlow<…UiState>` via
     `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`; events are methods),
     `<Name>Screen.kt` containing `<Name>Screen` (koinViewModel + collectAsStateWithLifecycle) and the
     stateless `<Name>Content(state, callbacks…)` plus at least one `@Preview` wrapped in `NetWorthTheme`.
   - `ui/components/` — feature-specific composables.
4. **Strings** in `feature/<name>/src/commonMain/composeResources/values/strings.xml`, prefixed `<name>_`.
   Import them from `com.neteinstein.networth.feature.<name>.generated.resources.*`.
5. **DI** — add bindings to `composeApp/.../di/AppModule.kt` (never a separate module). Use
   `single<Repo> { Impl() }` if the impl constructor has default args; `factoryOf` for use cases;
   `viewModelOf` for ViewModels. Extend `composeApp/src/commonTest/.../AppModuleTest.kt`.
6. **Navigation** — in `composeApp/.../ui/navigation/Route.kt` add `@Serializable data object <Name> : Route`
   (or a data class for arguments), register it in `navSavedStateConfiguration`
   (`AppNavigation.kt`) **and** add an `entry<Route.<Name>> { … }`. Add
   `implementation(projects.feature.<name>)` to `composeApp/build.gradle.kts` and `kover(projects.feature.<name>)`
   to the root `build.gradle.kts`.
7. **Tests** in `feature/<name>/src/commonTest/kotlin/...` with `kotlin.test`: use cases, repository, and
   ViewModel (setMain/resetMain + collect state in `backgroundScope`).
8. **Verify:** `./gradlew :feature:<name>:testDebugUnitTest ktlintCheck :androidApp:lintDebug :koverVerify`
   then `:androidApp:assembleDebug`. Note in the PR test plan what you could not run (e.g. iOS).
9. Commit as `feat(<name>): …`.
