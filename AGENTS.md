# PxRP — Agent instructions

## What it is

A Fabric mod (Minecraft 1.21.11) that lets server admins define chat commands via Lua scripts. Kotlin-based, single-module Gradle project.

## Build & run

```sh
./gradlew build               # produces remapped shadow jar — the only verification step
./gradlew runServer           # Fabric Loom dev server
./gradlew runClient           # Fabric Loom dev client
```

- Java 21, Kotlin 2.2.21, Fabric Loom 1.16, Yarn 1.21.11+build.5
- No tests, no lint, no typecheck — `build` is the only gate
- Shadow plugin relocates `org.luaj` → `ru.pyxiion.lib.luaj`
- Access widener: `pxrp.accesswidener` (empty v2 header). Build will print 5× "Cannot remap children/literals/command/requirement…" — these are cosmetic (Loom auto-detects `@Accessor` targets on Brigadier's `CommandNode`, which isn't a Minecraft class — access widener only works on Vanilla classes). Safe to ignore.
- `run/` is gitignored

## CI

`.github/workflows/build.yml` runs `./gradlew build` on push/PR to `main`, and on tag push (`v*`) creates a GitHub release with the jar.

## Entrypoints

| Type | Class |
|------|-------|
| Mod initializer | `ru.pyxiion.pxrp.PxRp` |

## Project layout

```
src/main/java/ru/pyxiion/pxrp/
  PxRp.kt                  # ModInit — registers /pxrp reload, lifecycle hooks
  LuaCmdLoader.kt          # Loads Lua scripts, bridges register() calls to Brigadier, registers mc.on
  LuaEventManager.kt       # Event bus for Lua — mc.on(), fire(), clear()
  LuaCommandManager.kt     # Manages the dynamic Brigadier command tree
  Utils.kt                 # checkPermission(), luaTableOf(), asVarArgFunction()
  api/                     # Lua-facing API: Context, Player, Vector, LuaMcApi
  storage/                 # DataTable, DataBackend (interface), JsonBackend, StorageManager
  mixins/                  # CommandNodeMixin (accessors), MinecraftServerMixin (reload hook)
  types/                   # LuaArgumentType interface, KotlinInstance (LuaUserdata wrapper), Utils.kt (toLuaValue)
  coerce/                  # KotlinToLua.kt — entirely commented out, dead code
src/main/resources/
  pxrp.lua                 # Bundled default Lua config (copied to config/ on first run)
  format.lua               # F-string-like template engine (loaded via require)
  simple.lua               # registerSimple convenience wrapper
MODRINTH.md                # Simplified README for Modrinth project page
```

## Lua system

- Lua runtime: `org.luaj:luaj-jse:3.0.1`
- Config file at `config/pxrp.lua` (auto-created from bundled resource on first run)
- `/pxrp reload` re-executes all Lua scripts (also hooks `MinecraftServer.reloadResources` via mixin)
- `register(path, args, handler, permission?)` registers a Brigadier command from Lua
- Supported arg types: `text` (free-form), `target` (player selector)
- Custom named args: `"msg:text"` syntax overrides auto-generated name
- **Handler receives `ctx` as first arg** — the old `player` global is NOT set. Use `ctx.player` instead.
- `require "format"` loads format/broadcastFormat; `require "simple"` loads registerSimple
- Loaded Lua std libs: `math`, `string`, `table`, `bit32`, `package`, and base lib (`type`, `tostring`, `pairs`, etc.). **Not** loaded: `io`, `os`, `coroutine`, `debug`.

## Lua API surfaces

| Global | Source |
|--------|--------|
| `mc` | `LuaMcApi.kt` — particle, playSound, broadcast, broadcastInRange, time, data, on |
| `register` | `LuaCmdLoader.kt` |
| `format`, `broadcastFormat` | `format.lua` (requires `"format"`) |
| `registerSimple` | `simple.lua` (requires `"simple"`) |

- `mc.data` is a `DataTable` (persistent global storage)
- `ctx.player.data` is also a `DataTable` (per-player storage)
- `mc.time()` returns epoch seconds as double
- `ctx.player.world` returns the **path component** of the world key (e.g. `"overworld"`, not `"minecraft:overworld"`)

## Events

`mc.on(event, handler)` registers a Lua handler for a game event. Handlers are cleared on `/pxrp reload`.

| Event | Handler args | Fires |
|-------|-------------|-------|
| `player_join` | `player` | Player joins the server |
| `player_leave` | `player` | Player disconnects |
| `player_death` | `player`, `damageType` | Player dies (`damageType`: `"fall"`, `"player_attack"`, etc.) |
| `player_chat` | `player`, `message` | Player sends a chat message |
| `server_start` | — | Server finishes starting (after Lua reload) |
| `server_stop` | — | Server is stopping (before save) |

- `player` is a Player snapshot object — same shape as `ctx.player` in commands
- Multiple handlers per event are allowed; errors in one don't affect others
- Internally backed by `LuaEventManager.kt`

### Cancellation (`player_join`, `player_chat`)

Returning `false` from a handler cancels the action:

| Event | Mechanism | Returning `false`... |
|-------|-----------|---------------------|
| `player_join` | `ServerPlayConnectionEvents.INIT` | Disconnects the player |
| `player_chat` | `ServerMessageEvents.ALLOW_CHAT_MESSAGE` | Suppresses the message |

Other events (`player_leave`, `player_death`, `server_start`, `server_stop`) are observational — return values are ignored.

Note: disconnecting a player during `player_join` (INIT) triggers the server's normal disconnect flow, which also fires `player_leave`. Scripts that broadcast on `player_leave` may show a ghost leave message for rejected players.

## Storage

- JSON backend at `config/pxrp/storage/global.json` and `config/pxrp/storage/players/<uuid>.json`
- Atomic writes via temp file + atomic move
- DataTable: lazy-loaded, validates types (no cyclic refs, no functions/userdata/threads)
- Nested table assignments require re-assignment — `ctx.player.data.nested = t` not `ctx.player.data.nested.key = v`
- **Data is saved on**: server stop, player disconnect, `/pxrp reload` — never on every write (batching)

## Error messages

Kotlin-side log and user-facing messages are in Russian — do not flag as bugs.

## Notable conventions

- Mixin accessors on `CommandNode` (`getChildren`, `getLiterals`, `getCommand`, `setCommand`, `setRequirement`) used to dynamically patch the Brigadier tree at runtime — no need for access widener entries since Mixin handles visibility
- `KotlinInstance.kt` (`types/`) is a live `LuaUserdata` wrapper for Kotlin objects; `coerce/KotlinToLua.kt` is entirely commented out
- `Utils.kt` (root) and `types/Utils.kt` are separate files with distinct helpers
