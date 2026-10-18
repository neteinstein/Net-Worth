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

    // Distribution channels, built by .github/workflows/release.yml:
    // - "github": the sideloaded APK published on GitHub Releases.
    // - "playstore": the app bundle uploaded to the Play Console (see the `play { }` block below).
    // Both share the application id; the Play build is re-signed by Play App Signing, so a device
    // switching from the sideloaded APK to the store install has to uninstall first.
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
        }
        create("playstore") {
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

    // The only flavor Gradle Play Publisher may upload (it is off for everything else, see `play { }`).
    playConfigs {
        register("playstore") {
            enabled.set(true)
        }
    }

    lint {
        checkDependencies = true
        abortOnError = true
        warningsAsErrors = false
    }
}

// Gradle Play Publisher: off by default and enabled for the "playstore" flavor only, so the "github"
// flavor can never be uploaded to the Play Console. `publishPlaystoreReleaseBundle` reads the
// ANDROID_PUBLISHER_CREDENTIALS env var (a Play service-account JSON key) and uploads to the
// "internal" track unless PLAY_TRACK overrides it, so nothing reaches production without a manual
// promotion in the Play Console. Release notes: src/playstore/play/release-notes/.
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
