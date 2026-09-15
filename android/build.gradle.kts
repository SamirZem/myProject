// Kotlin/KSP plugins are declared once here (apply false) and referenced without a version in
// each module, so Gradle loads a single instance of the Kotlin Gradle plugin across the build —
// declaring the same version separately in each module's own build.gradle.kts makes Gradle load
// it twice, which silently breaks cross-module type resolution for KSP (Room's processor sees
// types from :analyzer as unresolvable when :app is compiled).
//
// The Android Gradle Plugin is deliberately NOT declared here: it can only be resolved from
// Google's Maven repository, which isn't reachable in every environment this repo is developed
// in. Keeping it out of the root means `./gradlew :analyzer:test` still works anywhere with plain
// Maven Central access; `com.android.application` is declared, with its version, in
// android/app/build.gradle.kts only.
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
