plugins {
    alias(libs.plugins.networth.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            // Reads merchant rule files (JsonElement only, no generated serializers).
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
