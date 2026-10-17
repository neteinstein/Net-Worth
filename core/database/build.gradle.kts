plugins {
    alias(libs.plugins.networth.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.google)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
