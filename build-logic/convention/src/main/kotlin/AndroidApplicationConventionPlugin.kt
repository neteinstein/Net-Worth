import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * The Android app shell (`:androidApp`). AGP 9 has built-in Kotlin support, so no
 * `org.jetbrains.kotlin.android` plugin is applied here.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) { configure() }
    }

    private fun Project.configure() {
        with(pluginManager) {
            apply(libs.pluginId("android-application"))
            apply(libs.pluginId("compose-compiler"))
            apply(libs.pluginId("ktlint"))
        }
        configureKtlint()

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.version("android-compileSdk").toInt()
            defaultConfig {
                minSdk = libs.version("android-minSdk").toInt()
                targetSdk = libs.version("android-targetSdk").toInt()
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
