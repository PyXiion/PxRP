# PxRP — Agent instructions

Fabric mod (MC 1.21.11) that lets server admins define chat commands via Lua scripts. Kotlin 2.2.21, Fabric Loom 1.16, Yarn, Java 21.

## PxMC Code Design Guidelines

**Mantra: "Complexity must scale with the task, never with the platform."**

### 🎨 Lua API aesthetics (Clean Lua Rule)

**Properties over getters/setters** — expose clean fields via `__index`/`__newindex`, never force method-style access on Lua:
- Good: `player.helmet = item`, `player.health`
- Bad: `player:setHelmet(item)`, `player:getHealth()`

**Smart overloads** — minimal args for simple cases, table for complex. Never force empty placeholder arguments:
- Good: `mc.createItem("apple")`, `mc.createItem("apple", 64)`, `mc.createItem("sword", { name="God Sword", unbreakable=true })`
- Bad: `mc.createItem("apple", 1, {})` (forcing count + empty table)

### 🛠 Kotlin core responsibilities

**Mutation shield (mandatory cloning)** — Minecraft `ItemStack` references are mutable; sharing causes silent corruption. Any Kotlin method fetching from or injecting into the game must `copy()`. Never leak raw references to Lua.

**Encapsulated client sync** — modifying backend collections does not auto-update the client UI. Kotlin must handle sync internally (e.g. `sendContentUpdates()`). Never expose sync methods to Lua.

**Nil-mapping** — map Lua `nil` to Minecraft safe defaults (`ItemStack.EMPTY`, `null`, etc.). Never crash on `nil` where a default makes sense.

### 🚀 No architectural restrictions

Kotlin's job is to provide high-performance, predictable bindings — not to police what Lua scripts can build. Scripts may range from simple commands to heavy procedural worldgen. Prioritise single-thread performance and non-blocking patterns.

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
- 3 test files: `SyntaxParserTest`, `BuildVariantsTest`, `BrigadierTreeTest` — cover syntax parsing, variant generation, and tree topology (name-based dedup, sibling args, chaining).
- `BrigadierTreeTest` reflects `CommandNode.children` field directly — `getChildren()` returns a non-Map type on this classpath. Use `childrenField.get(node) as Map<*, *>`.
- To add tests: new file in `src/test/kotlin/ru/pyxiion/pxrp/`, use `kotlin.test.Test`, `kotlin.test.assertEquals`, etc.

## Project layout

```
src/main/java/ru/pyxiion/pxrp/
  PxRp.kt                  # lifecycle + event wiring
  LuaCmdLoader.kt          # Lua runtime, register() bridge, type map
  LuaCommandManager.kt     # dynamic Brigadier tree management
  CommandSyntax.kt         # SyntaxParser, buildVariants, parseSyntaxString, ArgDef, ArgToken — standalone, no Minecraft deps
  LuaEventManager.kt       # mc.on() event bus
  Scheduler.kt             # mc.schedule/mc.scheduleRepeating/mc.cancelTask
  Utils.kt                 # luaTableOf(), checkPermission(), asVarArgFunction()
  api/                     # Player (Lua-facing), Vector, LuaMcApi, ItemStackWrapper
  types/                   # LuaArgumentType, ChoiceArgumentType, toLuaValue()
  storage/                 # DataTable, DataBackend, JsonBackend, StorageManager
  mixins/
    CommandNodeMixin.java  # @Accessor on Brigadier CommandNode fields
    MinecraftServerMixin   # @Inject on reloadResources → luaLoader.reload()
  coerce/                  # DEAD CODE — entirely commented out
```

## Key conventions

