// Intentionally empty: each module declares its own plugins so that running
// tests on the pure-Kotlin `analyzer` module never requires resolving the
// Android Gradle Plugin (only needed by `:app`, and only reachable from a
// machine with access to Google's Maven repository / the Android SDK).
