package ru.pyxiion.pxrp.types

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.tree.ArgumentCommandNode
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

class ChoiceArgumentType(private val choices: List<String>) : LuaArgumentType {
    init {
        require(choices.isNotEmpty()) { "choice type requires at least one option" }
        require(choices.all { it.isNotBlank() }) { "choice options must not be blank" }
    }

    override fun getArg(ctx: CommandContext<ServerCommandSource>, name: String): Any {
        return StringArgumentType.getString(ctx, name)
    }

    override fun getBrigadierArgument(name: String): ArgumentCommandNode<ServerCommandSource, *> {
        return CommandManager.argument(name, ChoiceType(choices))
            .suggests(SuggestionProvider { _, builder ->
                choices.forEach { builder.suggest(it) }
                builder.buildFuture()
            })
            .build()
    }

    internal class ChoiceType(val choices: List<String>) : ArgumentType<String> {
        override fun parse(reader: StringReader): String {
            val start = reader.cursor
            val word = StringArgumentType.word().parse(reader)
            if (word in choices) return word
            reader.cursor = start
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ChoiceType) return false
            return choices == other.choices
        }

        override fun hashCode(): Int = choices.hashCode()

        override fun getExamples(): Collection<String> = choices
    }
}
