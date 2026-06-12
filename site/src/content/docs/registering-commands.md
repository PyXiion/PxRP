---
title: Registering Commands
description: Register dynamic chat commands with Lua handlers.
---

Use `register()` to create dynamic Brigadier commands without touching Java code.

## Signature

```lua
register(syntax, handler, permission?)
```

| Param | Type | Description |
|-------|------|-------------|
| `syntax` | `string` | Command syntax string |
| `handler` | `function` | Callback invoked when the command runs |
| `permission?` | `string` | Optional permission node (see [Permissions](/permissions)) |

## Context Object

The handler receives a context table as its first argument:

```lua
register("ping", function(ctx)
    ctx.player:sendMessage("Pong!")
end)
```

Properties on `ctx`:

| Property | Type | Description |
|----------|------|-------------|
| `ctx.player` | `player` | Live player wrapper — calls mutate the real player in real time |

## Reserved Commands

The following commands **cannot** be registered (silently blocked):

`pxrp`, `stop`, `reload`, `op`, `deop`, `ban`, `ban-ip`, `pardon`, `pardon-ip`, `save-all`, `save-on`, `save-off`, `whitelist`

## Examples

**Basic registration — no permission:**

```lua
register("healme", function(ctx)
    ctx.player:heal(20)
    ctx.player:sendMessage("Healed!")
end)
```

**With a permission node:**

```lua
register("fly <player:player>", function(ctx, target)
    target:setFlySpeed(0.1)
    target:sendMessage("Flight speed set!")
end, "myplugin.fly")
```

**Subcommands and arguments:**

```lua
register("warn <player:player> <reason:text>", function(ctx, target, reason)
    target:sendMessage("Warning: " .. reason)
    ctx.player:sendMessage("Warned " .. target.name)
end, "myplugin.warn")
```
