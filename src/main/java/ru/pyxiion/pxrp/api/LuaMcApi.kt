package ru.pyxiion.pxrp.api

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket
import net.minecraft.particle.ParticleEffect
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
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import ru.pyxiion.pxrp.Scheduler
import ru.pyxiion.pxrp.asVarArgFunction
import ru.pyxiion.pxrp.luaTableOf
import ru.pyxiion.pxrp.storage.StorageManager

class LuaMcApi(
    private val server: MinecraftServer,
    private val storage: StorageManager
) {
    val scheduler = Scheduler()
    private fun <T> getRegistryKey(registryType: RegistryKey<Registry<T>>, key: String): RegistryKey<T> {
        return RegistryKey.of(registryType, Identifier.of(key))
    }

    private fun requireParticle(arg: LuaValue): ParticleEffect {
        val id = arg.checkjstring()
        val registryKey = getRegistryKey(RegistryKeys.PARTICLE_TYPE, id)
        val particleType = Registries.PARTICLE_TYPE.get(registryKey)
        check(particleType != null) { "Failed to get particle $id. Particle type not found." }

        if (particleType is ParticleEffect) {
            return particleType
        }

        return try {
            particleType.codec.codec().parse(server.registryManager.getOps(NbtOps.INSTANCE), NbtCompound()).orThrow
        } catch (e: IllegalStateException) {
            throw IllegalArgumentException(
                "Particle '$id' requires additional parameters and cannot be used with mc.particle(). " +
                        "Use simple particles like minecraft:gust or minecraft:campfire_cosmoke instead."
            )
        }
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


    private fun particle(args: Varargs) {
        require(args.narg() == 5) { "particle(particle, x, y, z, world) requires 5 arguments" }
        val particle = requireParticle(args.arg(1))
        val (x, y, z) = (2..4).map { args.arg(it).checkdouble() }
        val world = requireWorld(args.arg(5))

        server.playerManager.playerList.forEach {
            world.spawnParticles(it, particle, true, false, x, y, z, 1, 0.0, 0.0, 0.0, 0.0)
        }
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

    private fun broadcastInRange(args: Varargs) {
        require(args.narg() in 6..7) { "broadcastInRange(text, x, y, z, range, world, overlay = false) requires 6..7 arguments" }
        val text = args.arg(1).checkjstring()
        val (x, y, z, range) = (2..5).map { args.arg(it).checkdouble() }
        val world = requireWorldKey(args.arg(6))
        val overlay = if (args.arg(7).isint()) args.arg(7).toint() else null

        doBroadcast(text, Vec3d(x, y, z), world, range, overlay)
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

    private fun resolveBlockId(id: String): String {
        return if (id.contains(':')) id else "minecraft:$id"
    }

    private fun setBlock(args: Varargs) {
        require(args.narg() == 5) { "setBlock(x, y, z, block, world) requires 5 arguments" }
        val (x, y, z) = (1..3).map { args.arg(it).checkdouble() }
        val blockId = resolveBlockId(args.arg(4).checkjstring())
        val world = requireWorld(args.arg(5))

        val block = Registries.BLOCK.get(Identifier.of(blockId))
            ?: throw IllegalArgumentException("Block $blockId not found")
        world.setBlockState(BlockPos.ofFloored(x, y, z), block.defaultState, 0x03)
    }

    private fun luaGetBlock(args: Varargs): Varargs {
        require(args.narg() == 4) { "getBlock(x, y, z, world) requires 4 arguments" }
        val (x, y, z) = (1..3).map { args.arg(it).checkdouble() }
        val world = requireWorld(args.arg(4))

        val pos = BlockPos.ofFloored(x, y, z)
        val block = world.getBlockState(pos).block
        return LuaValue.valueOf(Registries.BLOCK.getId(block).toString())
    }

    private fun fill(args: Varargs) {
        require(args.narg() == 8) { "fill(x1, y1, z1, x2, y2, z2, block, world) requires 8 arguments" }
        val (x1, y1, z1) = (1..3).map { args.arg(it).checkdouble() }
        val (x2, y2, z2) = (4..6).map { args.arg(it).checkdouble() }
        val blockId = resolveBlockId(args.arg(7).checkjstring())
        val world = requireWorld(args.arg(8))

        val block = Registries.BLOCK.get(Identifier.of(blockId))
            ?: throw IllegalArgumentException("Block $blockId not found")

        val minX = Math.floor(minOf(x1, x2)).toInt()
        val minY = Math.floor(minOf(y1, y2)).toInt()
        val minZ = Math.floor(minOf(z1, z2)).toInt()
        val maxX = Math.floor(maxOf(x1, x2)).toInt()
        val maxY = Math.floor(maxOf(y1, y2)).toInt()
        val maxZ = Math.floor(maxOf(z1, z2)).toInt()

        val volume = (maxX - minX + 1L) * (maxY - minY + 1L) * (maxZ - minZ + 1L)
        require(volume <= 32768) { "fill volume ($volume) exceeds maximum (32768)" }

        val state = block.defaultState
        for (pos in BlockPos.iterate(BlockPos(minX, minY, minZ), BlockPos(maxX, maxY, maxZ))) {
            world.setBlockState(pos, state, 0x02)
        }
    }

    fun toTable(): LuaTable {
        return luaTableOf(
            "particle" to this::particle.asVarArgFunction(),
            "broadcast" to this::broadcast.asVarArgFunction(),
            "playSound" to this::playSound.asVarArgFunction(),
            "broadcastInRange" to this::broadcastInRange.asVarArgFunction(),
            "data" to storage.getGlobalData(),
            "time" to this::luaTime.asVarArgFunction(),
            "schedule" to this::luaSchedule.asVarArgFunction(),
            "scheduleRepeating" to this::luaScheduleRepeating.asVarArgFunction(),
            "cancelTask" to this::luaCancelTask.asVarArgFunction(),
            "setBlock" to this::setBlock.asVarArgFunction(),
            "getBlock" to this::luaGetBlock.asVarArgFunction(),
            "fill" to this::fill.asVarArgFunction()
        )
    }
}