- **Russian error messages** in logs and chat — do not flag as bugs
- **`Utils.kt` (root)** and **`types/Utils.kt`** are separate files with distinct helpers
- **`rawset` vs `set` on Player**: After `setmetatable()`, all `LuaTable.set()` goes through `__newindex`. Methods and data writes use `t.rawset(key, value)`. Property writes (`health`, `food`, `air`, `maxHealth`, `speed`, `armor`, `head`, `chest`, `legs`, `feet`, `mainhand`, `offhand`, etc.) route through `__newindex`. Any new key added after `setmetatable` MUST use `rawset`.
- **ItemStack wrapper**: `ItemStackWrapper` stores `ItemStack` Java objects in LuaTables with a `__pxrp_item` marker key and the stack at `_stack`. `unwrap()` always calls `copy()` to prevent mutation sharing. `mc.createItem` is registered in `LuaCmdLoader` on the `mc` table. `player:setItem/setItem/getItem` in `Player.kt` rely on `ItemStackWrapper` for conversion.
- **Player armour properties**: `player.head`, `player.chest`, `player.legs`, `player.feet` — read returns wrapped `ItemStack` or `nil`, write accepts `ItemStack` or `nil` (clears slot). Internally uses `EquipmentSlot.XXX.getOffsetEntitySlotId(PlayerInventory.MAIN_SIZE)`.
- **Player hand properties**: `player.mainhand` (active hotbar slot), `player.offhand` (slot 40) — same read/write pattern as armour properties.
- **Permission propagation**: Parent literal nodes require OR of their children's permissions. If any child has no permission (nil), parent is unrestricted.
- **Choice args**: `ChoiceArgumentType` implements `LuaArgumentType` (not Brigadier's `ArgumentType`). It builds with `StringArgumentType.word()` and validates choices at runtime in `getArg()` — not at parse time. `SuggestionProvider` provides tab-completions. Different choice sets share the same argument type (Brigadier can't disambiguate them at parse time).
- **Reserved commands**: `pxrp`, `stop`, `reload`, `op`, `deop`, `ban`, `ban-ip`, `pardon`, `pardon-ip`, `save-all`, `save-on`, `save-off`, `whitelist` — top-level literals blocked in `addCommand()`.
- **Fresh nodes per variant**: `ArgDef` stores only `luaType` + `isOptional`. `LuaCommandManager.addCommand` creates fresh `ArgumentCommandNode` instances on demand via `argDef.luaType.getBrigadierArgument(name)` rather than sharing global node instances across variants.
- **Granular path permissions**: Permissions are tracked per-node (including arguments) via `pathPermissions` keys like `"cmd <arg>"`. Literal ancestors accumulate the OR of all child permissions; argument nodes only carry their own variant's permission.
- **API/ doc sync**: Every time you change the API (add/modify/remove arg types, Lua functions, syntax, events), you **must** update both `AGENTS.md` (agent instructions) and `README.md` (user-facing docs).

## Register syntax

```lua
register("cmd <name:type> [<name:type>]", handler, permission?)
```

| Part | Meaning |
|------|---------|
| `cmd` `sub` | Literal path tokens — create Brigadier literal nodes |
| `<name:type>` | Required argument. Missing `:type` raises a parse error |
| `[<name:type>]` or `[name:type]` | Optional trailing argument. Everything from first `[...]` onward is optional. Missing → `nil` in handler. Internally registers N+1 variants |
| `<name:choice=x,y>` | Choice type — validates at runtime, tab-completes options |

**Types**: `text` (multi-word), `word` (single word, no quotes), `player` (or `target` alias), `int`, `double`, `float`, `bool`, `block_pos` (returns `{x,y,z}`), `choice=opt1,opt2,...`

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

`mc.setBlock` / `mc.getBlock` / `mc.fill` — coordinates floored, blockId auto-prefixed with `minecraft:` if no namespace. `setBlock` uses flag `0x03` (notify clients + update neighbors). `fill` uses `0x02` (notify neighbors only, no block updates) and volume capped at 32,768 blocks. `setBlockState` loads chunks on demand.

## Lua environment

- Runtime: `org.luaj:luaj-jse:3.0.1`
- Config dir: `config/pxrp/` (all `.lua` alphabetically). Falls back to single `config/pxrp.lua`. First run creates `demo.lua` from resource.
- `package.path`: `config/pxrp/?.lua;config/pxrp/?/init.lua;?.lua`
- Loaded std libs: `math`, `string`, `table`, `bit32`, `package`, base lib. **Not loaded**: `io`, `os`, `coroutine`, `debug`
- `require "format"` → `format(template)` / `broadcastFormat(template)`
- `require "simple"` → `registerSimple(syntax, template, range?, overlay?)`
