package ru.pyxiion.pxrp

import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.command.permission.PermissionLevel
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text
import org.luaj.vm2.LuaError
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PxRp : ModInitializer {
    companion object {
        val logger: Logger = LoggerFactory.getLogger("PxRP")
        lateinit var instance: PxRp
    }

    lateinit var luaLoader: LuaCmdLoader


    override fun onInitialize() {
        instance = this
        ServerLifecycleEvents.SERVER_STARTED.register(fun(server) {
            try {
                luaLoader = LuaCmdLoader(server)
                luaLoader.reload()
            } catch (e: Throwable) {
                logger.error("Ошибка при запуске PxRP: ${e.message}", e)
            }
        })

        CommandRegistrationCallback.EVENT.register(fun(dispatcher, reg, env) {
            dispatcher.register(
                CommandManager.literal("pxrp")
                    .requires(Permissions.require("pyxiion.pxrp", PermissionLevel.ADMINS))
                    .then(
                        CommandManager.literal("reload").executes { ctx ->
                            try {
                                luaLoader.reload()
                                ctx.source.sendFeedback({
                                    Text.literal("Перезагрузилось")
                                }, false)
                                return@executes 1
                            } catch (e: LuaError) {
                                logger.error("Ошибка при перезагрузке PxRP: ${e.message}")
                                ctx.source.sendFeedback({
                                    Text.literal("Ошибка при перезагрузке PxRP: ${e.message}")
                                }, false)
                            } catch (e: Throwable) {
                                logger.error("Ошибка при перезагрузке PxRP: ${e.message}", e)
                                ctx.source.sendFeedback({
                                    Text.literal("Ошибка при перезагрузке PxRP: ${e.message}")
                                }, false)
                            }
                            0
                        }
                    ))
        })
    }


}