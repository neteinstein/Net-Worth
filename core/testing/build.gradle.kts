// Test doubles shared by feature modules' commonTest source sets. Never a main dependency.
plugins {
    alias(libs.plugins.networth.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            implementation(projects.core.designsystem)
        }
        androidMain.dependencies {
            // Screenshot helper for the features' androidHostTest (Roborazzi on Robolectric).
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
            implementation(libs.robolectric)
        }
    }
}
