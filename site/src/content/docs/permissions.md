---
title: Permissions
description: Permission nodes, Fabric Permissions API integration, and runtime checks.
---

Permissions control who can use registered commands. They integrate with the Fabric Permissions API, supporting any permission provider such as LuckPerms.

## Registering Permissions

Pass a permission node as the third argument to `register()`:

```lua
register("broadcast <message:text>", function(ctx, message)
    ctx.player:sendMessage(message)
end, "myplugin.broadcast")
```

- `nil` permission (no third argument) means **unrestricted** — any player can use the command.
- A string permission node restricts the command to players who have that node.

## Parent Literal Nodes

When a command has multiple subcommands, the parent literal node requires the **OR** of all child permissions. A player must have at least one child permission to see the parent tab-completion.

```
/gamemode creative        requires "myplugin.gamemode.creative"
/gamemode survival        requires "myplugin.gamemode.survival"
  Parent "gamemode"        requires "myplugin.gamemode.creative" OR "myplugin.gamemode.survival"
```

## Runtime Permission Checks

Use `player:hasPermission(perm)` to check permissions in Lua code:

```lua
register("fly", function(ctx)
    if ctx.player:hasPermission("myplugin.fly") then
        ctx.player:setFlySpeed(0.1)
        ctx.player:sendMessage("Flight enabled!")
    else
        ctx.player:sendMessage("You don't have permission to fly.")
    end
end)
```

This works with any Fabric Permissions API provider — LuckPerms, permission files, or OP levels.

## Examples

**OP-based fallback:** If no permission plugin is installed, the system falls back to Minecraft's OP system.

```lua
register("killall", function(ctx)
    ctx.player:sendMessage("Killed all entities")
end, "myplugin.killall")

register("heal", function(ctx, target)
    ctx.player:sendMessage("Healed " .. target.name)
end, "myplugin.heal")
```
