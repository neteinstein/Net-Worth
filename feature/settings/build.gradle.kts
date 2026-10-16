plugins {
    alias(libs.plugins.networth.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)
        }
    }
}
