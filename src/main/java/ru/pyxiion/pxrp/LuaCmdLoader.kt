package ru.pyxiion.pxrp

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.ArgumentCommandNode
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.command.argument.MessageArgumentType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.luaj.vm2.*
import org.luaj.vm2.LuaValue.NIL
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.JseBaseLib
import org.luaj.vm2.lib.jse.JseMathLib
import ru.pyxiion.pxrp.PxRp.Companion.logger
import ru.pyxiion.pxrp.api.LuaMcApi
import ru.pyxiion.pxrp.api.Player
import ru.pyxiion.pxrp.luaTableOf
import ru.pyxiion.pxrp.storage.StorageManager
import ru.pyxiion.pxrp.types.LuaArgumentType
import java.io.FileOutputStream
import kotlin.io.path.exists

// Represents a parsed Lua command argument with its name and argument type identifier (e.g. "text", "target")
data class LuaCommandArgument(
    val name: String,
    val type: String
)

// Loads and manages Lua-based commands from the config/pxrp/ directory.
// Bridges Lua script functions with Minecraft's Brigadier command system.
class LuaCmdLoader(
    private val server: MinecraftServer,
    private val storageManager: StorageManager
) {
    // Lua global environment shared across all script executions
    private lateinit var globals: Globals

    // Maps argument type names (used in Lua scripts) to their Brigadier node builders and runtime value extractors
    private val argumentTypes = mapOf(
        "text" to object : LuaArgumentType {
            override fun getArg(ctx: CommandContext<ServerCommandSource>, name: String): Any {
                val msg = MessageArgumentType.getMessage(ctx, name)
                return msg.literalString ?: msg.string
            }

            override fun getBrigadierArgument(name: String): ArgumentCommandNode<ServerCommandSource, *> {
                return CommandManager.argument(name, MessageArgumentType.message()).build()
            }
        },
        "target" to object : LuaArgumentType {
            override fun getArg(ctx: CommandContext<ServerCommandSource>, name: String): Any {
                return Player(EntityArgumentType.getPlayer(ctx, name)!!).toLuaValue()
            }

            override fun getBrigadierArgument(name: String): ArgumentCommandNode<ServerCommandSource, *> {
                return CommandManager.argument(name, EntityArgumentType.player()).build()
            }
        }
    )

    // Manages the registered Brigadier command tree for all Lua-defined commands
    private var commandManager: LuaCommandManager? = null

    // Provides the Minecraft API table exposed to Lua scripts
    private val api = LuaMcApi(server, storageManager)

    // Manages Lua event handlers registered via mc.on()
    val eventManager = LuaEventManager()

    // Task scheduler for mc.schedule / mc.scheduleRepeating
    val scheduler: Scheduler get() = api.scheduler

    // Sets up the Lua globals environment: installs standard libraries and registers
    // the `register` function and `mc` API table for Lua scripts
    fun prepareGlobals() {
        globals = Globals()
        LuaC.install(globals)
        LoadState.install(globals)
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(StringLib())
//        globals.load(CoroutineLib())
        globals.load(JseMathLib())

        val pxrpDir = FabricLoader.getInstance().configDir.resolve("pxrp").toAbsolutePath()
        globals.get("package").set("path", LuaValue.valueOf("${pxrpDir}/?.lua;${pxrpDir}/?/init.lua;?.lua"))

        globals.set("register", this::register.asVarArgFunction())
        val mcTable = api.toTable()
        val onHandler: (Varargs) -> Varargs = { args: Varargs ->
            require(args.narg() == 2) { "on(event, handler) requires 2 arguments" }
            val eventName = args.checkjstring(1)
            val handler = args.checkfunction(2)
            eventManager.on(eventName, handler)
            LuaValue.NIL
        }
        mcTable.set("on", onHandler.asVarArgFunction())
        globals.set("mc", mcTable)
    }

    // (Re)loads all Lua scripts from config/pxrp/ (or legacy config/pxrp.lua):
    // reinitializes globals, executes each script, registers all commands
    fun reload() {
        storageManager.saveAll()
        if (commandManager == null)
            commandManager = LuaCommandManager(server)

        commandManager!!.clear()
        eventManager.clear()
        scheduler.clear()
        prepareGlobals()

        val sources = getLuaSources()
        for ((name, source) in sources) {
            globals.load(source, name).call()
        }

        commandManager!!.registerAll()
        logger.info("PxRP зарегистрировал свои команды")
    }

    // Collects Lua sources to load, using the first matching strategy:
    //   1) config/pxrp/ directory — all .lua files, sorted alphabetically
    //   2) config/pxrp.lua        — legacy single-file fallback
    //   3) Neither exists         — creates config/pxrp/ with bundled demo.lua
    private fun getLuaSources(): List<Pair<String, String>> {
        val pxrpDir = FabricLoader.getInstance().configDir.resolve("pxrp")

        if (pxrpDir.toFile().isDirectory()) {
            val files = pxrpDir.toFile()
                .listFiles { f -> f.extension == "lua" }
                .orEmpty()
                .sortedBy { it.name }
            if (files.isNotEmpty()) {
                return files.map { it.name to it.readText() }
            }
        }

        val legacyFile = FabricLoader.getInstance().configDir.resolve("pxrp.lua")
        if (legacyFile.exists()) {
            return listOf("pxrp.lua" to legacyFile.toFile().readText())
        }

        pxrpDir.toFile().mkdirs()
        val resource = this::class.java.getResourceAsStream("/demo.lua")
            ?: throw IllegalStateException("Default demo.lua not found in mod JAR")
        val targetFile = pxrpDir.resolve("demo.lua").toFile()
        resource.copyTo(FileOutputStream(targetFile))
        logger.info("Создан конфигурационный файл demo.lua в config/pxrp/")

        return listOf("demo.lua" to targetFile.readText())
    }


    // Called from Lua as `register(command, arguments, handler, permission)`.
    // Parses the argument definitions, builds a Brigadier executor that dispatches
    // to the Lua handler function, and registers the command with the command manager
    private fun register(args: Varargs): Varargs {
        require(args.narg() in 3..4) { "register(command, arguments, handler, permission = nil) requires 3..4 arguments" }
        val commandName = args.arg(1).checkjstring()
        val arguments = args.arg(2).checkstringlist()
        val handler = args.arg(3)
        val permission: String? = args.arg(4).optjstring(null)

        require(handler.isfunction()) { "Command handler must be a function" }

        val parsedArgs = parseArgs(arguments)

        val executor =
            fun(ctx: CommandContext<ServerCommandSource>): Int {
                try {
                    executeLuaCommand(ctx, parsedArgs, handler.checkfunction())
                    return 1
                } catch (e: CommandSyntaxException) {
                    throw e
                } catch (e: LuaError) {
                    ctx.source.sendError(Text.literal("При выполнении команды произошла ошибка в скрипте. Сообщите об этом администратору."))
                    logger.error("Ошибка при выполнении команды \"${ctx.command}\": ${e.message}")
                } catch (e: Throwable) {
                    ctx.source.sendError(Text.literal("При выполнении команды произошла неизвестная ошибка. Сообщите об этом администратору."))
                    logger.error("Ошибка при выполнении команды \"${ctx.command}\": ${e.message}", e)
                }
                return 0
            }

        commandManager!!.addCommand(commandName, parsedArgs.map { it.second }, executor, permission)

        return NIL
    }

    // Parses Lua argument type strings (e.g. "target" or "msg:text") into pairs of
    // [typeName, BrigadierArgumentNode]. Auto-generates unique names for unnamed arguments
    private fun parseArgs(args: List<String>): List<Pair<String, ArgumentCommandNode<ServerCommandSource, *>>> {
        val result = mutableListOf<Pair<String, ArgumentCommandNode<ServerCommandSource, *>>>()
        val usedNames = mutableMapOf<String, Int>()

        for (arg in args) {
            if (arg.contains(':')) {
                val (name, type) = arg.split(':')
                require(argumentTypes.containsKey(type)) { "Unknown argument type '$type'" }
                result.add(Pair(type, argumentTypes[type]!!.getBrigadierArgument(name)))
            } else {
                require(argumentTypes.containsKey(arg)) { "Unknown argument type '$arg'" }
                val name = "$arg${usedNames.getOrPut(arg) { 0 } + 1}"
                usedNames[arg] = usedNames[arg]!!.plus(1)
                result.add(Pair(arg, argumentTypes[arg]!!.getBrigadierArgument(name)))
            }
        }

        return result
    }

    private fun prepareLuaArgAndContext(ctx: CommandContext<ServerCommandSource>, argsInfo: List<Pair<String, ArgumentCommandNode<ServerCommandSource, *>>>): Array<LuaValue> {
        val args = argsInfo.map { LuaCommandArgument(it.second.name, it.first) }

        val player = Player(ctx.source.playerOrThrow).toLuaValue()
        val ctxTable = luaTableOf("player" to player)

        val result = mutableListOf<LuaValue>()
        result.add(ctxTable)
        for (a in args) {
            val arg = argumentTypes[a.type]!!.getArg(ctx, a.name)
            result.add(
                when (arg) {
                    is LuaValue -> arg
                    is String -> LuaValue.valueOf(arg)
                    else -> throw IllegalArgumentException(
                        "Unsupported argument type: ${arg::class.simpleName ?: arg.javaClass.name}"
                    )
                }
            )
        }
        return result.toTypedArray()
    }

    // Invokes the Lua handler function with the prepared context and arguments,
    // then clears temporary global state (`player` and `currentContext`)
    private fun executeLuaCommand(ctx: CommandContext<ServerCommandSource>, argsInfo: List<Pair<String, ArgumentCommandNode<ServerCommandSource, *>>>, function: LuaFunction) {
        try {
            val luaArgs = prepareLuaArgAndContext(ctx, argsInfo)
            function.invoke(luaArgs)
        } catch (e: LuaError) {
            throw e
        }
    }
}