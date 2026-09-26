# Ktor fails release builds with R8 without it
-dontwarn org.slf4j.impl.StaticLoggerBinder

# VOICEVOX CORE: the Java API is bound to the native library by method name, and its data classes
# are (de)serialized with Gson. R8's shrinking must not remove any of it.
-keep class jp.hiroshiba.voicevoxcore.** { *; }

# Referenced by that library at compile time only (annotations); not needed at runtime.
-dontwarn jakarta.annotation.**
-dontwarn jakarta.validation.**