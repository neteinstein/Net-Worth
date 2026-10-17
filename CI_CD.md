# CI/CD — Net Worth

Modelled on [neteinstein/FamilyMoments](https://github.com/neteinstein/FamilyMoments): PR checks on every
pull request, a signed release on every push to `main`, the web app on GitHub Pages, and weekly Dependabot.
Shared setup (JDK 21, Gradle with caching, Firebase config) is the composite action
[`.github/actions/setup`](.github/actions/setup/action.yml).

| Workflow | Trigger | Purpose |
| --- | --- | --- |
| [`pr.yml`](.github/workflows/pr.yml) | pull requests to `main`, manual | Every check a PR must pass |
| [`release.yml`](.github/workflows/release.yml) | push to `main`, manual | Signed APK + AAB → GitHub Release; optional Play Console upload |
| [`deploy-pages.yml`](.github/workflows/deploy-pages.yml) | push to `main`, manual | Wasm web app → GitHub Pages |
| [`dependabot.yml`](.github/dependabot.yml) | weekly | Gradle and GitHub Actions update PRs |

## PR Checks (`pr.yml`)

All jobs run in parallel; a new push to the same PR cancels the in-flight run.

| Job | Runner | What it runs | Artifacts |
| --- | --- | --- | --- |
| `pr-title` | ubuntu | PR title follows Conventional Commits (it becomes the squash commit) | — |
| `lint` | ubuntu | `ktlintCheck` + `:androidApp:lintDebug` (`checkDependencies = true` → every module) | lint/ktlint reports |
| `compile` | ubuntu | `:androidApp:assembleDebug` | debug APK |
| `minified-release` | ubuntu | `:androidApp:assembleRelease` (R8 full mode, resource shrinking) + [`scripts/verify-obfuscation.sh`](scripts/verify-obfuscation.sh) | R8 `mapping.txt` |
| `unit-tests` | ubuntu | `testDebugUnitTest` (all `commonTest` suites on the JVM); results published as a check + PR comment | JUnit XML |
| `coverage` | ubuntu | Kover merged report + `:koverVerify` (≥ 80 % of non-UI code); summary in the job summary; uploaded to Codecov (never fails the build) | XML + HTML report |
| `compile-web` | ubuntu | `:composeApp:wasmJsBrowserDistribution` | web bundle |
| `compile-ios` | macos | `xcodebuild` of `iosApp` for the simulator, unsigned (its Run Script builds `ComposeApp.framework` through Gradle) | — |

`verify-obfuscation.sh` reads R8's mapping and fails if no `com.neteinstein.networth.*` class was renamed
(obfuscation silently switched off, or an over-broad `-keep`) or if a reflectively-loaded name lost its
identity (Credential Manager's Play Services provider, the database schema's `$$serializer`s). Add an entry
there whenever you add a keep rule for something resolved by name.

## Releases (`release.yml`)

On every push to `main` (or a manual run):

1. Fails fast if the signing secrets are missing.
2. Re-runs `ktlintCheck` and `testDebugUnitTest` on the exact merged commit.
3. Decodes the keystore and builds `:androidApp:assembleRelease :androidApp:bundleRelease`, signed, with
   `versionCode = run number` and `versionName = 1.0.<run number>` (`APP_VERSION_CODE` / `APP_VERSION_NAME`).
4. Re-runs `verify-obfuscation.sh`.
5. Creates a GitHub Release `v1.0.<run number>` with the APK, the AAB, R8's `mapping.txt` and their SHA-1s.
6. If `ANDROID_PUBLISHER_CREDENTIALS` is set, uploads the AAB to the Play Console with
   [Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher) (`publishReleaseBundle`) to the
   `internal` track, or to the track named by the `PLAY_TRACK` repo **variable**. Without the secret the step
   is skipped with a notice.

Locally `assembleRelease` falls back to debug signing, so it always works without secrets.

**First Play release:** the Play Developer API can only publish *updates*. Upload the first `.aab` (attached
to every GitHub Release) by hand under Play Console → your app → Testing/Production → Create release; the
workflow handles every release after that.

**Troubleshooting a Play 403:** `PERMISSION_DENIED` on `…/applications/com.neteinstein.networth/edits` is the
service account's standing, not this repo. Check that (1) the service account (`client_email` in the JSON) is
invited under Play Console → Users and permissions with access to this app and release permission for the
track; (2) a first release was uploaded by hand; (3) the Google Play Android Developer API is enabled in the
key's Cloud project; (4) new permissions can take a few hours to propagate — re-run later.

Mapping files: R8 produces a different one per build, so each release keeps its own. Run a trace through
`retrace <mapping.txt> <trace.txt>`. The AAB embeds its mapping, so the Play Console de-obfuscates by itself.

## Web (`deploy-pages.yml`)

Builds the Wasm production bundle and deploys it with `actions/deploy-pages`. One-time setup: repo
Settings → Pages → Source = **GitHub Actions**. For Google sign-in on the deployed site, add its origin
(`https://<owner>.github.io`) to the Web OAuth client's authorised JavaScript origins.

## Secrets and variables

| Name | Kind | Used by | Required? | Notes |
| --- | --- | --- | --- | --- |
| `KEYSTORE_BASE64` | secret | release | **Yes** | `base64 -w0 release.jks` of the upload keystore |
| `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | secret | release | **Yes** | Read by `androidApp/build.gradle.kts` (with `KEYSTORE_FILE`, set by the workflow) |
| `ANDROID_PUBLISHER_CREDENTIALS` | secret | release | No | Raw JSON key of a Play Console service account; enables the Play upload |
| `PLAY_TRACK` | variable | release | No | Play track (default `internal`) |
| `GOOGLE_SERVICES_JSON` | secret | release (and setup action) | No | Real `google-services.json`; otherwise the committed `androidApp/google-services.json.ci` placeholder is used |
| `NETWORTH_GOOGLE_WEB_CLIENT_ID`, `NETWORTH_GOOGLE_IOS_CLIENT_ID`, `NETWORTH_GOOGLE_API_KEY`, `NETWORTH_GOOGLE_PROJECT_NUMBER` | secret | release, deploy-pages | No | Google OAuth client ids / Picker key compiled into `GoogleConfig` (`docs/google-cloud-setup.md`). Without them builds succeed and sign-in reports "not configured" |
| `CODECOV_TOKEN` | secret | pr (coverage) | No | Only needed if the repo is private |
| `GITHUB_TOKEN` | automatic | all | — | `contents: write` only in release (to create the Release) |

Never committed: `google-services.json`, `GoogleService-Info.plist`, keystores, provisioning profiles.
Only `*.ci` placeholders live in the repo.

## Signing status per platform

| Platform | Status |
| --- | --- |
| Android | PRs: debug-signed minified release (R8 validation only). `main`: signed with the release keystore → GitHub Release; Play Console upload when credentials exist |
| iOS | Unsigned simulator build on PRs. No certificates/profiles in CI and no TestFlight upload yet |
| Web | No signing; deployed to GitHub Pages from `main` |

## Running the same checks locally

```bash
cp androidApp/google-services.json.ci androidApp/google-services.json
./gradlew ktlintCheck :androidApp:lintDebug testDebugUnitTest :koverVerify :androidApp:assembleRelease
./scripts/verify-obfuscation.sh
```
