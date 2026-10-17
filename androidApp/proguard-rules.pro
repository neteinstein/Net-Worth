# R8 rules for the release build (isMinifyEnabled + isShrinkResources).
# Most libraries ship consumer rules (Koin, Compose, coroutines, kotlinx.serialization, Firebase);
# keep this file to what the app itself needs.

# Navigation routes are @Serializable and saved/restored by name via kotlinx.serialization.
-keepclassmembers @kotlinx.serialization.Serializable class com.neteinstein.networth.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.neteinstein.networth.**$$serializer { *; }

# Keep line numbers for readable Crashlytics/Play Console stack traces; hide source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Credential Manager loads its Play Services provider reflectively.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
