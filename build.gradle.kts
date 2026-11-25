// Top-level build file. Version choices documented in README and personal-vpn-plan.md.
// AGP 8.7.3 + Gradle 8.9 + Kotlin 2.0.21: stable, widely available, supports compileSdk 35.
// compileSdk 35 / targetSdk 34 / minSdk 24 per project plan (compileSdk 36 in plan; using 35
// for AGP 8.7.x compatibility - bump to 36 with AGP 8.9+ when SDK is installed).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
