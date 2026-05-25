package ru.pyxiion.pxrp.api

import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.util.Optional
import java.util.UUID

class PersonalSidebarManager(private val server: MinecraftServer) {
    private val sidebars = mutableMapOf<UUID, PersonalSidebarData>()

    data class PersonalSidebarData(
        val objectiveName: String,
        val title: String,
        val lines: List<String>
    )

    fun setSidebar(player: ServerPlayerEntity, lines: List<String>, title: String?) {
        removeForPlayer(player)

        val objectiveName = "_pxrp_${player.uuid.toString().take(8)}"
        val displayTitle = title ?: "§6§lПерсональный сайдбар"

        val sideData = PersonalSidebarData(objectiveName, displayTitle, lines)
        sidebars[player.uuid] = sideData

        sendCreatePackets(player, sideData)
    }

    fun getSidebar(player: ServerPlayerEntity): PersonalSidebarData? {
        return sidebars[player.uuid]
    }

    fun setSidebarTitle(player: ServerPlayerEntity, title: String) {
        val data = sidebars[player.uuid] ?: return
        val newData = data.copy(title = title)
        sidebars[player.uuid] = newData

        val objective = makeObjective(newData.objectiveName, newData.title)
        player.networkHandler.sendPacket(
            ScoreboardObjectiveUpdateS2CPacket(objective, ScoreboardObjectiveUpdateS2CPacket.UPDATE_MODE)
        )
    }

    fun setSidebarLines(player: ServerPlayerEntity, lines: List<String>) {
        val data = sidebars[player.uuid] ?: return
        sendRemovePackets(player, data)
        val newData = data.copy(lines = lines)
        sidebars[player.uuid] = newData
        sendCreatePackets(player, newData)
    }

    fun clearSidebar(player: ServerPlayerEntity) {
        removeForPlayer(player)
    }

    fun restoreForPlayer(player: ServerPlayerEntity) {
        val data = sidebars[player.uuid] ?: return
        sendCreatePackets(player, data)
    }

    fun removeForPlayer(player: ServerPlayerEntity) {
        val data = sidebars.remove(player.uuid) ?: return
        sendRemovePackets(player, data)
    }

    private fun sendCreatePackets(player: ServerPlayerEntity, data: PersonalSidebarData) {
        val objective = makeObjective(data.objectiveName, data.title)

        player.networkHandler.sendPacket(
            ScoreboardObjectiveUpdateS2CPacket(objective, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE)
        )

        for ((index, line) in data.lines.withIndex()) {
            val score = data.lines.size - index
            player.networkHandler.sendPacket(
                ScoreboardScoreUpdateS2CPacket(
                    "_l$index",
                    data.objectiveName,
                    score,
                    Optional.of(Text.literal(line)),
                    Optional.empty()
                )
            )
        }

        player.networkHandler.sendPacket(
            ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, objective)
        )
    }

    private fun sendRemovePackets(player: ServerPlayerEntity, data: PersonalSidebarData) {
        player.networkHandler.sendPacket(
            ScoreboardObjectiveUpdateS2CPacket(
                makeObjective(data.objectiveName, data.title),
                ScoreboardObjectiveUpdateS2CPacket.REMOVE_MODE
            )
        )
    }

    private fun makeObjective(name: String, title: String): ScoreboardObjective {
        return ScoreboardObjective(
            server.scoreboard,
            name,
            ScoreboardCriterion.DUMMY,
            Text.literal(title),
            ScoreboardCriterion.RenderType.INTEGER,
            false,
            null
        )
    }
}
