plugins {
    alias(libs.plugins.networth.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
        }
        androidMain.dependencies {
            // OpenDocument launcher for the statement file picker.
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
