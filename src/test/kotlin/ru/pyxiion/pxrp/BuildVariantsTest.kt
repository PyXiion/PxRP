package ru.pyxiion.pxrp

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.ArgumentCommandNode
import net.minecraft.server.command.ServerCommandSource
import kotlin.test.Test
import ru.pyxiion.pxrp.types.LuaArgumentType
import kotlin.test.assertEquals

private fun argNode(name: String): ArgumentCommandNode<ServerCommandSource, *> =
    RequiredArgumentBuilder.argument<ServerCommandSource, String>(name, StringArgumentType.word()).build()

private val dummyType = object : LuaArgumentType {
    override fun getArg(ctx: CommandContext<ServerCommandSource>, name: String): Any = "test"
    override fun getBrigadierArgument(name: String): ArgumentCommandNode<ServerCommandSource, *> = argNode(name)
}

private fun req(name: String = "x") = ArgDef(dummyType, argNode(name), false)
private fun opt(name: String = "y") = ArgDef(dummyType, argNode(name), true)

class BuildVariantsTest {

    @Test
    fun `empty list returns single empty variant`() {
        val result = buildVariants(emptyList())
        assertEquals(listOf(emptyList<ArgDef>()), result)
    }

    @Test
    fun `all required returns single variant`() {
        val args = listOf(req("a"), req("b"))
        val result = buildVariants(args)
        assertEquals(1, result.size)
        assertEquals(args, result[0])
    }

    @Test
    fun `single optional generates two variants`() {
        val result = buildVariants(listOf(opt("a")))
        assertEquals(2, result.size)
        assertEquals(0, result[0].size)
        assertEquals(1, result[1].size)
        assertEquals(true, result[1][0].isOptional)
    }

    @Test
    fun `required plus one optional`() {
        val r = req("a")
        val o = opt("b")
        val result = buildVariants(listOf(r, o))
        assertEquals(2, result.size)
        assertEquals(listOf(r), result[0])
        assertEquals(listOf(r, o), result[1])
    }

    @Test
    fun `required plus two optional`() {
        val r = req("a")
        val o1 = opt("b")
        val o2 = opt("c")
        val result = buildVariants(listOf(r, o1, o2))
        assertEquals(3, result.size)
        assertEquals(listOf(r), result[0])
        assertEquals(listOf(r, o1), result[1])
        assertEquals(listOf(r, o1, o2), result[2])
    }

    @Test
    fun `all optional generates N plus 1 variants`() {
        val o1 = opt("a")
        val o2 = opt("b")
        val o3 = opt("c")
        val result = buildVariants(listOf(o1, o2, o3))
        assertEquals(4, result.size)
        assertEquals(emptyList<ArgDef>(), result[0])
        assertEquals(listOf(o1), result[1])
        assertEquals(listOf(o1, o2), result[2])
        assertEquals(listOf(o1, o2, o3), result[3])
    }

    @Test
    fun `single required returns single variant`() {
        val r = req()
        val result = buildVariants(listOf(r))
        assertEquals(1, result.size)
        assertEquals(listOf(r), result[0])
    }

    @Test
    fun `variants preserve arg objects`() {
        val r = req("a")
        val o = opt("b")
        val result = buildVariants(listOf(r, o))
        assertEquals(r, result[0][0])
        assertEquals(r, result[1][0])
        assertEquals(o, result[1][1])
    }
}
