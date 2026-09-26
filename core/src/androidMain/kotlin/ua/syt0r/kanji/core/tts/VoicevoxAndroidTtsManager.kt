package ua.syt0r.kanji.core.tts

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import jp.hiroshiba.voicevoxcore.blocking.Onnxruntime
import jp.hiroshiba.voicevoxcore.blocking.OpenJtalk
import jp.hiroshiba.voicevoxcore.blocking.Synthesizer
import jp.hiroshiba.voicevoxcore.blocking.VoiceModelFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

/**
 * Offline Japanese word pronunciation for Android, backed by the same VOICEVOX CORE engine as the
 * desktop build (see `VoicevoxJvmTtsManager`).
 *
 * The two differ in how the engine is fed and how the audio comes out:
 *  * Android's native libraries live in `jniLibs`, so ONNX Runtime is loaded through the class
 *    loader first (with `extractNativeLibs=false` it stays inside the APK, where the by-name
 *    `dlopen` that VOICEVOX does internally cannot find it);
 *  * the OpenJTalk dictionary and the voice model ship as APK assets and have to be unpacked into
 *    the app's private storage on first use, because VOICEVOX opens them by file path;
 *  * playback goes through [MediaPlayer] rather than `javax.sound.sampled` (not on Android) or a
 *    raw [android.media.AudioTrack] — the latter never started playback on at least one device.
 *
 * Anything that fails falls back to [fallback] (the system voice) rather than going silent.
 * Failures are logged under the `VoicevoxTts` tag (`adb logcat -s VoicevoxTts`).
 */
