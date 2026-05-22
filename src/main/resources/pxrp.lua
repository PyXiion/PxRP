require "format"
require "simple"


function fart()
    local pos = player.pos
    local dir = player.bodyDir

    broadcastFormat "*{p.name} пёрнул*" {p=player}
    mc.particle("minecraft:gust", pos.x - dir.x * 0.5, pos.y + 0.6, pos.z - dir.z * 0.5, player.world)
    mc.playSound("minecraft:entity.slime.squish", pos.x, pos.y, pos.z, player.world, 10, 0.1)
end

function kill(target)
    broadcastFormat "*{p.name} убил(а) {t.name}*" {p=player, t=target}
end


register("fart", {}, fart)
register("rp kill", {"target"}, kill, "rp.kill")