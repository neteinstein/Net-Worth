import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
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
        }

        extensions.configure<ComposeExtension> {
            (this as ExtensionAware).extensions.configure<ResourcesExtension> {
                packageOfResClass = "$derivedNamespace.generated.resources"
                generateResClass = org.jetbrains.compose.resources.ResourcesExtension.ResourceClassGeneration.Always
            }
        }
    }
}
