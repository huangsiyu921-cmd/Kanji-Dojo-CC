package ua.syt0r.kanji.core.tts

import jp.hiroshiba.voicevoxcore.blocking.Onnxruntime
import jp.hiroshiba.voicevoxcore.blocking.OpenJtalk
import jp.hiroshiba.voicevoxcore.blocking.Synthesizer
import jp.hiroshiba.voicevoxcore.blocking.VoiceModelFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**
 * Runtime files and synthesis settings for the bundled VOICEVOX CORE engine.
 *
 * The layout mirrors what VOICEVOX's own `download-*` tools produce, with one directory per
 * platform for the ONNX Runtime build — the library name only depends on the OS, so the CPU
 * architecture has to live in the path for several platforms to coexist in one checkout:
 *
 * ```
 * <baseDir>/core/onnxruntime/lib/<platform>/voicevox_onnxruntime.dll   (windows)
 *                                          libvoicevox_onnxruntime.so   (linux)
 *                                          libvoicevox_onnxruntime.dylib(macos)
 * <baseDir>/core/dict/open_jtalk_dic_utf_8-1.11/   ~102 MB, platform independent
 * <baseDir>/models/4.vvm                           ~55 MB, platform independent
 * ```
 *
 * `<platform>` is `<os>-<arch>` (`windows-x64`, `linux-arm64`, `macos-x64`, …), the same naming the
 * `voicevoxcore-*.jar` uses for its bundled class library. `tools/voicevox/fetch-runtime.ps1`
 * assembles such a directory, `VoicevoxConfig.resolve()` finds it.
 */
data class VoicevoxConfig(
    val baseDir: File,
    /** 玄野武宏 / ノーマル. See TTS-HANDOFF.md for the (mandatory) credits. */
    val styleId: Int = 11,
    val speedScale: Double = 1.0,
    val pitchScale: Double = 0.0,
    val intonationScale: Double = 1.0,
    val prePhonemeLength: Double = 0.10,
    /**
     * Trailing silence. VOICEVOX defaults to 0.10, which makes single-mora words («あ», «ん») sound
     * cut off; the spike settled on 0.50 for exactly this reason.
     */
    val postPhonemeLength: Double = 0.50
) {

    /**
     * VOICEVOX's own ONNX Runtime build — *not* the one that ships with, say, `onnxruntime-*.jar`,
     * and the one file the `voicevoxcore` jar does not provide for us.
     */
    val onnxRuntimeLibrary: File
        get() = File(baseDir, "core/onnxruntime/lib/$platformId/$onnxRuntimeLibraryName")

    val dictionaryDir: File get() = File(baseDir, "core/dict/open_jtalk_dic_utf_8-1.11")

    val modelFile: File get() = File(baseDir, "models/4.vvm")

    /**
     * Identifies the synthesis settings that produced cached audio. The cache lives in a directory
     * named after this, so changing a setting starts from an empty cache instead of replaying audio
     * that no longer matches.
     */
    val cacheDirectoryName: String
        get() = listOf(
            styleId, speedScale, pitchScale, intonationScale, prePhonemeLength, postPhonemeLength
        ).joinToString("-")

    fun isComplete(): Boolean =
        onnxRuntimeLibrary.isFile && dictionaryDir.isDirectory && modelFile.isFile

    companion object {

        /** Where the runtime files live. Set by the build/launch scripts, wins over everything else. */
        const val BASE_DIR_PROPERTY = "kanjidojo.voicevox.dir"

        /** Same, for launchers that would rather set an environment variable. */
        const val BASE_DIR_ENV = "KANJIDOJO_VOICEVOX_DIR"

        /** `<os>-<arch>`, e.g. `windows-x64`, `linux-arm64`, `macos-arm64`. */
        val platformId: String = platformId(
            osName = System.getProperty("os.name").orEmpty(),
            osArch = System.getProperty("os.arch").orEmpty()
        )

        /** The file name VOICEVOX publishes the ONNX Runtime build under, per OS. */
        val onnxRuntimeLibraryName: String = when {
            platformId.startsWith("windows") -> "voicevox_onnxruntime.dll"
            platformId.startsWith("macos") -> "libvoicevox_onnxruntime.dylib"
            else -> "libvoicevox_onnxruntime.so"
        }

        /**
         * Locates a usable runtime directory: explicit setting first, then the usual places so that
         * `./gradlew :desktopApp:run` works from a fresh checkout.
         */
        fun resolve(): VoicevoxConfig {
            val explicit = System.getProperty(BASE_DIR_PROPERTY)
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv(BASE_DIR_ENV)?.takeIf { it.isNotBlank() }
            if (explicit != null) return VoicevoxConfig(File(explicit))

            val candidates = candidateBaseDirs()
            val complete = candidates.firstOrNull { VoicevoxConfig(it).isComplete() }
            return VoicevoxConfig(complete ?: candidates.first())
        }

        /** Set by Compose Desktop in a packaged app: points at `<app>/resources`. */
        private const val APP_RESOURCES_PROPERTY = "compose.application.resources.dir"

        private fun candidateBaseDirs(): List<File> {
            val userDir = File(System.getProperty("user.dir") ?: ".")
            val home = File(System.getProperty("user.home") ?: ".")
            return buildList {
                // A packaged desktop app bundles the runtime files and tells us where they are.
                System.getProperty(APP_RESOURCES_PROPERTY)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { add(File(it)) }
                // Running from a checkout (repo root, or a module directory below it).
                add(File(userDir, "tools/voicevox/runtime"))
                add(File(userDir, "../tools/voicevox/runtime"))
                // A manually installed copy of the runtime files.
                add(File(home, ".kanji-dojo-cc/voicevox"))
                // Leftovers of the M1 spike environment, kept as a last resort on Windows.
                add(File("D:\\voicevox-spike"))
            }
        }

        private fun platformId(osName: String, osArch: String): String {
            val os = when {
                osName.startsWith("Win") -> "windows"
                osName.startsWith("Mac") -> "macos"
                osName.startsWith("Linux") -> "linux"
                else -> osName.lowercase()
            }
            val arch = when (osArch.lowercase()) {
                "amd64", "x86_64" -> "x64"
                "x86", "i386", "i686" -> "x86"
                "aarch64", "arm64" -> "arm64"
                else -> osArch.lowercase()
            }
            return "$os-$arch"
        }

    }

}

