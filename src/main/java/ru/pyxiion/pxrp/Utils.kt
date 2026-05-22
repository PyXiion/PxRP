package ru.pyxiion.pxrp

import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.command.CommandSource
import net.minecraft.entity.Entity
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction

fun LuaValue.checkstringlist(): List<String> {
    val table = this.checktable()
    val result = mutableListOf<String>()

    var index = 1
    while (true) {
        val value = table.rawget(index++)
        if (value.isnil()) break

        require(value.isstring()) { "Expected string at index $index, got ${value.typename()}" }

        result.add(value.tojstring())
    }

    return result
}


fun Entity.checkPermission(permission: String): Boolean {
    1 to 2
    return Permissions.check(this, permission)
}

fun CommandSource.checkPermission(permission: String): Boolean {
    1 to 2
    return Permissions.check(this, permission)
}

fun luaTableOf(vararg items: Pair<String, LuaValue>): LuaTable {
    return LuaTable().apply {
        items.forEach { (k, v) ->
            this.set(k, v)
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun ((Varargs) -> Varargs).asVarArgFunction() = object : VarArgFunction() {
    override fun invoke(args: Varargs): Varargs = this@asVarArgFunction(args)
}

@JvmName("asVarArgFunctionVoid")
@Suppress("NOTHING_TO_INLINE")
inline fun ((Varargs) -> Unit).asVarArgFunction() = object : VarArgFunction() {
    override fun invoke(args: Varargs): Varargs {
        this@asVarArgFunction(args)
        return NIL
    }
}