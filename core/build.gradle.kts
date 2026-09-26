@file:OptIn(ExperimentalBuildToolsApi::class)

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    alias(libs.plugins.build.config)
    id("app.cash.sqldelight")
}

kotlin {

    jvm()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.ui)
                api(compose.foundation)
                api(compose.material)
                api(compose.material3)
                api(libs.material3.window.size.clazz)
                api(compose.runtime)
                api(compose.materialIconsExtended)
                api(compose.components.resources)

                api(libs.koin.core)
                api(libs.koin.compose)
                api(libs.koin.compose.viewmodel)

                api(libs.kotlinx.datetime)
                api(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.reflect)

                implementation(libs.datastore.preferences.core)
                implementation(libs.wanakana.core)

                api(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.auth)

                api(libs.aboutlibraries.core)

                api(libs.compose.reorderable)
                api(libs.material.kolor)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)

                api(libs.lifecycle.viewmodel.ktx)
                api(libs.lifecycle.livedata.ktx)

                implementation(libs.work.runtime.ktx)

                api(libs.koin.android)
                api(libs.koin.androidx.compose)

                implementation(libs.navigation.compose)
                api(libs.activity.compose)
                api(libs.datastore.preferences)
                api(compose.uiTooling)

                api(libs.core.ktx)
                api(libs.appcompat)
                implementation(libs.media3.exoplayer)

                // VOICEVOX CORE for Android. The JVM jar only carries the desktop natives, the AAR
                // adds the Android ones (jni/<abi>/libvoicevox_core_java_api.so) plus the same
                // blocking Java API. See TTS-HANDOFF.md (M2).
                implementation(files("libs/voicevoxcore-android-0.17.0.aar"))
                implementation(libs.gson)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.jvm.sqlite.driver)
                implementation(libs.ktor.server.netty)
                implementation(libs.mp3spi)

                // Offline Japanese TTS engine for the desktop word pronunciation (see
                // TTS-HANDOFF.md). The jar ships the native library for every desktop platform and
                // unpacks the matching one at runtime, so no extra packaging step is needed yet.
                implementation(files("libs/voicevoxcore-0.17.0.jar"))
                implementation(libs.gson)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        iosMain {
            dependencies {
                implementation(libs.sqldelight.native.sqlite.driver)
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

compose.resources {
    generateResClass = always
    packageOfResClass = "ua.syt0r.kanji"
    publicResClass = true
}

registerPrepareAppAssetTasks()

sqldelight {
    linkSqlite = true
    databases {
        create("AppDataDatabase") {
            packageName.set("ua.syt0r.kanji.core.app_data.db")
            srcDirs("src/commonMain/sqldelight_app_data")
        }
        create("UserDataDatabase") {
            packageName.set("ua.syt0r.kanji.core.user_data.db")
            srcDirs("src/commonMain/sqldelight_user_data")
        }
    }
}

android {
    namespace = "ua.syt0r.kanji.core"

    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")

        // VOICEVOX runtime files, assembled by tools/voicevox/fetch-runtime.ps1 (.sh) and kept out
        // of the repository: the dictionary and the voice model become assets (extracted to
        // filesDir on first use), the ONNX Runtime build is a native library so that dlopen can
        // find it. See TTS-HANDOFF.md (M2/M3, Android).
        assets.srcDir(rootProject.file("tools/voicevox/runtime/core/dict"))
        assets.srcDir(rootProject.file("tools/voicevox/runtime/models"))
        jniLibs.srcDir(rootProject.file("tools/voicevox/runtime/android/jniLibs"))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            consumerProguardFile("consumer-rules.pro")
        }
    }

}

buildConfig {

    packageName = "ua.syt0r.kanji"

    buildConfigField("versionCode", AppVersion.versionCode.toLong())
    buildConfigField("versionName", AppVersion.versionName)
    buildConfigField("appDataAssetName", AppAssets.AppDataAssetFileName)
    buildConfigField("appDataDatabaseVersion", AppAssets.AppDataDatabaseVersion)

    val kanaVoiceFieldName = "kanaVoiceAssetName"

    sourceSets.getByName("androidMain") {
        buildConfigField(
            name = kanaVoiceFieldName,
            value = AppAssets.kanaVoiceOpus.fileName
        )
    }

    sourceSets.getByName("jvmMain") {
        buildConfigField(
            name = kanaVoiceFieldName,
            value = AppAssets.kanaVoiceWav.fileName
        )
    }

    sourceSets.getByName("iosMain") {
        buildConfigField(
            name = kanaVoiceFieldName,
            value = AppAssets.kanaVoiceWav.fileName
        )
    }

}

tasks.withType<Test> {
    useJUnitPlatform()
    // The VOICEVOX runtime files live outside the repository's sources (tools/voicevox/runtime,
    // assembled by tools/voicevox/fetch-runtime.ps1); point the tests at them explicitly.
    systemProperty("kanjidojo.voicevox.dir", rootProject.file("tools/voicevox/runtime").absolutePath)
}