/**
 * Offline Japanese word pronunciation for Desktop/JVM (Windows, Linux and macOS), backed by the
 * VOICEVOX CORE engine.
 *
 * Replaces the OS-voice based [JavaWordTtsManager] on the happy path; that implementation is kept
 * as [fallback] so a broken/missing engine degrades to the old behaviour (system voice, then kana
 * clips) instead of going silent.
 */
class VoicevoxJvmTtsManager(
    private val fallback: WordTtsManager,
    private val config: VoicevoxConfig = VoicevoxConfig.resolve(),
    private val cache: WordTtsCache? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WordTtsManager {

    /** The engine is heavy (≈750ms) to build and its native side is not thread safe. */
    private val engineMutex = Mutex()

    private val playbackLock = Any()

    @Volatile
    private var engine: VoicevoxEngine? = null

    /** Set when the native engine cannot be built at all, so we stop retrying on every tap. */
    @Volatile
    private var engineUnavailable = false

    @Volatile
    private var activeClip: Clip? = null

    override suspend fun isAvailable(): Boolean = config.isComplete()

    override val unavailableMessage: String
        get() = "The bundled Japanese voice is not installed. Expected the VOICEVOX runtime " +
            "files for ${VoicevoxConfig.platformId} in ${config.baseDir}."

    override suspend fun speak(word: String) {
        if (word.isBlank()) return

        val wav = try {
            // A cached utterance is played instantly; only a miss costs a synthesis.
            cached(word) ?: withContext(dispatcher) { synthesize(word) }.also { store(word, it) }
        } catch (cancellation: CancellationException) {
            // Cancellation is not a failure; swallowing it would fall back to the OS voice whenever
            // the screen (or the next auto-play) cancels the calling coroutine.
            throw cancellation
        } catch (e: Exception) {
            speakWithSystemVoice(word)
            return
        }

        try {
            // Deliberately not tied to the caller's lifetime, so leaving the screen mid-word does
            // not cut the audio off.
            withContext(NonCancellable) { play(wav) }
        } catch (e: Exception) {
            speakWithSystemVoice(word)
        }
    }

    override suspend fun preCache(word: String): Boolean {
        if (word.isBlank()) return false

        if (cached(word) != null) return true

        return try {
            store(word, withContext(dispatcher) { synthesize(word) })
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun cached(word: String): ByteArray? =
        cache?.let { runCatching { it.get(word) }.getOrNull() }

    private suspend fun store(word: String, wav: ByteArray) {
        cache?.let { runCatching { it.put(word, wav) } }
    }

    private suspend fun speakWithSystemVoice(word: String) {
        runCatching { withContext(NonCancellable) { fallback.speak(word) } }
    }

    private suspend fun synthesize(word: String): ByteArray = engineMutex.withLock {
        check(!engineUnavailable) { "VOICEVOX engine is not available on this machine" }

        val existing = engine
        if (existing != null) return@withLock existing.synthesize(word)

        val created = try {
            VoicevoxEngine(config)
        } catch (e: Exception) {
            engineUnavailable = true
            throw e
        }
        engine = created
        created.synthesize(word)
    }

    private suspend fun play(wav: ByteArray) {
        val clip = AudioSystem.getClip()
        clip.open(AudioSystem.getAudioInputStream(ByteArrayInputStream(wav)))

        replaceActiveClip(clip)
        try {
            awaitPlayback(clip)
        } finally {
            clearActiveClip(clip)
            runCatching { clip.close() }
        }
    }

    /**
     * Starts [clip] as the only playing one, mirroring Android's `QUEUE_FLUSH`: a second tap on 🔊
     * interrupts the previous word instead of talking over it.
     */
    private fun replaceActiveClip(clip: Clip) {
        val previous = synchronized(playbackLock) {
            val previous = activeClip
            activeClip = clip
            previous
        }
        previous?.let { runCatching { it.stop() } }
    }

    private fun clearActiveClip(clip: Clip) {
        synchronized(playbackLock) {
            if (activeClip === clip) activeClip = null
        }
    }

    /**
     * Starts [clip] and suspends until it has really been played.
     *
     * The listener must be attached *before* [Clip.start]: a clip starts asynchronously, so right
     * after that call `isRunning`/`isActive` are still false and using them as a completion check
     * closes the line before a single sample is played.
     */
    private suspend fun awaitPlayback(clip: Clip): Unit = suspendCancellableCoroutine { continuation ->
        clip.addLineListener { event ->
            if (event.type == LineEvent.Type.STOP && continuation.isActive) {
                continuation.resume(Unit)
            }
        }
        clip.start()
        // An empty clip never emits STOP on its own; anything else is stopped by the listener above.
        if (clip.framePosition >= clip.frameLength && continuation.isActive) {
            continuation.resume(Unit)
        }
    }

}

/**
 * Owns the native VOICEVOX objects for one process. Native handles are never freed explicitly —
 * they live as long as the app does, which keeps repeatedly tapping 🔊 cheap.
 */
private class VoicevoxEngine(private val config: VoicevoxConfig) {

    private val synthesizer: Synthesizer = run {
        // The JVM API unpacks its own native library from the jar and loads it via System.load().
        val onnxRuntime = Onnxruntime.loadOnce()
            .filename(config.onnxRuntimeLibrary.absolutePath)
            .perform()
        val openJtalk = OpenJtalk(config.dictionaryDir.absolutePath)
        Synthesizer.builder(onnxRuntime, openJtalk).build().also { synthesizer ->
            synthesizer.loadVoiceModel(VoiceModelFile(config.modelFile.absolutePath)).perform()
        }
    }

    fun synthesize(word: String): ByteArray {
        // Two-phase API: the one-shot `tts` does not expose the silence/length settings we need.
        val query = synthesizer.createAudioQuery(word, config.styleId).apply {
            speedScale = config.speedScale
            pitchScale = config.pitchScale
            intonationScale = config.intonationScale
            prePhonemeLength = config.prePhonemeLength
            postPhonemeLength = config.postPhonemeLength
        }
        return synthesizer.synthesis(query, config.styleId).perform()
    }

}
