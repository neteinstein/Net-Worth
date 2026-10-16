plugins {
    alias(libs.plugins.networth.kmp.compose)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
    }
}
