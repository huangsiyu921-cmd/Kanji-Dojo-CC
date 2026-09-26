package ua.syt0r.kanji.core.tts

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.system.measureTimeMillis

/**
 * End-to-end check of the M1 desktop engine: a real synthesis + playback round trip through the
 * bundled VOICEVOX CORE runtime.
 *
 * The runtime files are still referenced from the spike environment (see TTS-HANDOFF.md), so the
 * test silently skips when they are absent instead of failing every machine that lacks them.
 */
class VoicevoxJvmTtsManagerTest {

    private val config = VoicevoxConfig.default()

    private class RecordingFallback : WordTtsManager {

        var speakCalls = 0

        override suspend fun speak(word: String) {
            speakCalls++
        }

        override suspend fun isAvailable(): Boolean = false

        override val unavailableMessage: String = "not used in tests"

    }

    @Test
    fun synthesizesAndPlaysShortWord() = runBlocking {
        if (!config.isComplete()) return@runBlocking

        val fallback = RecordingFallback()
        val manager = VoicevoxJvmTtsManager(fallback = fallback, config = config)

        assertTrue(manager.isAvailable())

        // 「学校」 is 1.11s of audio with the settled postPhonemeLength. Waiting for playback is the
        // only way to tell a real round trip from an instant bail-out.
        val elapsed = measureTimeMillis { manager.speak("学校") }

        assertEquals(0, fallback.speakCalls, "engine path failed, fell back to the OS voice")
        assertTrue(elapsed >= 900, "speak() returned after only ${elapsed}ms, audio was not played")
    }

}
