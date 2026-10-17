// Test doubles shared by feature modules' commonTest source sets. Never a main dependency.
plugins {
    alias(libs.plugins.networth.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
        }
    }
}
