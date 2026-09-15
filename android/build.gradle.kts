// All plugins used anywhere in this build are declared once here (apply false) and referenced
// without a version in each module — the standard Android multi-module layout. Two problems
// showed up when this wasn't the case:
//  1. Declaring the same Kotlin plugin version separately in :analyzer and :app made Gradle load
//     it twice, which silently broke cross-module type resolution for KSP (Room's processor saw
//     types from :analyzer as unresolvable when :app was compiled).
//  2. Leaving the Android Gradle Plugin out of this root file (to dodge Google's Maven repo, see
//     below) while centralizing only the Kotlin plugins here caused Kotlin's Android target setup
//     to fail: "Could not generate a decorated class for type KotlinAndroidTarget >
//     com/android/build/gradle/api/BaseVariant" — AGP and the Kotlin Android plugin need to be
//     resolved together for their integration points to bind.
//
// Trade-off: resolving `com.android.application` requires Google's Maven repository
// (dl.google.com), which some environments this repo is developed in can't reach — so
// `./gradlew :analyzer:test` alone can't be verified from those. It still runs fine on CI
// (GitHub Actions), which does the actual app build.
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
