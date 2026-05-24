# PxRP — Agent instructions

Fabric mod (MC 1.21.11) that lets server admins define chat commands via Lua scripts. Kotlin 2.2.21, Fabric Loom 1.16, Yarn, Java 21.

## Build, test & run

```
./gradlew build          # compiles + runs all tests (the only CI verification step)
./gradlew test           # runs unit tests (pure logic, no Minecraft runtime needed)
./gradlew runServer      # or runClient
```

- Shadow relocates `org.luaj` → `ru.pyxiion.lib.luaj`
- Access widener `pxrp.accesswidener` is empty. Build prints 5× `Cannot remap children/literals/command/requirement…` — cosmetic (safe to ignore).
- `run/` is gitignored
- CI: `.github/workflows/build.yml` — `./gradlew build` on push/PR to `main`; tag push `v*` creates release (jar excluding `-all` and `-sources`)

## Testing

- `src/test/kotlin/ru/pyxiion/pxrp/` — JUnit 5 via `kotlin-test-junit5`. All tests run without Minecraft runtime.
- 4 test files: `SyntaxParserTest`, `BuildVariantsTest`, `ChoiceTypeTest`, `BrigadierTreeTest` — cover syntax parsing, variant generation, parse-time choice validation, and tree topology (name-based dedup, sibling args, chaining).
- `BrigadierTreeTest` reflects `CommandNode.children` field directly — `getChildren()` returns a non-Map type on this classpath. Use `childrenField.get(node) as Map<*, *>`.
- To add tests: new file in `src/test/kotlin/ru/pyxiion/pxrp/`, use `kotlin.test.Test`, `kotlin.test.assertEquals`, etc.

## Project layout

```
src/main/java/ru/pyxiion/pxrp/
  PxRp.kt                  # lifecycle + event wiring
  LuaCmdLoader.kt          # Lua runtime, register() bridge
  LuaCommandManager.kt     # dynamic Brigadier tree management
  CommandSyntax.kt         # SyntaxParser, buildVariants, parseSyntaxString, ArgDef, ArgToken — standalone, no Minecraft deps
  LuaEventManager.kt       # mc.on() event bus
  Scheduler.kt             # mc.schedule/mc.scheduleRepeating/mc.cancelTask
  Utils.kt                 # luaTableOf(), checkPermission(), asVarArgFunction()
  api/                     # Player (Lua-facing), Vector, LuaMcApi
  types/                   # LuaArgumentType, ChoiceArgumentType, toLuaValue()
  storage/                 # DataTable, DataBackend, JsonBackend, StorageManager
  mixins/
    CommandNodeMixin.java  # @Accessor on Brigadier CommandNode fields
    MinecraftServerMixin   # @Inject on reloadResources → luaLoader.reload()
  coerce/KotlinToLua.kt    # DEAD CODE — entirely commented out
```

## Key conventions

- **Russian error messages** in logs and chat — do not flag as bugs
- **`Utils.kt` (root)** and **`types/Utils.kt`** are separate files with distinct helpers
- **`rawset` vs `set` on Player**: After `setmetatable()`, all `LuaTable.set()` goes through `__newindex`. Methods and data writes use `t.rawset(key, value)`. Property writes (`health`, `food`, `gamemode`) route through `__newindex`. Any new key added after `setmetatable` MUST use `rawset`.
- **Permission propagation**: Parent literal nodes require OR of their children's permissions. If any child has no permission (nil), parent is unrestricted.
- **`mergeOrBuildArgsFrom` matches by `arg.name`** (not `arg.type`). Two commands under the same literal with different argument names register as separate children. This handles `register("gamemode <mode:choice=a,b> ...")` + `register("gamemode <mode2:choice=c,d> ...")` correctly — `mode` and `mode2` become distinct sibling nodes.
- **Choice args validate at parse time** — `ChoiceType` is a custom Brigadier `ArgumentType<String>`. Different choice sets are different types (`equals`/`hashCode` based on choices list). This enables Brigadier to disambiguate: `mode:choice=creative,spectator` and `mode2:choice=survival,adventure` reject each other's input at parse time.
- **Reserved commands**: `pxrp`, `stop`, `reload`, `op`, `deop`, `ban`, `ban-ip`, `pardon`, `pardon-ip`, `save-all`, `save-on`, `save-off`, `whitelist` — top-level literals blocked in `addCommand()`.

## Register syntax

```lua
register("cmd <name:type> [<name:type>]", handler, permission?)
```

| Part | Meaning |
|------|---------|
| `cmd` `sub` | Literal path tokens — create Brigadier literal nodes |
| `<name:type>` | Required argument. Missing `:type` raises a parse error |
| `[<name:type>]` | Optional trailing argument. Everything from first `[...]` onward is optional. Missing → `nil` in handler. Internally registers N+1 variants |
| `<name:choice=x,y>` | Choice type — custom Brigadier `ArgumentType<String>`, validates at parse time (rejects invalid values before execution), tab-completes options |

**Types**: `text`, `player` (or `target` alias), `int`, `double`, `float`, `bool`, `block_pos` (returns `{x,y,z}`), `choice=opt1,opt2,...`

**Handler**: `function(ctx, arg1, arg2, ...)` — `ctx.player` is a live wrapper (always reads from entity, not snapshot). `ctx.player.data` is a per-player DataTable.

## Storage

- JSON at `config/pxrp/storage/global.json` and `config/pxrp/storage/players/<uuid>.json`
- Atomic writes via temp file + atomic move
- DataTable validates types (no cyclic refs, no functions/userdata/threads)
- Nested table assignments require re-assignment: `data.nested = t` not `data.nested.key = v`
- **Saved on:** server stop, player disconnect, `/pxrp reload`. **Not saved on every write** — batching.

## Scheduler

- Ticked via `ServerTickEvents.END_SERVER_TICK` → `Scheduler.tick()`
- Delay/interval in ticks (20 ticks = 1 sec)
- `mc.cancelTask(id)` — returns `false` if `id >= nextId` (never scheduled) or already cancelled
- All tasks cleared on `/pxrp reload` and server stop
- Individual callback errors caught and logged (other tasks unaffected)

## Block API

`mc.setBlock` / `mc.getBlock` / `mc.fill` — coordinates floored, blockId auto-prefixed with `minecraft:` if no namespace, fill volume capped at 32,768 blocks, flag `0x02` (no neighbor updates for fill), `setBlockState` loads chunks on demand.

## Lua environment

- Runtime: `org.luaj:luaj-jse:3.0.1`
- Config dir: `config/pxrp/` (all `.lua` alphabetically). Falls back to single `config/pxrp.lua`. First run creates `demo.lua` from resource.
- `package.path`: `config/pxrp/?.lua;config/pxrp/?/init.lua;?.lua`
- Loaded std libs: `math`, `string`, `table`, `bit32`, `package`, base lib. **Not loaded**: `io`, `os`, `coroutine`, `debug`
- `require "format"` → `format(template)` / `broadcastFormat(template)`
- `require "simple"` → `registerSimple(syntax, template, range?, overlay?)`