class VoicevoxAndroidTtsManager(
    context: Context,
    private val fallback: WordTtsManager,
    private val cache: WordTtsCache? = null
) : WordTtsManager {

    private val appContext = context.applicationContext

    /** The engine is heavy to build and its native side is not thread safe. */
    private val engineMutex = Mutex()

    private val playbackLock = Any()

    @Volatile
    private var engine: AndroidVoicevoxEngine? = null

    /** Set when the engine cannot be built at all, so we stop retrying on every tap. */
    @Volatile
    private var engineUnavailable = false

    @Volatile
    private var activePlayer: MediaPlayer? = null

    private val preparingCount = AtomicInteger(0)
    private val _isPreparing = MutableStateFlow(false)
    override val isPreparing: StateFlow<Boolean> = _isPreparing

    /** The assets always ship with the APK, so a voice is always there in principle. */
    override suspend fun isAvailable(): Boolean = true

    override val unavailableMessage: String
        get() = "The bundled Japanese voice could not be loaded on this device."

    override suspend fun speak(word: String) {
        if (word.isBlank()) return

        val wav = try {
            // A cached utterance is played instantly; only a miss costs a synthesis.
            cached(word) ?: synthesizeTracking(word).also { store(word, it) }
        } catch (cancellation: CancellationException) {
            // Cancellation is not a failure. Swallowing it would fall back to the system voice
            // whenever the screen (or the next card) cancels the calling coroutine.
            throw cancellation
        } catch (t: Throwable) {
            // Throwable, not Exception: a failed System.loadLibrary/dlopen surfaces as an Error.
            Log.e(TAG, "could not synthesize '$word', using the system voice", t)
            speakWithSystemVoice(word)
            return
        }
        Log.i(TAG, "synthesized '$word' (${wav.size} bytes)")

        try {
            // Deliberately not tied to the caller's lifetime: leaving the screen mid-word (or the
            // next auto-play replacing this one) must not cut the audio off.
            withContext(NonCancellable) { play(wav) }
        } catch (t: Throwable) {
            Log.e(TAG, "could not play '$word', using the system voice", t)
            speakWithSystemVoice(word)
        }
    }

    override suspend fun preCache(word: String): Boolean {
        if (word.isBlank()) return false

        if (cached(word) != null) return true

        return try {
            store(word, synthesizeTracking(word))
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Log.e(TAG, "could not pre-cache '$word'", t)
            false
        }
    }

    /** Keeps [isPreparing] true exactly while the synthesizer runs, so screens can show a spinner. */
    private suspend fun synthesizeTracking(word: String): ByteArray {
        preparingCount.incrementAndGet()
        _isPreparing.value = true
        try {
            return withContext(Dispatchers.IO) { synthesize(word) }
        } finally {
            if (preparingCount.decrementAndGet() <= 0) _isPreparing.value = false
        }
    }

    private suspend fun cached(word: String): ByteArray? =
        cache?.let { runCatching { it.get(word) }.getOrNull() }

    private suspend fun store(word: String, wav: ByteArray) {
        cache?.let { runCatching { it.put(word, wav) } }
    }

    private suspend fun speakWithSystemVoice(word: String) {
        runCatching { withContext(NonCancellable) { fallback.speak(word) } }
            .onFailure { Log.e(TAG, "the system voice failed as well", it) }
    }

    private suspend fun synthesize(word: String): ByteArray = engineMutex.withLock {
        check(!engineUnavailable) { "VOICEVOX engine is not available on this device" }

        val existing = engine
        if (existing != null) return@withLock existing.synthesize(word)

        val created = try {
            AndroidVoicevoxEngine(appContext)
        } catch (t: Throwable) {
            engineUnavailable = true
            throw t
        }
        engine = created
        created.synthesize(word)
    }

    private suspend fun play(wav: ByteArray) {
        // A unique file per utterance: two players must never end up reading the same file.
        val file = File.createTempFile("voicevox-", ".wav", appContext.cacheDir)
        try {
            file.writeBytes(wav)

            val finished = CompletableDeferred<Result<Unit>>()
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener { finished.complete(Result.success(Unit)) }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                finished.complete(
                    Result.failure(IllegalStateException("MediaPlayer error $what/$extra"))
                )
                true
            }

            try {
                player.prepare()
                replaceActivePlayer(player)
                player.start()
                Log.i(TAG, "playing ${wav.size} bytes (duration=${player.duration}ms)")

                val timeout = player.duration.toLong() + PLAYBACK_TIMEOUT_SLACK_MS
                val result = withTimeoutOrNull(timeout) { finished.await() }
                when {
                    result == null -> Log.w(TAG, "playback did not finish within ${timeout}ms")
                    result.isFailure -> throw result.exceptionOrNull()!!
                }
            } finally {
                clearActivePlayer(player)
                runCatching { player.release() }
            }
        } finally {
            file.delete()
        }
    }

    /**
     * Makes [player] the only playing one, mirroring Android's `TextToSpeech.QUEUE_FLUSH`: tapping
     * 🔊 again interrupts the previous word instead of talking over it.
     */
    private fun replaceActivePlayer(player: MediaPlayer) {
        val previous = synchronized(playbackLock) {
            val previous = activePlayer
            activePlayer = player
            previous
        }
        previous?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
    }

    private fun clearActivePlayer(player: MediaPlayer) {
        synchronized(playbackLock) {
            if (activePlayer === player) activePlayer = null
        }
    }

    private companion object {

        const val TAG = "VoicevoxTts"
        const val PLAYBACK_TIMEOUT_SLACK_MS = 5_000L

    }

}

/**
 * Owns the native VOICEVOX objects for one process, and unpacks the bundled runtime files on first
 * use.
 */
private class AndroidVoicevoxEngine(private val context: Context) {

    private val synthesizer: Synthesizer = run {
        val baseDir = ensureRuntimeFiles(context)
        Log.i(TAG, "building the engine from $baseDir")

        // Load it through the class loader first. With `extractNativeLibs=false` (AGP's default)
        // the library stays *inside* the APK: `System.loadLibrary` handles that, while the
        // by-name dlopen() that Onnxruntime does internally cannot see it. Loading it here means
        // that dlopen just returns the handle of the already loaded library.
        loadOnnxRuntime()

        val onnxRuntime = Onnxruntime.loadOnce()
            .filename(onnxRuntimeLibrary())
            .perform()
        val openJtalk = OpenJtalk(File(baseDir, DICTIONARY_PATH).absolutePath)
        // The lambda parameter is called `created` on purpose: naming it `synthesizer` and then
        // reading `synthesizer` as the last expression of the `run` block would return this very
        // property — still null while it is being initialized.
        Synthesizer.builder(onnxRuntime, openJtalk).build().also { created ->
            created.loadVoiceModel(
                VoiceModelFile(File(baseDir, MODEL_PATH).absolutePath)
            ).perform()
            Log.i(TAG, "engine ready")
        }
    }

