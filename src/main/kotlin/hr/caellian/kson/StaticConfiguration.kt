package hr.caellian.kson

import java.io.File
import kotlin.reflect.KClass
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.security.AccessController
import java.security.PrivilegedAction

open class StaticConfiguration {
    private val config = Configuration()

    class PrivilegedAccessible(val field: Field): PrivilegedAction<Unit> {
        override fun run() {
            field.isAccessible = true
        }

    }

    fun save(path: File): StaticConfiguration {
        val instance: Any = getObjectInstance(this::class)!!

        fun handleChildFields(kClass: KClass<*>, parent: String = "") {
            kClass.java.declaredFields.filter { it.name != InstanceName }.forEach {
                val parentPrefix = if (parent.isEmpty()) "" else "$parent."
                config["$parentPrefix${it.name}"] = getFieldWithBypass(it)!!
            }
        }

        handleChildFields(instance::class)

        fun handleChildObjects(kClass: KClass<*>, parent: String = "") {
            kClass.nestedClasses.forEach{
                val parentPrefix = if (parent.isEmpty()) "" else "$parent."
                handleChildFields(it, "$parentPrefix${it.simpleName}")

                if (it.nestedClasses.isNotEmpty()) {
                    handleChildObjects(it, "$parentPrefix${it.simpleName}")
                }
            }
        }

        handleChildObjects(instance::class)

        config.save(path)
        return this
    }

    fun load(path: File): StaticConfiguration {
        config.load(path)

        val instance: Any = getObjectInstance(this::class)!!

        fun getChildClass(path: String, prevParent: KClass<*>? = instance::class): KClass<*>? {
            if (prevParent == null) return null

            return if (path.contains('.')) {
                val child = path.substring(0, path.indexOf('.'))
                val prev = prevParent.nestedClasses.firstOrNull { it.simpleName == child }
                getChildClass(path.substring(child.length + 1), prev)
            } else {
                prevParent.nestedClasses.firstOrNull { it.simpleName == path }
            }
        }

        fun setChildField(kClass: KClass<*>, field:String, value:Any) {
            getObjectInstance(kClass)?.javaClass?.declaredFields?.firstOrNull { it.name == field }?.also {
                try {
                    setFieldWithBypass(it, value)
                } catch (e: Throwable) {
                    KSONUtil.loggerBinder(KSONUtil.MessageType.Error, "Can't modify final field '$field'! This is a JRE/Reflection API bug.")
                    e.printStackTrace()
                }

            }
        }

        config.forEach { k, v ->
            if (k.contains('.')) {
                getChildClass(k.substring(0, k.lastIndexOf('.')))?.also {
                    setChildField(it, k.substring(k.lastIndexOf('.') + 1), v)
                }
            } else {
                setChildField(instance::class, k, v)
            }
        }

        return this
    }

    companion object {
        private const val InstanceName = "INSTANCE"

        private fun getObjectInstance(source: KClass<*>): Any? {
            for (field in source.java.fields) {
                if (field.name == InstanceName) {
                    AccessController.doPrivileged(PrivilegedAccessible(field))
                    return try {
                        field[0] as Any
                    } catch (e: IllegalAccessException) {
                        KSONUtil.loggerBinder(KSONUtil.MessageType.Error, "Unable to access object instance!")
                        null
                    }

                }
            }
            return null
        }

        private fun getFieldWithBypass(field: Field): Any? {
            var result: Any? = null
            try {
                AccessController.doPrivileged(PrivilegedAccessible(field))

                if (field.modifiers and Modifier.PRIVATE == Modifier.PRIVATE) {
                    val modifiersField = Field::class.java.getDeclaredField("modifiers")

                    AccessController.doPrivileged(PrivilegedAccessible(modifiersField))
                    modifiersField.setInt(field, field.modifiers and Modifier.PRIVATE.inv())
                    result = field.get(null)
                    modifiersField.setInt(field, field.modifiers or Modifier.PRIVATE)

                } else {
                    result = field.get(null)
                }
            } catch (e: SecurityException) {
                KSONUtil.loggerBinder(KSONUtil.MessageType.Error, "Security manager is preventing reading of private fields!")
            }
            return result
        }

        private fun setFieldWithBypass(field: Field, value: Any) {
            try {
                AccessController.doPrivileged(PrivilegedAccessible(field))

                if (field.modifiers and Modifier.FINAL == Modifier.FINAL) {
                    val modifiersField = Field::class.java.getDeclaredField("modifiers")

                    AccessController.doPrivileged(PrivilegedAccessible(modifiersField))
                    modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
                    field.set(null, value)
                    modifiersField.setInt(field, field.modifiers or Modifier.FINAL)

                } else {
                    field.set(null, value)
                }
            } catch (e: SecurityException) {
                KSONUtil.loggerBinder(KSONUtil.MessageType.Error, "Security manager is preventing changing of final fields!")
            }
        }
    }
}
