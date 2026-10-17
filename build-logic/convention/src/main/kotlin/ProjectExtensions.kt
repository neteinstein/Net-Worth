import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

internal const val BASE_PACKAGE = "com.neteinstein.networth"

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

internal fun VersionCatalog.pluginId(alias: String): String = findPlugin(alias).get().get().pluginId

/** `:feature:overview` -> `com.neteinstein.networth.feature.overview`, `:composeApp` -> `…networth.composeapp` */
internal val Project.derivedNamespace: String
    get() = BASE_PACKAGE + path.replace(':', '.').replace('-', '_').lowercase()

/** ktlint must never lint generated code (Compose resources, serialization, etc.). */
internal fun Project.configureKtlint() {
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            // Patterns match paths relative to each source root; every generated package here
            // (Compose `Res`, resource collectors) contains a `generated` segment.
            exclude("**/generated/**")
            exclude { it.file.path.contains("${java.io.File.separator}build${java.io.File.separator}") }
        }
    }
}
