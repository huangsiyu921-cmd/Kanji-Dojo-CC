import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.compose.hot-reload")
    id("com.mikepenz.aboutlibraries.plugin")
}

kotlin {

    jvm()

    jvmToolchain(17)
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }

    sourceSets {

        jvmMain {
            dependencies {
                implementation(compose.components.resources)
                implementation(project(":core"))
            }
        }

    }

}

val mainClassKt = "ua.syt0r.kanji.desktopApp.MainKt"

// --- VOICEVOX runtime files bundled into the desktop distribution -----------------------------
// tools/voicevox/runtime is assembled by tools/voicevox/fetch-runtime.ps1 (.sh) and stays out of
// the repository. Only the files of the platform being packaged are copied, and since jpackage
// cannot cross-compile, the host platform is always the platform being built for.

val voicevoxRuntimeDir = rootProject.file("tools/voicevox/runtime")

val voicevoxPlatformId: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val osName = when {
        os.startsWith("win") -> "windows"
        os.startsWith("mac") -> "macos"
        else -> "linux"
    }
    val archName = when (arch) {
        "amd64", "x86_64" -> "x64"
        "aarch64", "arm64" -> "arm64"
        "x86", "i386", "i686" -> "x86"
        else -> arch
    }
    "$osName-$archName"
}

// Inside appResourcesRootDir, Compose only merges `common/` plus the directory of the OS being
// packaged into <app>/resources.
val voicevoxComposePlatformDir = voicevoxPlatformId.substringBefore('-')

val voicevoxAppResourcesDir = layout.buildDirectory.dir("voicevox-app-resources")

val prepareVoicevoxAppResources by tasks.registering(Sync::class) {
    description = "Copies the VOICEVOX runtime files for $voicevoxPlatformId into the distribution"
    group = "compose desktop"
    into(voicevoxAppResourcesDir)

    val dictionaryDir = File(voicevoxRuntimeDir, "core/dict")
    val modelsDir = File(voicevoxRuntimeDir, "models")
    val libraryDir = File(voicevoxRuntimeDir, "core/onnxruntime/lib/$voicevoxPlatformId")

    if (dictionaryDir.isDirectory && libraryDir.isDirectory) {
        // Dictionary and voice model are plain data, shared by every platform.
        from(dictionaryDir) { into("common/core/dict") }
        from(modelsDir) { into("common/models") }
        // The ONNX Runtime build is per platform; keep the <os>-<arch> directory so that
        // VoicevoxConfig resolves the same layout in a checkout and in an installed app.
        from(libraryDir) {
            into("$voicevoxComposePlatformDir/core/onnxruntime/lib/$voicevoxPlatformId")
        }
    }
}

// Compose's own `prepareAppResources` is the task that copies appResourcesRootDir into the
// distribution, so it has to wait until the files have actually been assembled. The jpackage tasks
// additionally take the assembled directory as an input, otherwise they stay UP-TO-DATE when the
// runtime files appear for the first time.
tasks.matching {
    it.name == "prepareAppResources" ||
        it.name == "createDistributable" ||
        it.name == "runDistributable" ||
        it.name.startsWith("package")
}.configureEach {
    dependsOn(prepareVoicevoxAppResources)
    inputs.dir(voicevoxAppResourcesDir).withPropertyName("voicevoxAppResources")
}

compose.desktop {
    application {
        mainClass = mainClassKt
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            packageName = "Kanji dojo CC"
            packageVersion = AppVersion.desktopAppVersion
            vendor = "huangsy"

            modules("jdk.unsupported", "java.sql")

            // VOICEVOX speech runtime (OpenJTalk dictionary + voice model + ONNX Runtime), picked up
            // by VoicevoxConfig.resolve() through `compose.application.resources.dir`. See
            // TTS-HANDOFF.md (M3).
            appResourcesRootDir.set(voicevoxAppResourcesDir)

            windows {
                upgradeUuid = "12c852a8-6e21-41a7-bd47-3bec9ff5c5df"
                iconFile.set(File("windows_icon.ico"))
                menu = true
                shortcut = true
            }

            macOS {
                bundleID = "io.github.huangsy.kanjicc"
                iconFile.set(File("mac_icon.icns"))
            }

            linux {
                val linuxIcon = File("src/jvmMain/composeResources/drawable/windowIcon.png")
                iconFile.set(linuxIcon)
            }

        }
    }
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set(mainClassKt)
    javaLauncher
}

// The VOICEVOX runtime files are not part of the repository (they live in tools/voicevox/runtime
// and are assembled by tools/voicevox/fetch-runtime.ps1); tell the development run where they are
// instead of relying on the process working directory. Packaged builds resolve them themselves
// (see VoicevoxConfig.resolve and TTS-HANDOFF.md, M3).
tasks.withType<JavaExec>().configureEach {
    systemProperty("kanjidojo.voicevox.dir", rootProject.file("tools/voicevox/runtime").absolutePath)
}

compose.resources {
    generateResClass = always
    packageOfResClass = "ua.syt0r.kanji.desktopApp"
}

aboutLibraries {
    configPath = "core/credits"
    excludeFields = arrayOf("generated")
}
