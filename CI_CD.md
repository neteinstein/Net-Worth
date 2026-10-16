# CI/CD — Net Worth

Workflow: [`.github/workflows/ci.yml`](.github/workflows/ci.yml) (runs on every pull request, on pushes to
`main`, and manually). Shared setup (JDK 21, Gradle with caching, Firebase config) is the composite action
[`.github/actions/setup`](.github/actions/setup/action.yml).

## Jobs

All jobs run **in parallel**; none depends on another.

| Job | Runner | What it runs | Artifacts |
| --- | --- | --- | --- |
| `pr-title` | ubuntu | PR title follows Conventional Commits (`amannn/action-semantic-pull-request`) | — |
| `build-android` | ubuntu | `:androidApp:assembleDebug :androidApp:assembleRelease` (release = R8 minify + resource shrink) | APKs, R8 `mapping.txt` |
| `build-web` | ubuntu | `:composeApp:wasmJsBrowserDistribution` | static web bundle |
| `build-ios` | macos | `xcodebuild build` of `iosApp/iosApp.xcodeproj` for the simulator, `CODE_SIGNING_ALLOWED=NO` (the Run Script builds `ComposeApp.framework` through Gradle) | — |
| `unit-tests` | ubuntu | `testDebugUnitTest` — all `commonTest` suites on the JVM | JUnit XML |
| `coverage` | ubuntu | Kover merged report + `:koverVerify` (≥ 80 % line coverage of non-UI code); summary in the job summary | XML + HTML report |
| `lint` | ubuntu | `ktlintCheck` + `:androidApp:lintDebug` (`checkDependencies = true` → covers all modules) | lint/ktlint reports |

Concurrency: a new push to the same PR cancels the in-flight run.

## Secrets

| Secret | Used by | Required? | Notes |
| --- | --- | --- | --- |
| `GOOGLE_SERVICES_JSON` | `build-android` | No | Full contents of the real `google-services.json`. When absent, CI copies the committed placeholder `androidApp/google-services.json.ci` — fine for build validation, not for a shippable build. |
| `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | release build (future) | No | Read as environment variables by `androidApp/build.gradle.kts`. Not wired into CI yet — a release job would decode a base64 keystore secret to a temp file and export these. |
| `GITHUB_TOKEN` | `pr-title` | Automatic | Read-only. |

Platform secrets are **never committed**: `google-services.json`, `GoogleService-Info.plist`, keystores and
provisioning profiles are git-ignored. Only `*.ci` placeholders live in the repo.

## Signing status per platform

| Platform | Status |
| --- | --- |
| Android | Release build is **unsigned** in CI (validates R8). Signing config activates only when the keystore env vars are present. No Play upload yet. |
| iOS | **Unsigned** simulator build (`CODE_SIGNING_ALLOWED=NO`). No certificates/profiles in CI; `TEAM_ID` is empty in `Config.xcconfig` (set locally via `Local.xcconfig`). No TestFlight upload yet. |
| Web | No signing. Bundle uploaded as an artifact; no deployment target configured yet. |

## Not yet in place (deliberately)

- Release/deploy workflows (Play Console internal track, TestFlight, web hosting).
- Instrumented Android tests / iOS UI tests (need emulators/simulators; add when UI stabilises).
- Dependency update automation (Renovate/Dependabot) — bumps follow `.claude/skills/bump-dependencies`.

## Running the same checks locally

```bash
cp androidApp/google-services.json.ci androidApp/google-services.json
./gradlew testDebugUnitTest ktlintCheck :androidApp:lintDebug :koverVerify :androidApp:assembleRelease
```
