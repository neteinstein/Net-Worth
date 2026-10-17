plugins {
    alias(libs.plugins.networth.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.play.publisher)
}

android {
    namespace = "com.neteinstein.networth.android"

    defaultConfig {
        applicationId = "com.neteinstein.networth"
        // .github/workflows/release.yml sets these from the Actions run number, so every release gets a
        // unique, increasing versionCode without editing this file. Local builds use the defaults.
        versionCode = providers.environmentVariable("APP_VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.environmentVariable("APP_VERSION_NAME").orNull ?: "0.1.0"
    }

    // Distribution channels. For now only "github": the APK published on GitHub Releases by
    // .github/workflows/release.yml. A "playstore" flavor (and the Play Console upload, see the
    // `play { }` block below) is added when the app is ready for the store, as in FamilyMoments.
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
        }
    }

    // Release signing comes from CI secrets (KEYSTORE_BASE64 decoded to KEYSTORE_FILE, plus
    // KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD — see CI_CD.md). Without them the release build
    // falls back to debug signing so `assembleRelease` still works (and installs) on any machine.
    val keystoreFile = providers.environmentVariable("KEYSTORE_FILE").orNull
    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("KEY_PASSWORD").get()
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
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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

// Gradle Play Publisher, disabled until a "playstore" flavor exists: the "github" flavor must never be
// uploaded to the Play Console. To publish, add that flavor, enable it with
// `playConfigs { register("playstore") { enabled.set(true) } }` and run `publishPlaystoreReleaseBundle`
// with ANDROID_PUBLISHER_CREDENTIALS (a Play service-account JSON key) set. Goes to the "internal"
// track unless PLAY_TRACK overrides it, so nothing reaches production without a manual promotion.
play {
    enabled.set(false)
    track.set(providers.environmentVariable("PLAY_TRACK").orNull?.takeIf { it.isNotBlank() } ?: "internal")
    defaultToAppBundles.set(true)
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
