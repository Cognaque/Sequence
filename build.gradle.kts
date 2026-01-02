// Root build.gradle.kts
// Modern Gradle configuration using Version Catalog (libs.versions.toml)

plugins {
    // The 'apply false' syntax tells Gradle to load the plugin
    // but not to apply it to the root project itself.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

// Global cleanup task
tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}