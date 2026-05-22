package ru.pyxiion.pxrp.api

import net.minecraft.server.network.ServerPlayerEntity

data class Player(
    private val player: ServerPlayerEntity,
    @JvmField
    val name: String,
    @JvmField
    val pos: Vector,
    @JvmField
    val dir: Vector,
    @JvmField
    val bodyDir: Vector,
    @JvmField
    val world: String
) {
    companion object {
        fun fromMcPlayer(player: ServerPlayerEntity): Player {
            return Player(
                player,
                player.name.literalString!!,
                Vector.fromMc(player.entityPos),
                Vector.fromMc(player.rotationVector),
                Vector.fromRotation(player.bodyYaw, 0.0f),
                player.entityWorld.registryKey.value.path
            )
        }
    }
}
