@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

// Root build: declare every plugin once (apply false) so all modules share one classloader,
// then aggregate coverage across modules with Kover.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.play.publisher) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover)
}

// Kotlin/Wasm downloads Node.js, Yarn and Binaryen through repositories it adds to the project, which
// RepositoriesMode.FAIL_ON_PROJECT_REPOS forbids. Clearing downloadBaseUrl keeps the downloads but
// resolves them from the repositories declared in settings.gradle.kts instead.
allprojects {
    plugins.configureEach {
        listOf(
            extensions.findByType<WasmNodeJsEnvSpec>()?.downloadBaseUrl,
            extensions.findByType<WasmYarnRootEnvSpec>()?.downloadBaseUrl,
            extensions.findByType<BinaryenEnvSpec>()?.downloadBaseUrl,
        ).forEach { it?.set(null as String?) }
    }
}

dependencies {
    kover(projects.composeApp)
    kover(projects.core.database)
    kover(projects.core.designsystem)
    kover(projects.core.domain)
    kover(projects.core.google)
    kover(projects.feature.manage)
    kover(projects.feature.onboarding)
    kover(projects.feature.overview)
    kover(projects.feature.settings)
}

kover {
    reports {
        filters {
            excludes {
                // Coverage targets domain/data/ViewModel logic. Composables are verified by previews and
                // (later) UI tests, and generated code has nothing to assert.
                packages("*.generated.resources", "*.ui.theme", "*.ui.components", "*.ui.navigation")
                classes("*ComposableSingletons*", "*ScreenKt*", "*.AppKt*", "*.MainViewControllerKt*", "*.MainKt*")
                annotatedBy("androidx.compose.runtime.Composable", "androidx.compose.ui.tooling.preview.Preview")
            }
        }
        verify {
            // Ratchet up as features land; CI runs `koverVerify`.
            rule { minBound(80) }
        }
    }
}
