package ru.pyxiion.pxrp.api

import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import ru.pyxiion.pxrp.Scheduler
import ru.pyxiion.pxrp.asVarArgFunction
import ru.pyxiion.pxrp.luaTableOf
import ru.pyxiion.pxrp.storage.StorageManager
import java.util.UUID

class LuaMcApi(
    private val server: MinecraftServer,
    private val storage: StorageManager
) {
    val scheduler = Scheduler()
    private val playerCache = mutableMapOf<UUID, LuaValue>()

    fun invalidatePlayer(uuid: UUID) {
        playerCache.remove(uuid)
    }
    private fun <T> getRegistryKey(registryType: RegistryKey<Registry<T>>, key: String): RegistryKey<T> {
        return RegistryKey.of(registryType, Identifier.of(key))
    }

    private fun requireWorld(arg: LuaValue): ServerWorld {
        return server.getWorld(requireWorldKey(arg))
            ?: throw IllegalArgumentException("World ${arg.tostring()} not found.")
    }

    private fun requireWorldKey(arg: LuaValue): RegistryKey<World> {
        val key = arg.checkjstring()
        return getRegistryKey(RegistryKeys.WORLD, key)
    }


    private fun requireSound(arg: LuaValue): SoundEvent {
        val key = arg.checkjstring()
        return Registries.SOUND_EVENT.get(getRegistryKey(RegistryKeys.SOUND_EVENT, key))
            ?: throw IllegalArgumentException("Sound $key not found")
    }


    private fun playSound(args: Varargs) {
        require(args.narg() in 5..7) { "playSound(sound, x, y, z, world, volume = 1, pitch[0-2] = 1) requires 5..7 arguments" }
        val sound = requireSound(args.arg(1))
        val (x, y, z) = (2..4).map { args.arg(it).checkdouble() }
        val world = requireWorld(args.arg(5))
        val (volume, pitch) = (6..7).map { args.arg(it).optdouble(1.0).toFloat() }

        world.playSound(
            null,
            x, y, z,
            sound,
            SoundCategory.PLAYERS,
            volume,
            pitch
        )
    }

    private fun doBroadcast(text: String, pos: Vec3d?, world: RegistryKey<World>?, range: Double?, overlay: Int?) {
        var players = server.playerManager.playerList

        range?.let { rng ->
            require(pos != null && world != null)
            val rangeSquare = rng * rng
            players = players.filter {
                it.entityWorld.registryKey == world
                        && it.squaredDistanceTo(pos) < rangeSquare
            }
        }

        if (overlay == null) {
            players.forEach {
                it.sendMessage(Text.literal(text))
            }
        } else {
            val timingPacket = TitleFadeS2CPacket(20, overlay, 20)
            val titlePacket = OverlayMessageS2CPacket(Text.literal(text))
            players.forEach {
                with(it.networkHandler) {
                    sendPacket(timingPacket)
                    sendPacket(titlePacket)
                }
            }
        }
    }

    private fun luaTime(args: Varargs): Varargs {
        return LuaValue.valueOf(System.currentTimeMillis() / 1000.0)
    }

    private fun broadcast(args: Varargs) {
        require(args.narg() in 1..2) { "broadcast(text, overlay = false) requires 1..2 arguments" }
        val text = args.arg(1).checkjstring()
        val overlay = if (args.arg(2).isint()) args.arg(2).toint() else null

        doBroadcast(text, null, null, null, overlay)
    }

    private fun luaSchedule(args: Varargs): Varargs {
        require(args.narg() == 2) { "schedule(delay, callback) requires 2 arguments" }
        val delay = args.arg(1).checkint()
        val callback = args.arg(2).checkfunction()
        val id = scheduler.schedule(delay, callback)
        return LuaValue.valueOf(id)
    }

    private fun luaScheduleRepeating(args: Varargs): Varargs {
        require(args.narg() == 3) { "scheduleRepeating(delay, interval, callback) requires 3 arguments" }
        val delay = args.arg(1).checkint()
        val interval = args.arg(2).checkint()
        val callback = args.arg(3).checkfunction()
        val id = scheduler.scheduleRepeating(delay, interval, callback)
        return LuaValue.valueOf(id)
    }

    private fun luaCancelTask(args: Varargs): Varargs {
        require(args.narg() == 1) { "cancelTask(id) requires 1 argument" }
        val id = args.arg(1).checkint()
        val removed = scheduler.cancel(id)
        return LuaValue.valueOf(removed)
    }

    private fun luaGetPlayers(args: Varargs): Varargs {
        val list = LuaTable()
        server.playerManager.playerList.forEachIndexed { i, p ->
            list.set(i + 1, playerCache.getOrPut(p.uuid) { Player(p).toLuaValue() })
        }
        return list
    }

    private fun luaGetWorld(args: Varargs): Varargs {
        val name = args.arg(1).checkjstring()
        val key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(name))
        val world = server.getWorld(key)
            ?: throw IllegalArgumentException("World '$name' not found")
        return World(world, playerCache).toLuaValue()
    }

    fun toTable(): LuaTable {
        return luaTableOf(
            "broadcast" to this::broadcast.asVarArgFunction(),
            "playSound" to this::playSound.asVarArgFunction(),
            "data" to storage.getGlobalData(),
            "time" to this::luaTime.asVarArgFunction(),
            "schedule" to this::luaSchedule.asVarArgFunction(),
            "scheduleRepeating" to this::luaScheduleRepeating.asVarArgFunction(),
            "cancelTask" to this::luaCancelTask.asVarArgFunction(),
            "world" to this::luaGetWorld.asVarArgFunction(),
            "players" to this::luaGetPlayers.asVarArgFunction(),
            "onlineCount" to LuaValue.valueOf(server.playerManager.currentPlayerCount.toDouble()),
        )
    }
}