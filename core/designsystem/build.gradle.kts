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
        }
    }
}
