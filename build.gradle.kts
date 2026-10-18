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
    kover(projects.core.claude)
    kover(projects.core.database)
    kover(projects.core.designsystem)
    kover(projects.core.domain)
    kover(projects.core.google)
    kover(projects.feature.alerts)
    kover(projects.feature.future)
    kover(projects.feature.importer)
    kover(projects.feature.manage)
    kover(projects.feature.onboarding)
    kover(projects.feature.overview)
    kover(projects.feature.settings)
    kover(projects.feature.spending)
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

/**
 * Every user-facing string ships in English (`values/`) and European Portuguese (`values-pt/`): each module's
 * two strings.xml must declare the same strings and plurals, with the same placeholders, and every plural needs
 * its `one` and `other` forms. CI runs it with the lint job; run `./gradlew verifyTranslations` locally.
 */
abstract class VerifyTranslations : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringFiles: ConfigurableFileCollection

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    private val placeholder = Regex("%\\d+\\$[a-z]")

    @TaskAction
    fun verify() {
        val root = rootDirectory.get().asFile
        val problems = mutableListOf<String>()
        stringFiles.files.filter { it.parentFile.name == "values" }.sortedBy { it.path }.forEach { english ->
            val path = english.relativeTo(root).path
            val portuguese = english.parentFile.resolveSibling("values-pt").resolve("strings.xml")
            if (!portuguese.exists()) {
                problems += "$path: no values-pt/strings.xml next to it"
                return@forEach
            }
            val en = entries(english)
            val pt = entries(portuguese)
            (en.keys - pt.keys).sorted().forEach { problems += "$path: '$it' has no Portuguese translation" }
            (pt.keys - en.keys).sorted().forEach { problems += "$path: '$it' is only in values-pt" }
            en.keys.intersect(pt.keys).sorted().forEach { key ->
                val enForms = en.getValue(key)
                val ptForms = pt.getValue(key)
                if (enForms.keys != ptForms.keys) {
                    problems += "$path: '$key' has forms ${enForms.keys} in English but ${ptForms.keys} in Portuguese"
                }
                if (enForms.size > 1 || "other" in enForms) {
                    listOf("one", "other").filter { it !in enForms || it !in ptForms }
                        .forEach { problems += "$path: plural '$key' is missing its '$it' form" }
                }
                val enArgs = enForms.values.flatMap { placeholder.findAll(it).map { m -> m.value }.toList() }.toSet()
                val ptArgs = ptForms.values.flatMap { placeholder.findAll(it).map { m -> m.value }.toList() }.toSet()
                if (enArgs != ptArgs) problems += "$path: '$key' uses $enArgs in English but $ptArgs in Portuguese"
            }
        }
        val out = report.get().asFile
        out.parentFile.mkdirs()
        out.writeText(problems.joinToString("\n"))
        if (problems.isNotEmpty()) {
            throw GradleException("Translations are out of sync:\n" + problems.joinToString("\n") { "  - $it" })
        }
    }

    /** name → (quantity or "" for a plain string → text). */
    private fun entries(file: File): Map<String, Map<String, String>> {
        val document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val result = linkedMapOf<String, Map<String, String>>()
        val nodes = document.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? org.w3c.dom.Element ?: continue
            val name = element.getAttribute("name")
            when (element.tagName) {
                "string" -> result[name] = mapOf("" to element.textContent)
                "plurals" -> {
                    val items = element.getElementsByTagName("item")
                    result[name] = (0 until items.length).associate { index ->
                        val item = items.item(index) as org.w3c.dom.Element
                        item.getAttribute("quantity") to item.textContent
                    }
                }
            }
        }
        return result
    }
}

tasks.register<VerifyTranslations>("verifyTranslations") {
    group = "verification"
    description = "Checks that every values/strings.xml has a matching values-pt/strings.xml."
    stringFiles.from(
        fileTree(rootDir) {
            include("**/src/*/composeResources/values/strings.xml", "**/src/*/composeResources/values-pt/strings.xml")
            exclude("**/build/**", "build-logic/**")
        },
    )
    rootDirectory.set(layout.projectDirectory)
    report.set(layout.buildDirectory.file("reports/translations.txt"))
}
