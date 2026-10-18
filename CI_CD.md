# CI/CD — Net Worth

Modelled on [neteinstein/FamilyMoments](https://github.com/neteinstein/FamilyMoments): PR checks on every
pull request, a signed release on every push to `main`, the web app on GitHub Pages, and weekly Dependabot.
Shared setup (JDK 21, Gradle with caching, Firebase config) is the composite action
[`.github/actions/setup`](.github/actions/setup/action.yml).

| Workflow | Trigger | Purpose |
| --- | --- | --- |
| [`pr.yml`](.github/workflows/pr.yml) | pull requests to `main`, manual | Every check a PR must pass |
| [`release.yml`](.github/workflows/release.yml) | push to `main`, manual | Signed `github`-flavor APK → GitHub Release (**disabled for now**, see Releases) |
| [`deploy-pages.yml`](.github/workflows/deploy-pages.yml) | push to `main`, manual | Wasm web app → GitHub Pages |
| [`dependabot.yml`](.github/dependabot.yml) | weekly | Gradle and GitHub Actions update PRs |

## PR Checks (`pr.yml`)

All jobs run in parallel; a new push to the same PR cancels the in-flight run.

| Job | Runner | What it runs | Artifacts |
| --- | --- | --- | --- |
| `pr-title` | ubuntu | PR title follows Conventional Commits (it becomes the squash commit) | — |
| `lint` | ubuntu | `ktlintCheck` + `verifyTranslations` + `:androidApp:lintGithubDebug` (`checkDependencies = true` → every module) | lint/ktlint reports |
| `compile` | ubuntu | `:androidApp:assembleGithubDebug` | debug APK |
| `minified-release` | ubuntu | `:androidApp:assembleGithubRelease` + `:androidApp:bundlePlaystoreRelease` (R8 full mode, resource shrinking) + [`scripts/verify-obfuscation.sh`](scripts/verify-obfuscation.sh) | R8 `mapping.txt` per flavor |
| `unit-tests` | ubuntu | `testDebugUnitTest` (all `commonTest` suites on the JVM, plus the screenshot tests compared with the committed PNGs); results published as a check + PR comment | JUnit XML; screenshot diffs on failure |
| `coverage` | ubuntu | Kover merged report + `:koverVerify` (≥ 80 % of non-UI code); summary in the job summary; uploaded to Codecov (never fails the build) | XML + HTML report |
| `compile-web` | ubuntu | `:composeApp:wasmJsBrowserDistribution` | web bundle |
| `compile-ios` | macos | `xcodebuild` of `iosApp` for the simulator, unsigned (its Run Script builds `ComposeApp.framework` through Gradle) | — |

`verify-obfuscation.sh` reads R8's mapping and fails if no `com.neteinstein.networth.*` class was renamed
(obfuscation silently switched off, or an over-broad `-keep`) or if a reflectively-loaded name lost its
identity (Credential Manager's Play Services provider, the database schema's `$$serializer`s). Add an entry
there whenever you add a keep rule for something resolved by name.

## Flavors

`androidApp` has one flavor dimension, `distribution`:

| Flavor | Distributed via | Task |
| --- | --- | --- |
| `github` | GitHub Releases (APK, sideloaded) | `:androidApp:assembleGithubRelease` |
| `playstore` | Google Play (app bundle, internal track) | `:androidApp:bundlePlaystoreRelease` → `:androidApp:publishPlaystoreReleaseBundle` |

Flavored task names are `assembleGithubDebug`/`assembleGithubRelease`/`lintGithubDebug` (and the `Playstore`
equivalents); the aggregate `assembleDebug`/`assembleRelease` still work. `testDebugUnitTest` (the KMP
modules' alias) is unaffected: `androidApp` has no unit tests.

Gradle Play Publisher is applied to `androidApp` but **disabled by default** and enabled only for `playstore`
(`playConfigs` in `androidApp/build.gradle.kts`), so there is no publish task for the `github` flavor at all.
Both flavors share the application id `com.neteinstein.networth`. The Play build is re-signed by Play App
Signing, so a phone moving from the sideloaded APK to the store install has to uninstall it first.
Play release notes live in `androidApp/src/playstore/play/release-notes/en-US/default.txt`.

## Releases (`release.yml`)

**Disabled for now.** The **`release`** job runs only when the repository variable `ANDROID_RELEASE_ENABLED`
is `true`; until then pushes to `main` build no Android release and upload nothing to Play. The TestFlight
job needs it, so it is skipped too. Set the variable to turn both back on.

When enabled, on every push to `main` (or a manual run), the **`release`** job:

1. Fails fast if the signing secrets are missing.
2. Re-runs `ktlintCheck` and `testDebugUnitTest` on the exact merged commit.
3. Decodes the keystore and builds `:androidApp:assembleGithubRelease` and `:androidApp:bundlePlaystoreRelease`,
   both signed, with `versionCode = run number` and `versionName = 1.0.<run number>`
   (`APP_VERSION_CODE` / `APP_VERSION_NAME`).
4. Re-runs `verify-obfuscation.sh` (both release variants).
5. Creates a GitHub Release `v1.0.<run number>` with `NetWorth_version1_0_<n>-github.apk`,
   `NetWorth_version1_0_<n>-playstore.aab`, each flavor's R8 mapping and the APK's SHA-1.
6. **Publishes to Google Play** with `:androidApp:publishPlaystoreReleaseBundle` when the
   `ANDROID_PUBLISHER_CREDENTIALS` secret is set. The bundle goes to the `internal` track, or to the `PLAY_TRACK`
   variable (`alpha`, `beta`, `production`). Gradle Play Publisher uploads the R8 mapping with it, so Play
   Console crash reports are readable. Without the secret the step is skipped with a notice, not failed.

### One-time Google Play setup

1. Create the app in the Play Console (package `com.neteinstein.networth`) and enrol in Play App Signing.
   The keystore in `KEYSTORE_BASE64` is the **upload key**.
2. **Upload the first bundle by hand.** The Play Developer API can only publish updates. Take the `.aab`
   from a GitHub Release and upload it to the internal track in the Play Console.
3. In Google Cloud, create a service account with a JSON key. Then in the Play Console → Users and
   permissions, invite its e-mail with release permissions for this app.
4. Add the JSON key as the `ANDROID_PUBLISHER_CREDENTIALS` secret, and optionally `PLAY_TRACK` as a variable.

A `403 PERMISSION_DENIED` from the upload means the service account isn't set up yet, not that the repo
is wrong. The usual causes are: it wasn't invited to the app, there was no first manual release, the API
isn't enabled, or a permission is still propagating (this can take up to a day).

### iOS → TestFlight (disabled)

The **`testflight`** job is wired up but **off**: it runs only when the repository variable
`IOS_RELEASE_ENABLED` is `true`. When on, it runs after the Android job on a macOS runner.

1. Archives the `Release` configuration with automatic signing through an App Store Connect API key
   (`-allowProvisioningUpdates`). Xcode fetches or creates the distribution certificate and the App Store
   profile itself, so no `.p12` or profile is stored anywhere. Versions are
   `MARKETING_VERSION = 1.0.<run number>` and `CURRENT_PROJECT_VERSION = run number`, as on Android.
2. Exports with `iosApp/Configuration/ExportOptions.plist` (`app-store-connect`, destination `upload`), which
   sends the build to App Store Connect. It then shows up in TestFlight once processed.

To turn it on:
1. Create the app in App Store Connect (bundle id `com.neteinstein.networth`).
2. Create an API key with the **Admin** role. Cloud-managed distribution certificates need Admin.
3. Add the secrets `APPLE_TEAM_ID`, `APP_STORE_CONNECT_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID` and
   `APP_STORE_CONNECT_KEY_P8` (the `.p8` file's contents).
4. Set the variable `IOS_RELEASE_ENABLED=true`.

`Info.plist` already declares `ITSAppUsesNonExemptEncryption = NO` (the app only uses HTTPS), so TestFlight
builds don't wait on the export-compliance question.

Locally `assembleRelease`/`bundlePlaystoreRelease` fall back to debug signing, so they always work without secrets.

Mapping files: R8 produces a different one per build, so each release keeps its own. Run a trace through
`retrace <mapping.txt> <trace.txt>`.

## Web (`deploy-pages.yml`)

Builds the Wasm production bundle and deploys it with `actions/deploy-pages`. One-time setup: repo
Settings → Pages → Source = **GitHub Actions**. For Google sign-in on the deployed site, add its origin
(`https://<owner>.github.io`) to the Web OAuth client's authorised JavaScript origins.

## Secrets and variables

| Name | Kind | Used by | Required? | Notes |
| --- | --- | --- | --- | --- |
| `KEYSTORE_BASE64` | secret | release | **Yes** | `base64 -w0 release.jks` of the upload keystore |
| `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | secret | release | **Yes** | Read by `androidApp/build.gradle.kts` (with `KEYSTORE_FILE`, set by the workflow) |
| `ANDROID_PUBLISHER_CREDENTIALS` | secret | release | No | Play service-account JSON key. Without it the Play upload is skipped (see Releases → One-time Google Play setup) |
| `PLAY_TRACK` | variable | release | No | Play track for uploads; default `internal` |
| `ANDROID_RELEASE_ENABLED` | variable | release | No | `true` turns the Android release job on; off for now |
| `IOS_RELEASE_ENABLED` | variable | release (testflight) | No | `true` turns the TestFlight job on; off by default |
| `APPLE_TEAM_ID`, `APP_STORE_CONNECT_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID`, `APP_STORE_CONNECT_KEY_P8` | secret | release (testflight) | Only with `IOS_RELEASE_ENABLED` | App Store Connect API key (Admin) and team (see Releases → iOS) |
| `GOOGLE_SERVICES_JSON` | secret | release (and setup action) | No | Real `google-services.json`; otherwise the committed `androidApp/google-services.json.ci` placeholder is used |
| `NETWORTH_GOOGLE_WEB_CLIENT_ID`, `NETWORTH_GOOGLE_IOS_CLIENT_ID`, `NETWORTH_GOOGLE_API_KEY`, `NETWORTH_GOOGLE_PROJECT_NUMBER` | secret | release, deploy-pages | No | Google OAuth client ids / Picker key compiled into `GoogleConfig` (`docs/google-cloud-setup.md`). Without them builds succeed and sign-in reports "not configured" |
| `CODECOV_TOKEN` | secret | pr (coverage) | No | Only needed if the repo is private |
| `GITHUB_TOKEN` | automatic | all | — | `contents: write` only in release (to create the Release) |

Never committed: `google-services.json`, `GoogleService-Info.plist`, keystores, provisioning profiles.
Only `*.ci` placeholders live in the repo.

## Signing status per platform

| Platform | Status |
| --- | --- |
| Android | PRs: debug-signed minified `github` APK + `playstore` bundle (R8 validation only). `main`: both signed with the upload keystore → GitHub Release, and the bundle → Play internal track once `ANDROID_PUBLISHER_CREDENTIALS` is set |
| iOS | Unsigned simulator build on PRs. TestFlight job ready but disabled (`IOS_RELEASE_ENABLED`); automatic signing via an App Store Connect API key when enabled |
| Web | No signing; deployed to GitHub Pages from `main` |

## Running the same checks locally

```bash
cp androidApp/google-services.json.ci androidApp/google-services.json
./gradlew ktlintCheck verifyTranslations :androidApp:lintGithubDebug testDebugUnitTest :koverVerify :androidApp:assembleGithubRelease
./scripts/verify-obfuscation.sh
```
