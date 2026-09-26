package ua.syt0r.kanji.core.tts

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import jp.hiroshiba.voicevoxcore.blocking.Onnxruntime
import jp.hiroshiba.voicevoxcore.blocking.OpenJtalk
import jp.hiroshiba.voicevoxcore.blocking.Synthesizer
import jp.hiroshiba.voicevoxcore.blocking.VoiceModelFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Offline Japanese word pronunciation for Android, backed by the same VOICEVOX CORE engine as the
 * desktop build (see `VoicevoxJvmTtsManager`).
 *
 * The two differ in how the engine is fed:
 *  * Android's native library lives in `jniLibs`, so [Onnxruntime] gets a plain library name and
 *    dlopen resolves it, while on the desktop a path to the unpacked `.dll`/`.so`/`.dylib` is used;
 *  * the OpenJTalk dictionary and the voice model ship as APK assets and have to be unpacked into
 *    the app's private storage on first use, because VOICEVOX opens them by file path.
 *
 * Audio is written straight to an [AudioTrack] instead of `javax.sound.sampled` (not available on
 * Android). Anything that fails falls back to [fallback] (the system voice) rather than going
 * silent.
 */
class VoicevoxAndroidTtsManager(
    context: Context,
    private val fallback: WordTtsManager
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
    private var activeTrack: AudioTrack? = null

    /** The assets always ship with the APK, so a voice is always there in principle. */
    override suspend fun isAvailable(): Boolean = true

    override val unavailableMessage: String
        get() = "The bundled Japanese voice could not be loaded on this device."

    override suspend fun speak(word: String) {
        if (word.isBlank()) return

        val wav = try {
            withContext(Dispatchers.IO) { synthesize(word) }
        } catch (e: Exception) {
            fallback.speak(word)
            return
        }

        try {
            play(wav)
        } catch (e: Exception) {
            fallback.speak(word)
        }
    }

    private suspend fun synthesize(word: String): ByteArray = engineMutex.withLock {
        check(!engineUnavailable) { "VOICEVOX engine is not available on this device" }

        val existing = engine
        if (existing != null) return@withLock existing.synthesize(word)

        val created = try {
            AndroidVoicevoxEngine(appContext)
        } catch (e: Exception) {
            engineUnavailable = true
            throw e
        }
        engine = created
        created.synthesize(word)
    }

    private suspend fun play(wav: ByteArray) {
        val pcm = wavToPcm(wav)
        if (pcm.isEmpty()) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBufferSize, PLAYBACK_BUFFER_BYTES))
            .build()

        replaceActiveTrack(track)
        try {
            val finished = CompletableDeferred<Unit>()
            track.setNotificationMarkerPosition(pcm.size / BYTES_PER_FRAME)
            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: AudioTrack) {
                        finished.complete(Unit)
                    }

                    override fun onPeriodicNotification(track: AudioTrack) = Unit
                },
                Handler(Looper.getMainLooper())
            )

            track.play()
            track.write(pcm, 0, pcm.size)
            withTimeoutOrNull(PLAYBACK_TIMEOUT_MS) { finished.await() }
        } finally {
            clearActiveTrack(track)
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    /**
     * Makes [track] the only playing one, mirroring Android's `TextToSpeech.QUEUE_FLUSH`: tapping
     * 🔊 again interrupts the previous word instead of talking over it.
     */
    private fun replaceActiveTrack(track: AudioTrack) {
        val previous = synchronized(playbackLock) {
            val previous = activeTrack
            activeTrack = track
            previous
        }
        previous?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
    }

    private fun clearActiveTrack(track: AudioTrack) {
        synchronized(playbackLock) {
            if (activeTrack === track) activeTrack = null
        }
    }

    private companion object {

        const val SAMPLE_RATE = 24000
        const val BYTES_PER_FRAME = 2
        const val PLAYBACK_BUFFER_BYTES = 64 * 1024
        const val PLAYBACK_TIMEOUT_MS = 30_000L

        /** Skips the RIFF header so the samples can be handed to [AudioTrack] directly. */
        fun wavToPcm(wav: ByteArray): ByteArray {
            var position = 12
            while (position + 8 <= wav.size) {
                val id = String(wav, position, 4, Charsets.US_ASCII)
                val size = readIntLe(wav, position + 4)
                if (id == "data") {
                    val start = position + 8
                    val end = minOf(start + size, wav.size)
                    return if (start >= end) ByteArray(0) else wav.copyOfRange(start, end)
                }
                position += 8 + size + (size and 1)
            }
            return wav
        }

        fun readIntLe(bytes: ByteArray, offset: Int): Int =
            (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    }

}

/**
 * Owns the native VOICEVOX objects for one process, and unpacks the bundled runtime files on first
 * use.
 */
private class AndroidVoicevoxEngine(context: Context) {

    private val synthesizer: Synthesizer = run {
        val baseDir = ensureRuntimeFiles(context)

        // The AAR puts libvoicevox_onnxruntime.so into jniLibs; dlopen resolves a plain name.
        val onnxRuntime = Onnxruntime.loadOnce()
            .filename(ONNX_RUNTIME_LIBRARY)
            .perform()
        val openJtalk = OpenJtalk(File(baseDir, DICTIONARY_PATH).absolutePath)
        Synthesizer.builder(onnxRuntime, openJtalk).build().also { synthesizer ->
            synthesizer.loadVoiceModel(
                VoiceModelFile(File(baseDir, MODEL_PATH).absolutePath)
            ).perform()
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

    private companion object {

        /** 玄野武宏 / ノーマル. See TTS-HANDOFF.md for the (mandatory) credits. */
        const val STYLE_ID = 11

        const val ONNX_RUNTIME_LIBRARY = "libvoicevox_onnxruntime.so"

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
            if (File(baseDir, READY_MARKER).isFile) return baseDir

            val assets = context.assets
            copyAssetTree(assets, DICTIONARY_ASSET, File(baseDir, DICTIONARY_PATH))
            copyAssetTree(assets, MODEL_ASSET, File(baseDir, MODEL_PATH))
            // Written last, so an interrupted extraction is simply redone next time.
            File(baseDir, READY_MARKER).writeText(RUNTIME_VERSION)
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
