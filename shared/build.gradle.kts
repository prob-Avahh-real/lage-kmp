plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.detekt)
}

detekt {
    config.from(rootProject.file("detekt-config.yml"))
    buildUponDefaultConfig = true

    // Android sources not supported in commonTest
    source.from(
        files("src/commonMain/kotlin"),
        files("src/commonTest/kotlin"),
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Ktor — HTTP 客户端 (HTTP/2, Brotli/Gzip, Content Negotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.encoding)     // ← Brotli + Gzip 压缩
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)

            // KotlinX
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)       // Android: OkHttp 引擎
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)       // iOS: Darwin 引擎 (URLSession)
        }
    }
}

android {
    namespace = "com.lage.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
