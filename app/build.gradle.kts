plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    // Required for Room @Entity, @Dao, and @Database processing
    alias(libs.plugins.google.devtools.ksp)
}

// KSP Configuration for Room to handle potential missing types during incremental builds
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

android {
    namespace = "com.cognaque.sequence"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cognaque.sequence"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Kotlin 1.9.23 is compatible with Compose Compiler 1.5.11
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            // Using .add() explicitly to avoid DSL resolution ambiguity
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }

    // Updated SourceSets to ensure Gradle 8.13 recognizes test directories
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
        getByName("test") {
            java.srcDirs("src/test/java")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java")
        }
    }

    // Explicitly enable unit tests if they were somehow disabled by default
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

// Ensure consistent JVM targets across all modules to prevent task graph errors
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        // Added freeCompilerArgs to ensure better compatibility with newer Gradle versions
        freeCompilerArgs.addAll(listOf("-Xjvm-default=all", "-Xcontext-receivers"))
    }
}

dependencies {
    // Core Android and Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Jetpack Compose with Bill of Materials (BOM)
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.8")


    // Room Database - Local Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // KSP processes the Room annotations in MainActivity.kt
    ksp(libs.androidx.room.compiler)

    // Testing - Ensure standard JUnit is present to trigger 'testClasses' task
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.01"))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug Tools
    debugImplementation(libs.androidx.ui.tooling)
    // FIX: Changed 'ui-test-manifest' to 'ui.test.manifest' to resolve the minus operator error
    debugImplementation(libs.androidx.ui.test.manifest)
}
