plugins {
    kotlin("jvm")
}

kotlin {
    // Must match :app's Java target (compileOptions in app/build.gradle.kts). A dependency
    // compiled to a higher JVM bytecode target than the consuming module's own target can make
    // the Kotlin compiler/KSP fail to resolve its classes — this is what caused Room's KSP
    // processor to report MatchAnalysisEntity's types (which import AnalysisResult from this
    // module) as "not present" even though the project dependency was declared correctly.
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

tasks.test {
    useJUnitPlatform()
}
