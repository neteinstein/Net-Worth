---
name: bump-dependencies
description: Update Kotlin, Compose Multiplatform, AGP, Gradle and library versions in gradle/libs.versions.toml to the latest stable releases while checking KMP/Compose Multiplatform compatibility. Use when asked to update, bump or upgrade dependencies or tooling.
---

# Bump dependencies safely

All versions live in `gradle/libs.versions.toml` (plus the Gradle version in
`gradle/wrapper/gradle-wrapper.properties`). Never inline a version elsewhere.

## 1. Find latest *stable* versions

Read `maven-metadata.xml` and ignore `alpha|beta|rc|dev|eap|-M` versions:

- Google Maven (`https://dl.google.com/dl/android/maven2/<group path>/<artifact>/maven-metadata.xml`): AGP
  (`com/android/tools/build/gradle`), `androidx.*`, Firebase BOM, google-services.
- Maven Central (prefer `https://repo1.maven.org/maven2/…` or the Google mirror
  `https://maven-central.storage-download.googleapis.com/maven2/…` if `repo.maven.apache.org` returns 429):
  Kotlin, `org.jetbrains.compose.*`, `org.jetbrains.androidx.*`, kotlinx, Koin, Kover.
- Gradle Plugin Portal (`https://plugins.gradle.org/m2/…`): Compose plugin marker, ktlint-gradle.
- Gradle: `https://services.gradle.org/versions/current`.
- Android SDK: `https://dl.google.com/android/repository/repository2-3.xml` (`platforms;android-NN`).

## 2. Check compatibility before changing anything

- **Compose Multiplatform ↔ Material 3 / icons:** the matching versions are hard-coded in the CMP Gradle
  plugin. Download `compose-gradle-plugin-<cmp>.jar` (plugin portal, follow redirects with `curl -L`) and run
  `strings 'org/jetbrains/compose/ComposePlugin$Dependencies.class' | grep material3`.
- **JetBrains lifecycle / navigation3 / savedstate:** open the artifact's `.module` file and check which
  `org.jetbrains.compose.*` version it requires — it must be ≤ the CMP version in the catalog.
- **Koin:** check `koin-compose-viewmodel`'s `.module` for its lifecycle requirement.
- **Kotlin ↔ CMP ↔ AGP:** CMP release notes list the supported Kotlin range; AGP release notes list the
  minimum Gradle version; the Compose compiler is versioned with Kotlin (same `kotlin` version ref).
- Every KMP library must publish `android`, `iosArm64`, `iosSimulatorArm64` and `wasmJs` variants (check
  the `.module` file's `variants`).
- `targetSdk`/`compileSdk` = latest stable platform; `minSdk` stays 5 below unless told otherwise.

## 3. Apply and verify

1. Edit the catalog (and wrapper: `./gradlew wrapper --gradle-version <v>` twice).
2. `./gradlew testDebugUnitTest ktlintCheck :androidApp:lintGithubDebug :koverVerify :androidApp:assembleGithubRelease compileKotlinWasmJs`.
3. iOS can only be verified on macOS — rely on the `build-ios` CI job and say so in the PR.
4. Commit as `build(deps): bump <thing> to <version>` (one logical bump per commit when practical) and
   update the stack table in AGENTS.md if a headline version changed.
