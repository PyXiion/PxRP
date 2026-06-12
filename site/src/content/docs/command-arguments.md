---
title: Command Arguments
description: Argument types, syntax, and optional parameters in command registration.
---

## Argument Types

Arguments are declared with `<name:type>` in the syntax string.

| Type | Description | Lua type |
|------|-------------|----------|
| `text` | Multi-word string (remainder of input) | `string` |
| `word` | Single word (no spaces) | `string` |
| `player` | Online player name (alias: `target`) | `player` wrapper |
| `int` | Whole number | `number` |
| `double` | Decimal number | `number` |
| `float` | Decimal number | `number` |
| `bool` | Boolean value | `boolean` |
| `block_pos` | Block coordinates | `{x, y, z}` table |
| `choice=x,y` | One of the listed options | `string` |

## Required vs Optional

**Required arguments** use angle brackets and must always be provided:

```lua
register("teleport <player:player> <x:int> <y:int> <z:int>", function(ctx, target, x, y, z)
    target:setPos(x, y, z)
end)
```

**Optional arguments** use square brackets. The first optional argument makes everything after it optional:

```lua
register("greet <player:player> [<message:text>]", function(ctx, target, message)
    if message then
        target:sendMessage(message)
    else
        target:sendMessage("Hello!")
    end
end)
```

Any missing optional parameters become `nil` in Lua:

```lua
register("kick <player:player> [<reason:text>]", function(ctx, target, reason)
    -- reason is nil if not provided
    target:kick(reason or "Kicked by an operator")
end)
```

## Examples

**Block positions:**

```lua
register("setwarp <name:word> <pos:block_pos>", function(ctx, name, pos)
    -- pos is {x, y, z}
    ctx.player:sendMessage("Warp " .. name .. " set at " .. pos.x .. ", " .. pos.y .. ", " .. pos.z)
end)
```

**Choices:**

```lua
register("time set <value:choice=day,night,noon,midnight>", function(ctx, value)
    ctx.player:sendMessage("Time set to " .. value)
end)
```

**Mixed optional params:**

```lua
register("message <player:player> <message:text>", function(ctx, target, msg)
    target:sendMessage(msg)
end)
```
