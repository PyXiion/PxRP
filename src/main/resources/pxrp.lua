require "format"
require "simple"


function fart(ctx)
    local player = ctx.player
    local pos = player.pos
    local dir = player.bodyDir
    local now = mc.time()
    local last = player.data.lastFart or 0

    if now - last < 10 then
        mc.broadcast("Wait " .. (10 - (now - last)) .. " seconds to fart again!")
        return
    end

    player.data.lastFart = now

    broadcastFormat "*{p.name} farted*" {p=player}
    mc.particle("minecraft:gust", pos.x - dir.x * 0.5, pos.y + 0.6, pos.z - dir.z * 0.5, player.world)
    mc.playSound("minecraft:entity.slime.squish", pos.x, pos.y, pos.z, player.world, 10, 0.1)
end

function kill(ctx, target)
    broadcastFormat "*{p.name} killed {t.name}*" {p=ctx.player, t=target}
end

function coins(ctx)
    local bal = ctx.player.data.coins or 0
    if bal < 100 then
        mc.broadcast("You have " .. bal .. " coins")
    else
        mc.broadcast("You have a lot of coins! (" .. bal .. ")")
    end
end

function giveCoins(ctx)
    ctx.player.data.coins = (ctx.player.data.coins or 0) + 10
    mc.broadcast("+10 coins!")
end

function payCoins(ctx, target)
    local amount = 10
    local bal = ctx.player.data.coins or 0

    if bal < amount then
        mc.broadcast("Недостаточно монет! У тебя только " .. bal)
        return
    end

    ctx.player.data.coins = bal - amount
    target.data.coins = (target.data.coins or 0) + amount
    mc.broadcast(ctx.player.name .. " передал " .. amount .. " монет " .. target.name)
end

function eventStats()
    local total = (mc.data.totalEvents or 0) + 1
    mc.data.totalEvents = total
    mc.broadcast("Серверное событие #" .. total .. " запущено!")
end


register("fart", {}, fart)
register("rp kill", {"target"}, kill, "rp.kill")
register("coins", {}, coins)
register("rp coins give", {}, giveCoins)
register("rp pay", {"target"}, payCoins)
register("rp event", {}, eventStats)