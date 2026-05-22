package ru.pyxiion.pxrp.types

import org.luaj.vm2.*
import kotlin.reflect.*
import kotlin.reflect.full.*

class KotlinInstance(obj: Any) : LuaUserdata(obj) {
    private var klass = obj::class

    override fun get(key: LuaValue): LuaValue {
        val fieldName = key.tojstring()

        klass.memberProperties
            .find { it.name == fieldName && it.visibility == KVisibility.PUBLIC }
            ?.let {
                return toLuaValue(it.call(m_instance))
            }

        klass.memberFunctions
            .find { it.name == fieldName && it.visibility == KVisibility.PUBLIC }
            ?.let {

            }

        return super.get(key)
    }

    companion object {
        private fun fromLuaValue(value: LuaValue): Any? = when {
            value.isnil() -> null
            value.isboolean() -> value.toboolean()
            value.isnumber() -> value.todouble()
            value.isstring() -> value.tojstring()
            value.isuserdata() -> value.checkuserdata()
            value.istable() -> value
            else -> error("Unsupported LuaValue: $value")
        }

        private fun toLuaValue(value: Any?): LuaValue = when (value) {
            null -> LuaValue.NIL
            is Boolean -> LuaValue.valueOf(value)
            is Int -> LuaValue.valueOf(value)
            is Double -> LuaValue.valueOf(value)
            is Long -> LuaValue.valueOf(value.toDouble())
            is Float -> LuaValue.valueOf(value.toDouble())
            is String -> LuaValue.valueOf(value)
            is List<*> -> LuaTable().apply {
                value.forEachIndexed { i, v -> set(i + 1, toLuaValue(v)) }
            }
            is Map<*, *> -> LuaTable().apply {
                value.forEach { (k, v) -> set(toLuaValue(k), toLuaValue(v)) }
            }
            else -> KotlinInstance(value)
        }
    }
}
