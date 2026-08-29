package ua.syt0r.kanji.core.tts

import platform.AVFoundation.AVSpeechSynthesisVoice
import platform.AVFoundation.AVSpeechSynthesizer
import platform.AVFoundation.AVSpeechUtterance
import platform.AVFoundation.voiceWithLanguage

class IosWordTtsManager : WordTtsManager {

    private val synthesizer = AVSpeechSynthesizer()

    override suspend fun isAvailable(): Boolean = AVSpeechSynthesisVoice.voiceWithLanguage("ja-JP") != null

    override val unavailableMessage: String
        get() = "No Japanese voice found on this device. Download a Japanese voice in " +
            "Settings > Accessibility > Spoken Content, then restart Kanji Dojo."

    override suspend fun speak(word: String) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(word)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("ja-JP")
        synthesizer.speakUtterance(utterance)
    }

}
