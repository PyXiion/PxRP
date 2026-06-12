---
title: Events
description: Complete examples of event-driven scripts.
---

## 1. Welcome Message

```lua
mc.on("player_join", function(player)
    mc.broadcast(player.displayName .. " joined the server!")
end)
```

## 2. Bad Word Filter

```lua
local badWords = {"badword1", "badword2", "badword3"}

mc.on("player_chat", function(player, message)
    for _, word in ipairs(badWords) do
        if message:lower():find(word) then
            player:sendMessage("Watch your language!")
            return false -- cancel the message
        end
    end
end)
```

## 3. Block Break Protection

```lua
mc.on("player_block_break", function(player, pos, blockId)
    if blockId == "minecraft:bedrock" then
        player:sendMessage("You cannot break bedrock!")
        return false -- cancel
    end
end)
```

## 4. TNT Placement Prevention

```lua
mc.on("player_block_place", function(player, pos, blockId)
    if blockId == "minecraft:tnt" then
        player:sendMessage("TNT is disabled on this server!")
        return false -- cancel
    end
end)
```
