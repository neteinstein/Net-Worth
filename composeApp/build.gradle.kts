import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.networth.kmp.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // iOS: the umbrella framework the Xcode project links (see iosApp/ and AGENTS.md).
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // Web: this module is also the wasmJs executable (served from src/wasmJsMain/resources).
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.database)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.google)
            implementation(projects.feature.manage)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.overview)
            implementation(projects.feature.settings)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
            implementation(projects.core.testing)
        }
    }
}
