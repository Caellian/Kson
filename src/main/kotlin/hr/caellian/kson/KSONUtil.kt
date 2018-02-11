package hr.caellian.kson

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object KSONUtil {
    val prettyGson = GsonBuilder().setPrettyPrinting().create()
    val uglyGson = Gson()

    enum class MessageType {
        Info,
        Debug,
        Warning,
        Error,
        Fatal
    }

    /**
     * Function used for logging.
     * Override if you use a custom logging library.
     */
    var loggerBinder: (MessageType, String) -> Unit = { messageType: MessageType, s: String ->
        println("[${messageType.name}]\t$s")

        if (messageType == MessageType.Fatal) System.exit(1)
    }

    fun serialize(path: String, toSerialize: Any, compression: Boolean = false) {
        if (File(path).parentFile?.exists() == false) File(path).parentFile.mkdirs()

        val writer: OutputStreamWriter?
        if (compression) {
            writer = OutputStreamWriter(GZIPOutputStream(FileOutputStream(path)), "UTF-8")
            uglyGson.toJson(toSerialize, writer)
        } else {
            writer = OutputStreamWriter(FileOutputStream(path), "UTF-8")
            prettyGson.toJson(toSerialize, writer)
        }
        writer.close()
    }

    inline fun <reified T : Any> deserialize(path: String, decompress: Boolean = false): T? {
        if (!File(path).exists()) return null

        var reader: InputStreamReader = if (decompress) {
            try {
                InputStreamReader(GZIPInputStream(FileInputStream(path)), "UTF-8")
            } catch (e: Throwable) {
                loggerBinder(MessageType.Warning, "File '$path' is not in GZIP format!")
                InputStreamReader(FileInputStream(path), "UTF-8")
            }

        } else {
            InputStreamReader(FileInputStream(path), "UTF-8")
        }

        var result: T?
        try {
            result = uglyGson.fromJson(reader)
        } catch (e: Throwable) {
            if (!decompress) {
                loggerBinder(MessageType.Error, "Unable to deserialize file '$path', it might be compressed, trying with decompression...")
                reader.close()
                reader = InputStreamReader(GZIPInputStream(FileInputStream(path)), "UTF-8")

                try {
                    result = uglyGson.fromJson(reader)
                } catch (e: Throwable) {
                    loggerBinder(MessageType.Error, "Unable to deserialize file '$path'!")
                    reader.close()
                    return null
                }

                loggerBinder(MessageType.Warning, "File '$path' is compressed! Set 'decompress' parameter to true!")
                reader.close()
                return result
            } else {
                loggerBinder(MessageType.Error, "Unable to deserialize file '$path'!")
                reader.close()
                return null
            }
        }

        return result
    }

    inline fun <reified T : Any> deserializeOrDefault(path: String, default: T, decompress: Boolean = false): T {
        return deserialize(path, decompress) ?: default
    }
}
