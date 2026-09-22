plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.parcelize")
    kotlin("plugin.compose")
    // Firebase / Google services are only needed by the googlePlay flavor and require a
    // google-services.json which is not part of this repository, so resolve them here but
    // apply them conditionally below.
    id("com.google.gms.google-services") apply false
    id("com.google.firebase.crashlytics") apply false
    id("com.mikepenz.aboutlibraries.plugin")
}

// Without google-services.json the Google Services plugin fails the build (e.g. plain
// `assembleDebug` builds every flavor). Apply it only when the file exists, so fdroid builds
// and fresh clones keep working.
val hasGoogleServicesConfig = file("google-services.json").exists() ||
        file("src/googlePlay/google-services.json").exists()

if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

adjustFlavorTasks()

kotlin {
    jvmToolchain(17)
}

android {

    namespace = "ua.syt0r.kanji"

    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.huangsy.kanjicc"
        minSdk = 26
        targetSdk = 36
        versionCode = AppVersion.versionCode
        versionName = AppVersion.versionName
    }

    buildTypes {
        val debug = getByName("debug") {
            versionNameSuffix = "-debug"
            applicationIdSuffix = ".dev"
        }

        val release = getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "version"

    productFlavors {
        create("googlePlay") {
            dimension = "version"
        }

        create("fdroid") {
            dimension = "version"
            applicationIdSuffix = ".fdroid"
        }
    }

    buildFeatures {
        compose = true
    }

    val keystoreFile = rootProject.file("keystore.jks")

    val signedBuildSigningConfig = signingConfigs.create("signedBuild") {
        storeFile = keystoreFile
        System.getenv("ANDROID_KEYSTORE_PASSWORD")?.let { storePassword = it }
        System.getenv("ANDROID_KEY_ALIAS")?.let { keyAlias = it }
        System.getenv("ANDROID_KEY_PASSWORD")?.let { keyPassword = it }
    }

    val debugSigningConfig = signingConfigs.getByName("debug")

    // debug：本地 run / 测试固定使用 Android 默认 debug 签名，不需要 keystore 密码
    buildTypes.getByName("debug").signingConfig = debugSigningConfig

    // release：keystore.jks 在场时使用正式签名；打包该变体时需要
    // ANDROID_KEYSTORE_PASSWORD / ANDROID_KEY_ALIAS / ANDROID_KEY_PASSWORD 环境变量。
    buildTypes.getByName("release").signingConfig = if (keystoreFile.exists()) {
        signedBuildSigningConfig
    } else {
        debugSigningConfig
    }

    if (keystoreFile.exists() && System.getenv("ANDROID_KEYSTORE_PASSWORD").isNullOrEmpty()) {
        logger.warn(
            "keystore.jks 存在但未设置 ANDROID_KEYSTORE_PASSWORD：debug 构建不受影响，" +
                "但 release 打包会失败。发布前请设置 ANDROID_KEYSTORE_PASSWORD / " +
                "ANDROID_KEY_ALIAS / ANDROID_KEY_PASSWORD。"
        )
    }

    dependenciesInfo {
        // Removes a signing block with encrypted data for reproducible F-Droid builds
        includeInApk = false
    }

}

dependencies {
    implementation(project(":core"))

    "googlePlayImplementation"(platform(libs.firebase.bom))
    "googlePlayImplementation"(libs.firebase.analytics.ktx)
    "googlePlayImplementation"(libs.firebase.crashlytics.ktx)
    "googlePlayImplementation"(libs.billing.ktx)
    "googlePlayImplementation"(libs.review.ktx)
}

aboutLibraries {
    configPath = "core/credits"
    excludeFields = arrayOf("generated")
}

fun adjustFlavorTasks() {
    project.gradle.taskGraph.whenReady {
        allTasks.forEach { task ->

            val isFdroid = task.name.contains("fdroid", ignoreCase = true)

            val isGoogleTask = task.name.contains("GoogleServices", ignoreCase = true) ||
                    task.name.contains("Crashlytics", ignoreCase = true)

            val isArtProfileTask = task.name.contains("ArtProfile", ignoreCase = true)

            if (isFdroid && (isGoogleTask || isArtProfileTask)) {
                println("Disabling f-droid task: ${task.name}")
                task.enabled = false
            }

        }
    }
}
