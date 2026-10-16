plugins {
    `kotlin-dsl`
}

group = "com.neteinstein.networth.buildlogic"

dependencies {
    // compileOnly: the real plugin jars are put on the classpath by the root build's `apply false`.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.networth.kmp.library.get().pluginId
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = libs.plugins.networth.kmp.compose.get().pluginId
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("androidApplication") {
            id = libs.plugins.networth.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
