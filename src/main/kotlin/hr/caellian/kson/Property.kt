package hr.caellian.kson

import com.github.salomonbrys.kotson.*
import com.google.gson.*
import java.math.BigDecimal
import java.math.BigInteger

class Property<T>(var value: T) {

    fun getSignature(): String {
        return when (value) {
            is Byte -> "Byte"
            is Short -> "Short"
            is Int -> "Integer"
            is BigInteger -> "BigInteger"
            is Float -> "Float"
            is Double -> "Double"
            is Long -> "Long"
            is BigDecimal -> "BigDecimal"
            else -> "Handled"
        }
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        const val Signature = "kson-signature"

        val serializer = jsonSerializer<Property<*>> {
            when {
                it.src.getSignature() != "Handled" ->
                    jsonObject(
                        Signature to it.src.getSignature(),
                        "value" to it.src.value
                )
                else -> it.context.typedSerialize(it.src.value!!)
            }
        }

        val deserializer = jsonDeserializer {
            Property(when (it.json) {
                is JsonObject -> {
                    if ((it.json as JsonObject).contains(Signature)) {
                        when (it.json[Signature].asString) {
                            "Byte" -> (it.json as JsonObject)["value"].asByte
                            "Short" -> (it.json as JsonObject)["value"].asShort
                            "Integer" -> (it.json as JsonObject)["value"].asInt
                            "BigInteger" -> (it.json as JsonObject)["value"].asBigInteger
                            "Float" -> (it.json as JsonObject)["value"].asFloat
                            "Double" -> (it.json as JsonObject)["value"].asDouble
                            "Long" -> (it.json as JsonObject)["value"].asLong
                            "BigDecimal" -> (it.json as JsonObject)["value"].asBigDecimal
                            else -> it.context.deserialize((it.json as JsonObject)["value"] ?: it.json, Any::class.java)
                        }
                    } else {
                        it.context.deserialize((it.json as JsonObject)["value"] ?: it.json, Any::class.java)
                    }
                }
                else -> it.context.deserialize(it.json, Any::class.java)
            })
        }
    }
}
