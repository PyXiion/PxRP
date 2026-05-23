simple = {
    defaultOverlay = 20 * 7
}

function registerSimple(cmd, args, template, range, overlay)
    local argNames = {}
    local typeCounts = {}

    for i, arg in ipairs(args) do
        local name, typ
        -- Разбираем аргумент на имя и тип
        if arg:find(":") then
            name, typ = arg:match("([^:]+):([^:]+)")
        else
            typ = arg
            -- Генерируем имя на основе типа
            typeCounts[typ] = (typeCounts[typ] or 0) + 1
            if typeCounts[typ] > 1 then
                name = typ .. tostring(typeCounts[typ] - 1)
            else
                name = typ
            end
        end
        table.insert(argNames, name)
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

    -- Регистрируем команду
    register(cmd, args, handler)
end