package ru.pyxiion.pxrp

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import ru.pyxiion.pxrp.mixins.CommandNodeMixin

class LuaCommandManager(
    private val server: MinecraftServer
) {
    private var isRegistered = false
    private val commands = linkedMapOf<String, LiteralCommandNode<ServerCommandSource>>()
    private val pathPermissions = mutableMapOf<String, MutableSet<String?>>()
    private val originalNodes = mutableMapOf<String, LiteralCommandNode<ServerCommandSource>>()

    companion object {
        val RESERVED_COMMANDS = setOf(
            "pxrp", "stop", "reload", "op", "deop",
            "ban", "ban-ip", "pardon", "pardon-ip",
            "save-all", "save-on", "save-off",
            "whitelist"
        )
    }

    fun addCommand(
        command: String,
        arguments: List<ArgumentCommandNode<ServerCommandSource, *>>,
        executor: (ctx: CommandContext<ServerCommandSource>) -> Int,
        permission: String?
    ) {
        check(!isRegistered) { "You can't add commands until you unregister the current commands" }

        val commandParts = command.split(" ")
        require(commandParts.isNotEmpty()) { "Command must not be empty" }
        require(commandParts.first() !in RESERVED_COMMANDS) {
            "Команда '${commandParts.first()}' является зарезервированной и не может быть переопределена через Lua"
        }

        val cmd = getOrCreateNodeByCmdPath(commandParts)

        // Propagate permission to all literal nodes along the command path
        for (i in commandParts.indices) {
            val prefix = commandParts.take(i + 1).joinToString(" ")
            val perms = pathPermissions.getOrPut(prefix) { mutableSetOf() }
            perms.add(permission)

            val snapshot = perms.toSet()
            val node = getNodeByPath(commandParts.take(i + 1))

            if (null in snapshot) {
                (node as CommandNodeMixin).setRequirement { true }
            } else {
                (node as CommandNodeMixin).setRequirement { source ->
                    snapshot.any { perm -> source.checkPermission(perm!!) }
                }
            }
        }

        val lastArg = mergeOrBuildArgsFrom(cmd, arguments)
        (lastArg as CommandNodeMixin).command = Command { ctx -> executor(ctx) }

        PxRp.logger.debug("/{} was added to the LuaCommandManager with arguments = {}, permission = {}", command, arguments, permission)
    }

    private fun getOrCreateNodeByCmdPath(path: List<String>): LiteralCommandNode<ServerCommandSource> {
        var node = commands.getOrPut(path.first()) { CommandManager.literal(path.first()).build() }

        for (i in 1..<path.size) {
            var child = node.getChild(path[i])
            if (child == null) {
                child = CommandManager.literal(path[i]).build()
                node.addChild(child)
            }
            node = child as LiteralCommandNode
        }

        return node
    }

    private fun getNodeByPath(path: List<String>): LiteralCommandNode<ServerCommandSource> {
        var node = commands[path.first()]!!
        for (i in 1..<path.size) {
            node = node.getChild(path[i]) as LiteralCommandNode<ServerCommandSource>
        }
        return node
    }

    @Suppress("UNCHECKED_CAST")
    private fun mergeOrBuildArgsFrom(
        node: LiteralCommandNode<ServerCommandSource>,
        args: List<ArgumentCommandNode<ServerCommandSource, *>>
    ): CommandNode<ServerCommandSource> {
        var currentNode: CommandNode<ServerCommandSource> = node
        for (arg in args) {
            val existing = (currentNode as CommandNodeMixin).children.values.find {
                it is ArgumentCommandNode<*, *> && it.name == arg.name
            }
            val argNode = if (existing != null && existing !== arg) {
                PxRp.logger.warn(
                    "[PxRP] Несколько скриптов регистрируют аргумент '{}' с одинаковым типом под одним путём. " +
                            "Будет использована конфигурация первого скрипта.",
                    arg.name
                )
                existing
            } else if (existing != null) {
                existing
            } else {
                currentNode.addChild(arg)
                arg
            }
            currentNode = argNode as CommandNode<ServerCommandSource>
        }

        return currentNode
    }

    fun registerAll() {
        check(!isRegistered) { "You can't register commands twice" }
        isRegistered = true
        val dispatcher = server.commandManager.dispatcher
        val root = dispatcher.root as CommandNodeMixin
        commands.forEach { (k, v) ->
            if (root.children.containsKey(k)) {
                originalNodes.putIfAbsent(k, root.children[k] as LiteralCommandNode<ServerCommandSource>)
                root.children.remove(k)
                root.literals.remove(k)
            }
            dispatcher.root.addChild(v)
        }
        updateCommandLists()
    }

    fun clear() {
        val root = (server.commandManager.dispatcher.root as CommandNodeMixin)

        if (isRegistered) {
            commands.forEach { (k, _) ->
                root.children.remove(k)
                root.literals.remove(k)
            }
            originalNodes.forEach { (name, original) ->
                root.children[name] = original
                root.literals[name] = original
            }
            originalNodes.clear()
        }
        isRegistered = false

        commands.clear()
        pathPermissions.clear()
        updateCommandLists()
    }

    private fun updateCommandLists() {
        val cmdManager = server.commandManager
        server.playerManager.playerList.forEach {
            cmdManager.sendCommandTree(it)
        }
    }
}