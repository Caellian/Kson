package hr.caellian.kson

import com.github.salomonbrys.kotson.*
import com.google.gson.GsonBuilder
import java.io.*

class Configuration(cloned: Map<String, Property<out Any>>? = null): MutableMap<String, Any> {
    private val prettyGson = GsonBuilder()
            .registerTypeAdapter(Property.serializer)
            .registerTypeAdapter(Property.deserializer)
            .setPrettyPrinting()
            .create()!!

    private var internal = LinkedHashMap<String, Property<out Any>>()

    init {
        cloned?.forEach{ internal[it.key] = it.value }
    }

    override val size: Int
        get() = internal.size

    override fun containsKey(key: String): Boolean {
        return internal.containsKey(key)
    }

    override fun containsValue(value: Any): Boolean {
        return internal.containsValue(Property(value))
    }

    override fun isEmpty(): Boolean {
        return internal.isEmpty()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
        get() = internal.entries.map { Node(it.key, it.value.value) }.toMutableSet()
    override val keys: MutableSet<String>
        get() = internal.keys
    override val values: MutableCollection<Any>
        get() = internal.values.map { it.value }.toMutableSet()

    override fun clear() {
        internal.clear()
    }

    override fun get(key: String): Any? {
        return internal[key]?.value ?: Configuration().also {
            it.putAll(internal
                    .filter {it.key.contains('.') && it.key.length > key.length + 1 && it.key.substring(0, key.length) == key }
                    .map { it.key.substring(key.length + 1) to it.value })
        }
    }

    override fun put(key: String, value: Any): Any? {
        return if (value is Configuration) {
            value.forEach { k, v -> put("$key.$k", v) }
            this
        } else {
            internal.put(key, Property(value))?.value
        }

    }

    override fun putAll(from: Map<out String, Any>) {
        internal.putAll(from.map { e -> e.key to Property(e.value) })
    }

    override fun remove(key: String): Any? {
        return internal.remove(key)
    }

    fun save(path: File): Configuration {
        if (path.parentFile?.exists() == false) path.parentFile.mkdirs()

        val writer = OutputStreamWriter(FileOutputStream(path), "UTF-8")
        prettyGson.toJson(internal, writer)
        writer.close()

        return this
    }

    fun load(path: File): Configuration {
        if (!path.exists()) return this

        val reader = InputStreamReader(FileInputStream(path), "UTF-8")
        internal = prettyGson.fromJson(reader)
        reader.close()

        return this
    }

    override fun toString(): String {
        return "{${internal.entries.joinToString(", ")}}"
    }

    class Node(override val key: String, override var value: Any) : MutableMap.MutableEntry<String, Any> {
        override fun setValue(newValue: Any): Any {
            val old = value
            value = newValue
            return old
        }

        override fun toString(): String {
            return "$key: $value"
        }
    }
}