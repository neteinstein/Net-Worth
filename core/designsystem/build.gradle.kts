plugins {
    alias(libs.plugins.networth.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            // OpenDocument launchers for the file picker.
            implementation(libs.androidx.activity.compose)
        }
    }
}
