# PxRP

> [!NOTE]
> Beware that this project is also coded by AI. Humans included too.

x![version](https://img.shields.io/badge/version-0.3.0-blue)

A Lua-scriptable roleplay command framework for Minecraft Fabric servers. Define custom chat commands using Lua scripts — no Java or Kotlin mod code required.

## Features

- **Lua-driven commands** — Write `.lua` files to register Brigadier commands with tab completion, argument parsing, and permission checks.
- **Event system** — React to player joins, leaves, deaths, chat messages, and server lifecycle with Lua handlers.
- **Dynamic reload** — `/pxrp reload` re-executes all Lua scripts without restarting the server.
- **Built-in argument types** — `text` (free-form message) and `target` (player selector with tab completion).
- **Minecraft API exposed to Lua** — particles, sounds, global and range-limited broadcasting, scheduling (delayed and repeating tasks), block manipulation (set/get/fill).
- **Persistent data storage** — Key-value data per player (`ctx.player.data`) and globally (`mc.data`), auto-persisted to JSON.
- **Permission system** — Uses the Fabric Permissions API (supports both OP-based and permissions-plugin-based systems).
- **Player context** — Command handlers receive the sender as a full Player object with readable properties (health, position, inventory, gamemode, etc.) and callable methods (`sendMessage`, `teleport`, `kick`, `playSound`, `give`).
- **Lua libraries** — Bundled `format.lua` (f-string-like templating) and `simple.lua` (concise command registration).

## Requirements

- Minecraft 1.21.x
- Fabric Loader ≥0.19.2
- Fabric API ≥0.141.4
- Fabric Language Kotlin ≥1.10.8

## Quick start

1. Install the mod on your Fabric server.
2. On first run, `config/pxrp/demo.lua` is created with example scripts.
3. Run `/pxrp reload` (requires operator level 4 or `pyxiion.pxrp` permission) to apply changes.

## Examples

### Basic command

```lua
register("fart", {}, function(ctx)
    local player = ctx.player
    local pos = player.pos
    local dir = player.bodyDir

    broadcastFormat "*{p.name} farted*" {p = player}
    mc.particle("minecraft:gust", pos.x - dir.x * 0.5, pos.y + 0.6, pos.z - dir.z * 0.5, player.world)
    mc.playSound("minecraft:entity.slime.squish", pos.x, pos.y, pos.z, player.world, 10, 0.1)
end)
```

### Arguments

Argument types can be given custom names with the `name:type` syntax:

```lua
register("rp kill", {"target"}, function(ctx, target)
    broadcastFormat "*{p.name} killed {t.name}*" {p = ctx.player, t = target}
end, "rp.kill")

-- Custom arg names are useful when you have multiple args of the same type:
register("try", {"action:text"}, function(ctx, action)
    mc.broadcast(ctx.player.name .. " tries to " .. action)
end)
```

### Persistent player data

Every player has a `ctx.player.data` Lua table that persists automatically to disk.

```lua
register("coins", {}, function(ctx)
    local bal = ctx.player.data.coins or 0
    mc.broadcast("You have " .. bal .. " coins")
end)

register("rp coins give", {}, function(ctx)
    ctx.player.data.coins = (ctx.player.data.coins or 0) + 10
    mc.broadcast("+10 coins! Total: " .. ctx.player.data.coins)
end)
```

### Cross-player data

Data tables are shared — modifying another player's data from a command works:

```lua
register("rp pay", {"target"}, function(ctx, target)
    local bal = ctx.player.data.coins or 0
    if bal < 10 then
        mc.broadcast("Not enough coins! You have " .. bal)
        return
    end
    ctx.player.data.coins = bal - 10
    target.data.coins = (target.data.coins or 0) + 10
    mc.broadcast(ctx.player.name .. " paid 10 coins to " .. target.name)
end)
```

### Global server data

`mc.data` is a server-wide persistent table shared by all players.

```lua
register("rp event", {}, function()
    local total = (mc.data.totalEvents or 0) + 1
    mc.data.totalEvents = total
    mc.broadcast("Server event #" .. total .. " started!")
end)
```

### Bundled Lua libraries

The mod ships with two Lua libraries loaded via `require`:

```lua
require "format"    -- provides format() and broadcastFormat()
require "simple"    -- provides registerSimple()
```

Include these at the top of any script in `config/pxrp/` to use them.

#### `format.lua` — template engine

Templates use `{expr}` placeholders with dot-notation access:

```lua
format(pattern)(args)              -- returns formatted string
broadcastFormat(pattern)(args)     -- formats and broadcasts in one call
```

```lua
broadcastFormat "*{p.name} throws a fireball at {t.name}*" {p = ctx.player, t = target}
```

#### `simple.lua` — concise command registration

`registerSimple` creates a command that formats and broadcasts a template, passing `p = ctx.player` automatically:

```lua
registerSimple(cmd, args, template, range?, overlay?)
```

| Param | Type | Description |
|-------|------|-------------|
| `cmd` | `string` | Command path |
| `args` | `table` | Argument type list (same as `register`) |
| `template` | `string` | Format template (`{p.name}`, `{argName}`, etc.) |
| `range` | `number?` | If > 0, uses `broadcastInRange` with this radius |
| `overlay` | `boolean\|number?` | Send as title overlay: `true` = 7s, or a custom tick count |

```lua
-- Global broadcast
registerSimple("wave", {}, "*{p.name} waves at everyone*", 15)    -- range 15 blocks
registerSimple("bow", {}, "*{p.name} bows*", nil, true)           -- title overlay
registerSimple("cheer", {}, "*{p.name} cheers*", 20, 60)          -- both
```

### Events

`mc.on(event, handler)` registers a handler that fires when a game event occurs. Handlers are cleared on `/pxrp reload`.

```lua
mc.on("player_join", function(player)
    mc.broadcast("Welcome, " .. player.name .. "!")
end)

mc.on("player_leave", function(player)
    mc.broadcast(player.name .. " left the server.")
end)

mc.on("player_death", function(player, damageType)
    mc.broadcast(player.name .. " died to " .. damageType)
end)

mc.on("player_chat", function(player, message)
    if message == "hello" then
        mc.broadcast(player.name .. " said hello!")
    end
end)

mc.on("server_start", function()
    mc.data.startTime = mc.time()
end)

mc.on("server_stop", function()
    local uptime = mc.time() - (mc.data.startTime or mc.time())
    mc.data.lastUptime = uptime
end)
```

| Event | Handler args | Fires | Cancellable |
|-------|-------------|-------|:-----------:|
| `player_join` | `player` | Player joins the server | ✓ |
| `player_leave` | `player` | Player disconnects | ✗ |
| `player_death` | `player`, `damageType` | Player dies (`"fall"`, `"player_attack"`, etc.) | ✗ |
| `player_chat` | `player`, `message` | Player sends a chat message | ✓ |
| `server_start` | — | Server finishes starting (after Lua reload) | ✗ |
| `server_stop` | — | Server is stopping (before save) | ✗ |

The `player` parameter is the same Player object used in command handlers (`ctx.player`). Multiple handlers per event are allowed; an error in one doesn't affect others.

### Cancelling events

For `player_join` and `player_chat`, returning `false` from the handler cancels the action:

```lua
-- Kick the player if they're banned
mc.on("player_join", function(player)
    local bans = mc.data.bans or {}
    if bans[player.name] then
        return false  -- player is disconnected
    end
end)

-- Block messages containing swear words
mc.on("player_chat", function(player, message)
    local blocked = {"badword1", "badword2"}
    for _, word in ipairs(blocked) do
        if message:find(word) then
            return false  -- message is suppressed
        end
    end
end)
```

- `player_join`: fires before the player fully connects (via `INIT`). Returning `false` disconnects them.
- `player_chat`: fires before the message is broadcast (via `ALLOW_CHAT_MESSAGE`). Returning `false` blocks the message.
- Other events (`player_leave`, `player_death`, `server_start`, `server_stop`) are observational only — return values are ignored.
- Note: disconnecting a rejected player during `player_join` triggers `player_leave` as well. Scripts that broadcast on leave may show a ghost message for rejected players.

### Built-in Lua standard libraries

PxRP loads the following Lua standard libraries via [`luaj`](https://github.com/luaj/luaj) (targeting Lua 5.1):

| Library | Globals | Reference |
|---------|---------|-----------|
| **Base** | `type`, `tostring`, `tonumber`, `pairs`, `ipairs`, `pcall`, `error`, `assert`, `select`, `unpack`, `_G`, etc. | [§2–6](https://www.lua.org/manual/5.1/manual.html#2) |
| **math** | `math.random`, `math.randomseed`, `math.floor`, `math.ceil`, `math.sin`, `math.cos`, `math.sqrt`, `math.min`, `math.max`, `math.pi`, `math.huge` | [§5.6](https://www.lua.org/manual/5.1/manual.html#5.6) |
| **string** | `string.format`, `string.sub`, `string.find`, `string.match`, `string.gmatch`, `string.gsub`, `string.len`, `string.byte`, `string.char`, `string.rep`, `string.lower`, `string.upper` | [§5.4](https://www.lua.org/manual/5.1/manual.html#5.4) |
| **table** | `table.insert`, `table.remove`, `table.sort`, `table.concat`, `table.maxn` | [§5.5](https://www.lua.org/manual/5.1/manual.html#5.5) |
| **bit32** | `bit32.band`, `bit32.bor`, `bit32.bxor`, `bit32.lshift`, `bit32.rshift`, `bit32.arshift`, `bit32.bnot` | [luaj wiki](https://github.com/luaj/luaj) |
| **package** | `require`, `package.path`, `package.loaded`, `package.preload` | [§5.3](https://www.lua.org/manual/5.1/manual.html#5.3) |

The following standard libraries are **not** loaded: `io`, `os`, `coroutine`, `debug`.

See the complete [Lua 5.1 Reference Manual](https://www.lua.org/manual/5.1/) for detailed documentation.

## Lua API

### `register(syntax, handler, permission?)`

Registers a Brigadier command. The syntax string combines the command path and argument definitions in a single string.

- `syntax` — Command syntax, e.g. `"msg <target:player> <msg:text>"`. Literal tokens form the path, `<name:type>` tokens define typed arguments. Missing `:type` on an angle-bracket token raises an error. Optional trailing args: `[<name:type>]` (everything from the first `[...]` onward is optional; missing options are `nil` in the handler). Subcommands use literal tokens: `"admin mute <target:player>"`.
- `handler` — Lua function called when the command executes. The first argument is always a Context object (`ctx`); subsequent arguments are the parsed command arguments in order.
- `permission` — Optional permission node string.

Supported argument types:

| Type | Lua value | Minecraft source |
|------|-----------|-----------------|
| `text` | string | Free-form message (trailing text) |
| `player` / `target` | Player object | Player selector (must target exactly one player) |
| `int` | number | Integer value |
| `double` | number | Double-precision decimal |
| `float` | number | Single-precision decimal |
| `bool` | boolean | Boolean (`true`/`false`) |
| `block_pos` | table `{x, y, z}` | Block position (x y z coordinates) |
| `choice=a,b,c` | string | One of the comma-separated options (tab-completes, validates at runtime) |

Reserved commands — the following top-level command names cannot be overridden via `register()` (an error is thrown at registration time):
`pxrp`, `stop`, `reload`, `op`, `deop`, `ban`, `ban-ip`, `pardon`, `pardon-ip`, `save-all`, `save-on`, `save-off`, `whitelist`. These are critical server management commands that must not be shadowed.

Optional arguments — everything from the first `[<name:type>]` onward is optional. Internally registers N+1 Brigadier variants so the handler is called without the optional arg(s). Missing params become `nil` in Lua.

Examples:
```lua
register("msg <target:player> <msg:text>",   handler)
register("mute <target:player>",             handler, "pxrp.mod")
register("setblocks <pos:block_pos> <block:text>", handler, "pxrp.admin")
register("homelist",                           handler)

-- Choice type — tab suggestions + validation
register("gamemode <mode:choice=creative,survival,adventure,spectator> [<target:player>]", handler, "pxrp.admin")

-- Optional args — reason is nil when omitted
register("kick <target:player> [<reason:text>]", handler, "pxrp.mod")
```

### Context object

Every command handler receives a **Context** object as its first argument. The context exposes the executing player:

| Field | Type | Description |
|---|---|---|
| `ctx.player` | `Player` | The player who executed the command |

### Player object

The Player object is accessed via `ctx.player` inside a command handler (or as the first arg in events like `player_join`). It's a live wrapper around the Minecraft player — every property read fetches the current state from the entity.

#### Properties

| Property | Type | Settable | Description |
|----------|------|:--------:|-------------|
| `name` | string | ✗ | Player name |
| `uuid` | string | ✗ | UUID string |
| `world` | string | ✗ | World path (e.g. `"overworld"`) |
| `pos` | `{x, y, z}` | ✗ | Position |
| `dir` | `{x, y, z}` | ✗ | Look direction |
| `bodyDir` | `{x, z}` | ✗ | Body yaw direction |
| `health` | number | ✓ | Current health |
| `maxHealth` | number | ✗ | Max health |
| `food` | number | ✓ | Food level |
| `saturation` | number | ✗ | Saturation |
| `gamemode` | string | ✓ | Gamemode (`"survival"`, `"creative"`, etc.) |
| `ping` | number | ✗ | Latency (ms) |
| `xpLevel` | number | ✗ | Experience level |
| `xpProgress` | number | ✗ | XP progress (0–1) |
| `isOp` | boolean | ✗ | Operator status |
| `displayName` | string | ✗ | Display name |
| `isSneaking` | boolean | ✗ | Sneaking state |
| `isSprinting` | boolean | ✗ | Sprinting state |
| `selectedSlot` | number | ✗ | Hotbar slot |
| `fallDistance` | number | ✗ | Fall distance |
| `isFlying` | boolean | ✗ | Flying state |
| `air` | number | ✗ | Air ticks |
| `maxAir` | number | ✗ | Max air ticks |

Setting a read-only property logs a warning and does nothing.

#### Methods

Methods are called with `:` syntax:

```lua
ctx.player:sendMessage("Hello!")
ctx.player:teleport(100, 64, 200)
```

| Method | Args | Description |
|--------|------|-------------|
| `sendMessage` | text | Sends a chat message |
| `sendActionBar` | text | Action bar overlay |
| `sendTitle` | title, [subtitle=nil] | Title + optional subtitle |
| `kick` | [reason="Kicked"] | Disconnects the player |
| `teleport` | x, y, z, [worldName=nil] | Teleport (intra-world or cross-dimension) |
| `damage` | amount | Deal generic damage |
| `heal` | amount | Heal health |
| `playSound` | id, [volume=1], [pitch=1] | Play a sound to the player |
| `give` | itemId, [count=1] | Give an item |
| `clear` | — | Clear inventory |

#### Per-player persistent storage (`ctx.player.data`)

A Lua table that persists to disk (<config>/pxrp/storage/players/<uuid>.json). Data is written to disk when:
- The server stops
- A player disconnects
- `/pxrp reload` or F3+T is executed

This batching approach improves performance for scripts that make multiple data assignments.

```lua
ctx.player.data.coins = (ctx.player.data.coins or 0) + 1          -- queued for write
ctx.player.data.inventory = {sword = 1, shield = 1}               -- queued for write
ctx.player.data.nested.key = "value"                               -- ❌ will NOT save
-- Fix:
local t = ctx.player.data.nested or {}
t.key = "value"
ctx.player.data.nested = t                                         -- ✅ queued for write
```

### `mc.data` — Persistent global storage

A server-wide persistent table (<config>/pxrp/storage/global.json). Data is written to disk when:
- The server stops
- A player disconnects
- `/pxrp reload` is executed

```lua
mc.data.eventActive = true
mc.data.totalPlayers = (mc.data.totalPlayers or 0) + 1
```

### `mc.particle(id, x, y, z, world)`

Spawns a particle at the given coordinates for all online players.

### `mc.playSound(id, x, y, z, world, volume?, pitch?)`

Plays a sound at the given coordinates.

### `mc.broadcast(text, overlay?)`

Sends a chat message to all players. If `overlay` is a number, sends a title overlay for that many ticks.

### `mc.broadcastInRange(text, x, y, z, range, world, overlay?)`

Sends a message only to players within the given radius in the given world.

### `mc.time()`

Returns the current server epoch time in seconds (as a double). Useful for cooldowns:

```lua
local last = ctx.player.data.lastFart or 0
if mc.time() - last < 10 then
    mc.broadcast("Wait " .. (10 - (mc.time() - last)) .. " seconds!")
    return
end
ctx.player.data.lastFart = mc.time()
```

### `mc.schedule(delay, callback)`

Runs a Lua function once after `delay` ticks (20 ticks = 1 second). Returns a task ID that can be used with `mc.cancelTask`.

```lua
mc.schedule(40, function()
    mc.broadcast("§e2 seconds have passed!")
end)
```

### `mc.scheduleRepeating(delay, interval, callback)`

Runs a Lua function repeatedly. The first execution happens after `delay` ticks, then every `interval` ticks after that. Returns a task ID.

```lua
local id = mc.scheduleRepeating(0, 20, function()
    local time = mc.data.gameTime or 0
    time = time + 1
    mc.data.gameTime = time
    mc.broadcast("§e" .. time .. " second(s) have passed!")
end)
```

### `mc.cancelTask(id)`

Cancels a previously scheduled task by its ID. Returns `true` if the task was found and cancelled, `false` otherwise.

```lua
local id = mc.schedule(100, function()
    mc.broadcast("This will never run!")
end)
mc.cancelTask(id)
```

- All tasks are automatically cancelled on `/pxrp reload` and server stop.
- Callback errors are caught and logged per-task without affecting other tasks.
- **Important**: callbacks run on the server tick thread — do not perform blocking operations.

### `mc.setBlock(x, y, z, blockId, world)`

Places a block at the given position. `blockId` defaults to `minecraft:` namespace if omitted — `"stone"` is equivalent to `"minecraft:stone"`. Triggers full neighbor updates (redstone, water flow, observers), same as placing a block manually.

```lua
mc.setBlock(0, 64, 0, "diamond_block", "minecraft:overworld")
```

### `mc.getBlock(x, y, z, world)` → string

Returns the registry ID of the block at the given position, e.g. `"minecraft:stone"` or `"minecraft:air"`.

```lua
local block = mc.getBlock(0, 4, 0, "minecraft:overworld")
if block == "minecraft:air" then
    mc.broadcast("The floor was broken!")
end
```

### `mc.fill(x1, y1, z1, x2, y2, z2, blockId, world)`

Fills a cuboid region. No neighbor updates are sent — blocks appear instantly to clients without causing observer/redstone cascades. Volume capped at 32,768 blocks (≈ 32×32×32). Unloaded chunks are loaded on demand, same as vanilla `/fill`.

```lua
-- Reset an arena floor
mc.fill(-10, 4, -10, 10, 4, 10, "glass", "minecraft:overworld")
```

### `mc.on(event, handler)`

Registers a Lua handler for a game event. See the [Events](#events) section for available events and examples.

## Storage

Data is stored as JSON in `config/pxrp/storage/`:

- `config/pxrp/storage/global.json` — Global data
- `config/pxrp/storage/players/<uuid>.json` — Per-player data

The storage backend is abstract (`DataBackend` interface). Currently ships with `JsonBackend`. The interface allows adding SQLite or PostgreSQL backends later without changing Lua code.

## License

GNU Lesser General Public License v3.0. See [LICENSE.txt](LICENSE.txt).
