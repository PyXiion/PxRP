simple = {
    defaultOverlay = 20 * 7
}

function registerSimple(syntax, template, range, overlay)
    local argNames = {}
    for arg in syntax:gmatch("<([^:>]+):[^>]+>") do
        table.insert(argNames, arg)
    end

    overlay = overlay == true and simple.defaultOverlay or overlay

    local render = format(template)
    local handler = function(ctx, ...)
        local argValues = {...}
        local argTable = {p = ctx.player}
        for i, name in ipairs(argNames) do
            argTable[name] = argValues[i]
        end

        if range ~= nil and range > 0 then
            local pos = ctx.player.pos
            mc.broadcastInRange(render(argTable), pos.x, pos.y, pos.z, range, ctx.player.world, overlay)
        else
            mc.broadcast(render(argTable), overlay)
        end
    end

    register(syntax, handler)
end
