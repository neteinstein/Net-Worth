import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

/**
 * KMP library + Compose Multiplatform: compiler plugin, the common Compose stack (Material 3,
 * resources, lifecycle/ViewModel, Koin-Compose) and a per-module generated `Res` class.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) { configure() }
    }

    private fun Project.configure() {
        pluginManager.apply(KmpLibraryConventionPlugin::class.java)
        with(pluginManager) {
            apply(libs.pluginId("compose-multiplatform"))
            apply(libs.pluginId("compose-compiler"))
        }

        extensions.configure<KotlinMultiplatformExtension> {
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                // Required so Compose resources (strings, drawables) are packaged for Android.
                androidResources { enable = true }
            }

            sourceSets.commonMain.dependencies {
                implementation(libs.library("compose-runtime"))
                implementation(libs.library("compose-foundation"))
                implementation(libs.library("compose-ui"))
                implementation(libs.library("compose-material3"))
                implementation(libs.library("compose-material-icons-core"))
                implementation(libs.library("compose-components-resources"))
                implementation(libs.library("compose-ui-tooling-preview"))
                implementation(libs.library("jetbrains-lifecycle-viewmodel"))
                implementation(libs.library("jetbrains-lifecycle-viewmodel-compose"))
                implementation(libs.library("jetbrains-lifecycle-runtime-compose"))
                implementation(libs.library("koin-core-viewmodel"))
                implementation(libs.library("koin-compose"))
                implementation(libs.library("koin-compose-viewmodel"))
            }
            // Screenshot tests (Roborazzi on Robolectric) live in androidHostTest.
            sourceSets.matching { it.name == "androidHostTest" }.configureEach {
                dependencies {
                    implementation(libs.library("roborazzi"))
                    implementation(libs.library("roborazzi-compose"))
                    implementation(libs.library("robolectric"))
                    implementation(libs.library("junit"))
                }
            }
        }

        // Screenshots are compared with the committed goldens on every test run; record new ones
        // with `./gradlew testDebugUnitTest -PrecordScreenshots`.
        val record = providers.gradleProperty("recordScreenshots").isPresent
        tasks.withType(Test::class.java).configureEach {
            systemProperty("roborazzi.test.record", record)
            systemProperty("roborazzi.test.verify", !record)
            systemProperty("robolectric.graphicsMode", "NATIVE")
            systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            // Robolectric (SDK 36) reaches into FileDescriptor internals on JDK 17+.
            jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
            inputs.files(project.fileTree("src/androidHostTest/screenshots")).withPropertyName("screenshotGoldens")
        }

        extensions.configure<ComposeExtension> {
            (this as ExtensionAware).extensions.configure<ResourcesExtension> {
                packageOfResClass = "$derivedNamespace.generated.resources"
                generateResClass = org.jetbrains.compose.resources.ResourcesExtension.ResourceClassGeneration.Always
            }
        }
    }
}
