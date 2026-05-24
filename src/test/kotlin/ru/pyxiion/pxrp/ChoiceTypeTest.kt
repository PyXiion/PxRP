package ru.pyxiion.pxrp

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kotlin.test.Test
import ru.pyxiion.pxrp.types.ChoiceArgumentType
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChoiceTypeTest {

    private fun choiceType(vararg choices: String) =
        ChoiceArgumentType.ChoiceType(choices.toList())

    @Test
    fun `valid choice is accepted`() {
        val result = choiceType("creative", "spectator").parse(StringReader("creative"))
        assertEquals("creative", result)
    }

    @Test
    fun `any valid choice is accepted`() {
        val ct = choiceType("creative", "spectator", "survival", "adventure")
        assertEquals("creative", ct.parse(StringReader("creative")))
        assertEquals("spectator", ct.parse(StringReader("spectator")))
        assertEquals("survival", ct.parse(StringReader("survival")))
        assertEquals("adventure", ct.parse(StringReader("adventure")))
    }

    @Test
    fun `invalid choice throws`() {
        val ct = choiceType("creative", "spectator")
        val ex = assertFailsWith<CommandSyntaxException> {
            ct.parse(StringReader("survival"))
        }
        assertTrue(ex.message!!.isNotBlank())
    }

    @Test
    fun `cursor is reset on parse failure`() {
        val reader = StringReader("survival")
        val ct = choiceType("creative", "spectator")
        assertFailsWith<CommandSyntaxException> {
            ct.parse(reader)
        }
        assertEquals(0, reader.cursor)
    }

    @Test
    fun `empty input throws`() {
        val ct = choiceType("creative")
        assertFailsWith<CommandSyntaxException> {
            ct.parse(StringReader(""))
        }
    }

    @Test
    fun `single choice matches`() {
        val ct = choiceType("only_option")
        assertEquals("only_option", ct.parse(StringReader("only_option")))
    }

    @Test
    fun `equals and hashCode depend on choices`() {
        val a1 = choiceType("x", "y")
        val a2 = choiceType("x", "y")
        val b = choiceType("x", "z")

        assertEquals(a1, a2)
        assertEquals(a1.hashCode(), a2.hashCode())

        assertTrue(a1 != b)
    }

    @Test
    fun `getExamples returns choices`() {
        val ct = choiceType("alpha", "beta")
        assertEquals(listOf("alpha", "beta"), ct.examples.toList())
    }
}
