package ua.syt0r.kanji.core.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * File backed [WordTtsCache]: one `.wav` per word, named after the url-encoded word so the cache
 * screen can list what is inside without a separate index.
 *
 * Totals are kept under [maxBytes] by dropping the least recently used files on write, so a long
 * lived installation cannot grow without bound. The Android target has an identical twin in
 * `androidMain` — keep the two in sync.
 */
class FileWordTtsCache(
    private val directory: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES
) : WordTtsCache {

    /** Serializes writes and the LRU sweep; reads and listings are lock free. */
    private val writeLock = ReentrantLock()

    @Volatile
    private var cachedStats: WordTtsCacheStats? = null

    override suspend fun get(word: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = fileFor(word)
        if (!file.isFile) return@withContext null

        // Touching the file keeps it away from the LRU sweep.
        file.setLastModified(System.currentTimeMillis())
        runCatching { file.readBytes() }.getOrNull()
    }

    override suspend fun put(word: String, wav: ByteArray): Unit = withContext(Dispatchers.IO) {
        writeLock.withLock {
            runCatching {
                directory.mkdirs()
                fileFor(word).writeBytes(wav)
                sweep()
            }
        }
        Unit
    }

    override suspend fun stats(): WordTtsCacheStats = withContext(Dispatchers.IO) {
        cachedStats?.let { return@withContext it }
        val files = files()
        WordTtsCacheStats(
            entries = files.size,
            sizeBytes = files.sumOf { it.length() }
        ).also { cachedStats = it }
    }

    override suspend fun entries(): List<WordTtsCacheEntry> = withContext(Dispatchers.IO) {
        files()
            .sortedByDescending { it.lastModified() }
            .map { file -> WordTtsCacheEntry(word = wordOf(file), sizeBytes = file.length()) }
    }

    override suspend fun remove(word: String) {
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                runCatching { fileFor(word).delete() }
            }
        }
        cachedStats = null
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                runCatching { files().forEach { it.delete() } }
            }
        }
        cachedStats = WordTtsCacheStats(entries = 0, sizeBytes = 0)
    }

    private fun files(): List<File> =
        directory.listFiles { file -> file.isFile && file.name.endsWith(EXTENSION) }
            ?.toList()
            .orEmpty()

    private fun fileFor(word: String): File =
        File(directory, encode(word) + EXTENSION)

    private fun wordOf(file: File): String =
        decode(file.name.removeSuffix(EXTENSION))

    /** Drops the least recently used entries until the directory fits into [maxBytes]. */
    private fun sweep() {
        val files = files()
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) {
            cachedStats = null
            return
        }

        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= maxBytes) return@forEach
            total -= file.length()
            file.delete()
        }
        cachedStats = null
    }

    private fun encode(word: String): String =
        URLEncoder.encode(word, Charsets.UTF_8.name())

    private fun decode(name: String): String =
        runCatching { URLDecoder.decode(name, Charsets.UTF_8.name()) }.getOrDefault(name)

    private companion object {

        const val EXTENSION = ".wav"
        const val DEFAULT_MAX_BYTES = 256L * 1024 * 1024

    }

}
