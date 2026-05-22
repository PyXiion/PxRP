package ru.pyxiion.pxrp.coerce

import org.luaj.vm2.LuaValue
import kotlin.reflect.KClass

//fun Any?.toLua(): LuaValue {
//    return when(this) {
//        null -> LuaValue.NIL
//        is Boolean -> LuaValue.valueOf(this)
//
//        is Byte -> LuaValue.valueOf(this.toInt())
//        is Short -> LuaValue.valueOf(this.toInt())
//        is Int -> LuaValue.valueOf(this)
//
//        is Float -> LuaValue.valueOf(this.toDouble())
//        is Long -> LuaValue.valueOf(this.toDouble())
//        is Double -> LuaValue.valueOf(this)
//
//        is Char -> LuaValue.valueOf(this.toString())
//        is String -> LuaValue.valueOf(this)
//
//        is LuaValue -> this
//
//        is Class<*> -> KotlinClass.fromClass(this.kotlin)
//        is KClass<*> -> KotlinClass.fromClass(this)
//    }
//}