package ua.syt0r.kanji.core.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * File backed [WordTtsCache]: one `.wav` per utterance, named after a hash of its key.
 *
 * Totals are kept under [maxBytes] by dropping the least recently used files on write, so a long
 * lived installation cannot grow without bound. The same implementation is used by the Android
 * target (see `androidMain`), where the directory is the app's private storage.
 */
class FileWordTtsCache(
    private val directory: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES
) : WordTtsCache {

    /** Serializes writes and the LRU sweep; reads are lock free. */
    private val writeLock = ReentrantLock()

    @Volatile
    private var cachedStats: WordTtsCacheStats? = null

    override suspend fun get(key: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        if (!file.isFile) return@withContext null

        // Touching the file keeps it away from the LRU sweep.
        file.setLastModified(System.currentTimeMillis())
        runCatching { file.readBytes() }.getOrNull()
    }

    override suspend fun put(key: String, wav: ByteArray): Unit = withContext(Dispatchers.IO) {
        writeLock.withLock {
            runCatching {
                directory.mkdirs()
                fileFor(key).writeBytes(wav)
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

    private fun fileFor(key: String): File =
        File(directory, hash(key) + EXTENSION)

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

    private fun hash(key: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
            .take(32)

    private companion object {

        const val EXTENSION = ".wav"
        const val DEFAULT_MAX_BYTES = 256L * 1024 * 1024

    }

}