    fun synthesize(word: String): ByteArray {
        val query = synthesizer.createAudioQuery(word, STYLE_ID).apply {
            // Same settled settings as the desktop build — see VoicevoxConfig there.
            speedScale = 1.0
            pitchScale = 0.0
            intonationScale = 1.0
            prePhonemeLength = 0.10
            // VOICEVOX defaults to 0.10, which makes single-mora words («あ», «ん») sound cut off.
            postPhonemeLength = 0.50
        }
        return synthesizer.synthesis(query, STYLE_ID).perform()
    }

    /** Loads the ONNX Runtime build through the class loader (see the note in the initializer). */
    private fun loadOnnxRuntime() {
        val extracted = extractedOnnxRuntimeLibrary()
        if (extracted != null) {
            Log.i(TAG, "System.load($extracted)")
            System.load(extracted)
        } else {
            Log.i(TAG, "System.loadLibrary($LIBRARY_WITHOUT_SUFFIX)")
            System.loadLibrary(LIBRARY_WITHOUT_SUFFIX)
        }
    }

    /** What `Onnxruntime.loadOnce().filename(...)` should be given. */
    private fun onnxRuntimeLibrary(): String =
        extractedOnnxRuntimeLibrary() ?: ONNX_RUNTIME_LIBRARY

    private fun extractedOnnxRuntimeLibrary(): String? {
        val nativeDir = context.applicationInfo.nativeLibraryDir ?: return null
        val file = File(nativeDir, ONNX_RUNTIME_LIBRARY)
        return if (file.isFile) file.absolutePath else null
    }

    private companion object {

        val TAG = "VoicevoxTts"

        /** 玄野武宏 / ノーマル. See TTS-HANDOFF.md for the (mandatory) credits. */
        const val STYLE_ID = 11

        const val ONNX_RUNTIME_LIBRARY = "libvoicevox_onnxruntime.so"

        /** The same library as `System.loadLibrary` sees it. */
        const val LIBRARY_WITHOUT_SUFFIX = "voicevox_onnxruntime"

        /** Both live in the APK's assets, see the assets.srcDir entries in core/build.gradle.kts. */
        const val DICTIONARY_ASSET = "open_jtalk_dic_utf_8-1.11"
        const val MODEL_ASSET = "4.vvm"

        /** Where they end up in the app's private storage, mirroring the desktop layout. */
        const val DICTIONARY_PATH = "core/dict/$DICTIONARY_ASSET"
        const val MODEL_PATH = "models/$MODEL_ASSET"

        /** Bump to force a re-extraction after changing the bundled files. */
        const val RUNTIME_VERSION = "1"
        const val READY_MARKER = ".runtime-$RUNTIME_VERSION"

        fun ensureRuntimeFiles(context: Context): File {
            val baseDir = File(context.filesDir, "voicevox")
            val dictionaryDir = File(baseDir, DICTIONARY_PATH)
            val modelFile = File(baseDir, MODEL_PATH)
            val marker = File(baseDir, READY_MARKER)

            if (marker.isFile && dictionaryDir.isDirectory && modelFile.isFile) return baseDir

            Log.i(TAG, "unpacking the voice runtime into $baseDir (first use)")
            val startedAt = System.currentTimeMillis()
            val assets = context.assets
            dictionaryDir.deleteRecursively()
            copyAssetTree(assets, DICTIONARY_ASSET, dictionaryDir)
            copyAssetTree(assets, MODEL_ASSET, modelFile)

            check(dictionaryDir.isDirectory && modelFile.isFile) {
                "the bundled voice runtime could not be unpacked into $baseDir"
            }
            // Written last, so an interrupted extraction is simply redone next time.
            marker.writeText(RUNTIME_VERSION)
            Log.i(TAG, "runtime unpacked in ${System.currentTimeMillis() - startedAt}ms")
            return baseDir
        }

        fun copyAssetTree(assets: AssetManager, assetPath: String, target: File) {
            val children = assets.list(assetPath).orEmpty()
            if (children.isEmpty()) {
                copyAsset(assets, assetPath, target)
                return
            }
            target.mkdirs()
            children.forEach { child ->
                copyAssetTree(assets, "$assetPath/$child", File(target, child))
            }
        }

        fun copyAsset(assets: AssetManager, assetPath: String, target: File) {
            target.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }

    }

}
