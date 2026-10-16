plugins {
    alias(libs.plugins.networth.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.neteinstein.networth.android"

    defaultConfig {
        applicationId = "com.neteinstein.networth"
        versionCode = 1
        versionName = "0.1.0"
    }

    // Release signing comes from CI secrets (see CI_CD.md). Without them the release build is
    // produced unsigned, which is enough to validate R8/minification on every PR.
    val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        buildConfig = false
    }

    lint {
        checkDependencies = true
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.core.designsystem)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)

    implementation(project.dependencies.platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
