import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Base for every shared KMP module: Android (KMP library plugin, AGP 9+), iosArm64,
 * iosSimulatorArm64 (no iosX64 — removed upstream) and wasmJs (web).
 *
 * Also wires kotlin.test into commonTest, ktlint, Kover, and a `testDebugUnitTest` alias so the
 * JVM-hosted run of commonTest has the same task name as in a classic Android module.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) { configure() }
    }

    private fun Project.configure() {
        with(pluginManager) {
            apply(libs.pluginId("kotlin-multiplatform"))
            apply(libs.pluginId("android-kotlin-multiplatform-library"))
            apply(libs.pluginId("ktlint"))
            apply(libs.pluginId("kover"))
        }
        configureKtlint()

        extensions.configure<KotlinMultiplatformExtension> {
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                namespace = derivedNamespace
                compileSdk = libs.version("android-compileSdk").toInt()
                minSdk = libs.version("android-minSdk").toInt()
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
                // Runs commonTest on the JVM (task: testAndroidHostTest) — no emulator needed.
                withHostTest { }
            }

            iosArm64()
            iosSimulatorArm64()

            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                browser()
            }

            sourceSets.commonMain.dependencies {
                implementation(project.dependencies.platform(libs.library("koin-bom")))
                implementation(libs.library("koin-core"))
                implementation(libs.library("kotlinx-coroutines-core"))
            }
            sourceSets.commonTest.dependencies {
                implementation(libs.library("kotlin-test"))
                implementation(libs.library("kotlinx-coroutines-test"))
            }
        }

        // Familiar alias: `./gradlew testDebugUnitTest` runs commonTest on the JVM in every module.
        tasks.register("testDebugUnitTest") {
            group = "verification"
            description = "Runs commonTest on the JVM via the Android host test (alias of testAndroidHostTest)."
            dependsOn("testAndroidHostTest")
        }
    }
}
