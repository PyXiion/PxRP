package ru.pyxiion.pxrp

import com.mojang.brigadier.tree.ArgumentCommandNode
import net.minecraft.server.command.ServerCommandSource
import ru.pyxiion.pxrp.types.LuaArgumentType

data class ArgDef(
    val luaType: LuaArgumentType,
    val node: ArgumentCommandNode<ServerCommandSource, *>,
    val isOptional: Boolean
)

data class ArgToken(
    val name: String,
    val typeDef: String,
    val isOptional: Boolean
)

class SyntaxParser(private val syntax: String) {
    private var pos = 0

    fun parse(): Pair<List<String>, List<ArgToken>> {
        val commandParts = mutableListOf<String>()
        val argTokens = mutableListOf<ArgToken>()

        skipWhitespace()
        while (pos < syntax.length) {
            when (syntax[pos]) {
                '<' -> argTokens.add(parseArg(required = true))
                '[' -> argTokens.add(parseArg(required = false))
                else -> commandParts.add(parseLiteral())
            }
            skipWhitespace()
        }

        return Pair(commandParts, argTokens)
    }

    private fun parseLiteral(): String {
        val start = pos
        while (pos < syntax.length && syntax[pos] !in " \t<[") pos++
        if (pos == start) error("Expected literal, argument, or end at position $pos")
        return syntax.substring(start, pos)
    }

    private fun parseArg(required: Boolean): ArgToken {
        val open = syntax[pos]
        val close = if (required) '>' else ']'

        if (open != if (required) '<' else '[') {
            error("Expected '$open' at position $pos")
        }
        pos++

        val nameStart = pos
        var nameEnd = -1

        while (pos < syntax.length && syntax[pos] != close) {
            if (syntax[pos] == ':' && nameEnd == -1) {
                nameEnd = pos
            }
            pos++
        }

        if (pos >= syntax.length) {
            error("Missing closing '$close' for argument starting at position $nameStart")
        }
        if (nameEnd == -1) {
            error("Argument '${syntax.substring(nameStart, pos)}' is missing type specification. Use <name:type>.")
        }

        val name = syntax.substring(nameStart, nameEnd)
        val typeDef = syntax.substring(nameEnd + 1, pos)

        if (name.isEmpty()) error("Argument has empty name at position $nameStart")
        if (typeDef.isEmpty()) error("Argument '$name' has empty type at position $nameStart")

        val cleanName = if (!required && name.startsWith("<")) name.substring(1) else name
        val cleanTypeDef = if (!required && typeDef.endsWith(">")) typeDef.substring(0, typeDef.length - 1) else typeDef

        pos++

        return ArgToken(cleanName, cleanTypeDef, !required)
    }

    private fun skipWhitespace() {
        while (pos < syntax.length && syntax[pos].isWhitespace()) pos++
    }
}

fun parseSyntaxString(syntax: String): Pair<List<String>, List<ArgToken>> {
    val (commandParts, argTokens) = SyntaxParser(syntax).parse()

    var optionalStarted = false
    for (token in argTokens) {
        if (!token.isOptional && optionalStarted) {
            throw IllegalArgumentException("Required argument '${token.name}' cannot follow optional arguments")
        }
        if (token.isOptional) optionalStarted = true
    }

    return Pair(commandParts, argTokens)
}

fun buildVariants(argDefs: List<ArgDef>): List<List<ArgDef>> {
    if (argDefs.isEmpty() || argDefs.none { it.isOptional }) {
        return listOf(argDefs)
    }

    val firstOptional = argDefs.indexOfFirst { it.isOptional }
    val required = argDefs.take(firstOptional)
    val optional = argDefs.drop(firstOptional)

    return (0..optional.size).map { i -> required + optional.take(i) }
}
