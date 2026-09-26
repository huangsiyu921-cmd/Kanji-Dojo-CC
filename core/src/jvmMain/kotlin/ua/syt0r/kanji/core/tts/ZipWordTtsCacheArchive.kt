package ua.syt0r.kanji.core.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * [WordTtsCacheArchive] backed by a zip file in the user's home — the desktop twin of the class in
 * `androidMain`, which uses a location the user can reach over USB.
 */
class ZipWordTtsCacheArchive(
    private val archiveFile: File
) : WordTtsCacheArchive {

    override val location: String get() = archiveFile.absolutePath

    override suspend fun export(cache: WordTtsCache): Int = withContext(Dispatchers.IO) {
        runCatching {
            val entries = cache.entries()
            archiveFile.parentFile?.mkdirs()
            ZipOutputStream(archiveFile.outputStream()).use { zip ->
                entries.forEach { entry ->
                    val wav = cache.get(entry.word) ?: return@forEach
                    zip.putNextEntry(ZipEntry(encode(entry.word)))
                    zip.write(wav)
                    zip.closeEntry()
                }
            }
            entries.size
        }.getOrDefault(-1)
    }

    override suspend fun import(cache: WordTtsCache): Int = withContext(Dispatchers.IO) {
        if (!archiveFile.isFile) return@withContext -1

        runCatching {
            var restored = 0
            ZipInputStream(archiveFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        cache.put(decode(entry.name), zip.readBytes())
                        restored++
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            restored
        }.getOrDefault(-1)
    }

    private fun encode(word: String): String =
        URLEncoder.encode(word, Charsets.UTF_8.name()) + SUFFIX

    private fun decode(entryName: String): String =
        runCatching { URLDecoder.decode(entryName.removeSuffix(SUFFIX), Charsets.UTF_8.name()) }
            .getOrDefault(entryName.removeSuffix(SUFFIX))

    private companion object {

        const val SUFFIX = ".wav"

    }

}
