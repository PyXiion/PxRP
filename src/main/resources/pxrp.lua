require "format"
require "simple"

math.randomseed(mc.time())

local L = {
    tryCritFail = "КРИТИЧЕСКИЙ ПРОВАЛ",
    tryFail = "ПРОВАЛ",
    trySuccess = "УСПЕХ",
    tryCritSuccess = "КРИТИЧЕСКИЙ УСПЕХ",
    tryFormat = "*%s пытается %s — %s (%d)*",
    rollFormat = "*%s бросает d%d и получает %d*",
    coinFlipFormat = "*%s подбрасывает монету — %s*",
    coinHeads = "орёл",
    coinTails = "решка",
    danceFormat = "*%s танцует*",
    stareFormat = "*%s уставился на %s*",
    hugFormat = "*%s обнимает %s*",
    bowTemplate = "*{p.name} кланяется*",
    waveTemplate = "*{p.name} машет всем*",
    laughTemplate = "*{p.name} смеётся*",
    cheerTemplate = "*{p.name} радуется*",
}


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


function tryCmd(ctx, action)
    local roll = math.random(100)
    local outcome
    if roll <= 20 then outcome = L.tryCritFail
    elseif roll <= 50 then outcome = L.tryFail
    elseif roll <= 80 then outcome = L.trySuccess
    else outcome = L.tryCritSuccess
    end
    mc.broadcast(string.format(L.tryFormat, ctx.player.name, action, outcome, roll))
end

function rollCmd(ctx)
    local result = math.random(100)
    mc.broadcast(string.format(L.rollFormat, ctx.player.name, 100, result))
end

function rollSidesCmd(ctx, sidesText)
    local sides = tonumber(sidesText) or 6
    local result = math.random(1, sides)
    mc.broadcast(string.format(L.rollFormat, ctx.player.name, sides, result))
end

function coinflipCmd(ctx)
    local result = math.random(2) == 1 and L.coinHeads or L.coinTails
    mc.broadcast(string.format(L.coinFlipFormat, ctx.player.name, result))
end

function danceCmd(ctx)
    local p = ctx.player
    mc.particle("minecraft:note", p.pos.x, p.pos.y + 1, p.pos.z, p.world)
    mc.playSound("minecraft:entity.experience_orb.pickup", p.pos.x, p.pos.y, p.pos.z, p.world, 1, 1.4)
    mc.broadcast(string.format(L.danceFormat, p.name))
end

function stareCmd(ctx, target)
    mc.broadcast(string.format(L.stareFormat, ctx.player.name, target.name))
end

function hugCmd(ctx, target)
    mc.particle("minecraft:heart", target.pos.x, target.pos.y + 1, target.pos.z, target.world)
    mc.broadcast(string.format(L.hugFormat, ctx.player.name, target.name))
end


register("fart", {}, fart)
register("rp kill", {"target"}, kill, "rp.kill")
register("coins", {}, coins)
register("rp coins give", {}, giveCoins)
register("rp pay", {"target"}, payCoins)
register("rp event", {}, eventStats)
register("try", {"action:text"}, tryCmd)
register("roll", {}, rollCmd)
register("rp roll", {"sides:text"}, rollSidesCmd)
register("rp coinflip", {}, coinflipCmd)
register("rp dance", {}, danceCmd)
register("rp stare", {"target"}, stareCmd)
registerSimple("rp bow", {}, L.bowTemplate)
registerSimple("rp wave", {}, L.waveTemplate)
registerSimple("rp laugh", {}, L.laughTemplate)
registerSimple("rp cheer", {}, L.cheerTemplate)
register("rp hug", {"target"}, hugCmd)