// Top-level build.gradle.kts
plugins {
    // NOTE: Use 'alias' for versions defined in the libs.versions.toml file
    // If you are not using the version catalog, you can hardcode the versions.
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}