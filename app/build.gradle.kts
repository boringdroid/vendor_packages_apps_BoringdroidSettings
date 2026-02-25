import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.diffplug.spotless") version "7.2.1"
    id("com.android.application")
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

spotless {
    format("misc") {
        target("*.md", ".gitignore")
        trimTrailingWhitespace()
        leadingSpacesToTabs()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        ktfmt().kotlinlangStyle()
    }

    kotlin {
        target("**/*.kt")
        ktfmt().kotlinlangStyle()
    }
}

android {
    namespace = "com.boringdroid.settings"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.boringdroid.settings"
        minSdk = 28
        targetSdk = 35
        versionCode = 140
        versionName = "140"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("plugin.keystore")
            storePassword = "android"
        }
    }

    lint {
        abortOnError = true
        baseline = file("lint-baseilne.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

val javaToolchains = extensions.getByType<JavaToolchainService>()

tasks.withType<JavaCompile>().configureEach {
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.lifecycle:lifecycle-common:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.20")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("com.google.truth:truth:1.4.4")
    androidTestImplementation("com.google.truth.extensions:truth-java8-extension:1.4.4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
