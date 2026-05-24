package ru.pyxiion.pxrp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SyntaxParserTest {

    @Test
    fun `parses single literal`() {
        val (parts, args) = SyntaxParser("gamemode").parse()
        assertEquals(listOf("gamemode"), parts)
        assertTrue(args.isEmpty())
    }

    @Test
    fun `parses multiple literals`() {
        val (parts, args) = SyntaxParser("cmd sub").parse()
        assertEquals(listOf("cmd", "sub"), parts)
        assertTrue(args.isEmpty())
    }

    @Test
    fun `parses literal with required arg`() {
        val (parts, args) = SyntaxParser("cmd <name:text>").parse()
        assertEquals(listOf("cmd"), parts)
        assertEquals(listOf(ArgToken("name", "text", false)), args)
    }

    @Test
    fun `parses literal with multiple required args`() {
        val (parts, args) = SyntaxParser("cmd <name:text> <count:int>").parse()
        assertEquals(listOf("cmd"), parts)
        assertEquals(2, args.size)
        assertEquals(ArgToken("name", "text", false), args[0])
        assertEquals(ArgToken("count", "int", false), args[1])
    }

    @Test
    fun `parses optional arg`() {
        val (parts, args) = SyntaxParser("cmd [<opt:bool>]").parse()
        assertEquals(listOf("cmd"), parts)
        assertEquals(listOf(ArgToken("opt", "bool", true)), args)
    }

    @Test
    fun `parses choice arg`() {
        val (parts, args) = SyntaxParser("cmd <mode:choice=creative,survival>").parse()
        assertEquals(listOf("cmd"), parts)
        assertEquals(listOf(ArgToken("mode", "choice=creative,survival", false)), args)
    }

    @Test
    fun `parses required and optional together`() {
        val syntax = "gamemode <mode:choice=creative,spectator> [<target:player>]"
        val (parts, args) = SyntaxParser(syntax).parse()
        assertEquals(listOf("gamemode"), parts)
        assertEquals(2, args.size)
        assertEquals(ArgToken("mode", "choice=creative,spectator", false), args[0])
        assertEquals(ArgToken("target", "player", true), args[1])
    }

    @Test
    fun `handles extra whitespace`() {
        val (parts, args) = SyntaxParser("  cmd   <arg:text>  ").parse()
        assertEquals(listOf("cmd"), parts)
        assertEquals(listOf(ArgToken("arg", "text", false)), args)
    }

    @Test
    fun `errors on missing type`() {
        assertFailsWith<IllegalStateException> {
            SyntaxParser("cmd <name>").parse()
        }
    }

    @Test
    fun `errors on empty name`() {
        assertFailsWith<IllegalStateException> {
            SyntaxParser("cmd <:text>").parse()
        }
    }

    @Test
    fun `errors on empty type`() {
        assertFailsWith<IllegalStateException> {
            SyntaxParser("cmd <name:>").parse()
        }
    }

    @Test
    fun `errors on missing closing bracket`() {
        assertFailsWith<IllegalStateException> {
            SyntaxParser("cmd <name:text").parse()
        }
    }

    @Test
    fun `empty input returns empty results`() {
        val (parts, args) = SyntaxParser("").parse()
        assertTrue(parts.isEmpty())
        assertTrue(args.isEmpty())
    }

    @Test
    fun `whitespace only returns empty results`() {
        val (parts, args) = SyntaxParser("   ").parse()
        assertTrue(parts.isEmpty())
        assertTrue(args.isEmpty())
    }

    @Test
    fun `errors on required after optional`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            parseSyntaxString("cmd [<opt:bool>] <req:text>")
        }
        assertTrue(ex.message!!.contains("req"))
    }

    @Test
    fun `no error when optional follows required`() {
        val (parts, args) = parseSyntaxString("cmd <req:text> [<opt:bool>]")
        assertEquals(listOf("cmd"), parts)
        assertEquals(2, args.size)
        assertEquals(ArgToken("req", "text", false), args[0])
        assertEquals(ArgToken("opt", "bool", true), args[1])
    }

    @Test
    fun `parses optional with angle bracket wrapper`() {
        val (parts, args) = SyntaxParser("cmd [<name:text>]").parse()
        assertEquals(listOf("cmd"), parts)
        assertEquals(listOf(ArgToken("name", "text", true)), args)
    }

    @Test
    fun `literal after required arg`() {
        val (parts, args) = SyntaxParser("cmd <target:player> do").parse()
        assertEquals(listOf("cmd", "do"), parts)
        assertEquals(listOf(ArgToken("target", "player", false)), args)
    }

    @Test
    fun `parseSyntaxString with literal after arg`() {
        val (parts, args) = parseSyntaxString("cmd <target:player> do")
        assertEquals(listOf("cmd", "do"), parts)
        assertEquals(listOf(ArgToken("target", "player", false)), args)
    }

    @Test
    fun `subcommands with same argument before literal`() {
        val doParts = SyntaxParser("cmd <target:player> do").parse()
        val undoParts = SyntaxParser("cmd <target:player> undo").parse()
        assertEquals(listOf("cmd", "do"), doParts.first)
        assertEquals(listOf("cmd", "undo"), undoParts.first)
        assertEquals(doParts.second, undoParts.second)
        assertEquals(ArgToken("target", "player", false), doParts.second.single())
    }
}
