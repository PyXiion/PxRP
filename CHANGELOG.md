# Changelog

## 0.5.1 — Rebuild

No API changes. CI fix for Modrinth publish.

---

## 0.5.0 — Entity/Structure/Sidebar/Metatable APIs, 10 new events

### New `mc.*` functions

| Function | Signature |
|----------|-----------|
| `mc.getMetatable(name)` | `"entity"\|"player"\|"world"\|"structure"` → shared metatable |
| `mc.loadStructure(id)` | string → structure table |
| `mc.loadStructureFile(path)` | file path → structure table |
| `mc.getEntity(uuid)` | string → entity table or nil |
| `mc.dump(value, maxDepth?)` | recursive table debug dump |
| `mc.emit(event, ...)` | programmatic event firing |
| `mc.players` | property — list of online player tables (cached) |
| `mc.onlineCount` | property — number of online players |

### New entity methods

| Method | Signature | Notes |
|--------|-----------|-------|
| `entity:readNbt()` | → table | Dump entity NBT to Lua table |
| `entity:writeNbt(t)` | ← table | Write NBT back to entity |
| `entity:raycast(range, includeFluids?)` | → entity or `{x,y,z}` or nil | Hits entities before blocks |
| `entity:damage(amount, sourceEntity?)` | — | With optional damage source |
| `entity:addEffect(id, duration, amplifier?, particles?, icon?)` | → bool | Status effect |
| `entity:removeEffect(id)` | → bool | |
| `entity:hasEffect(id)` | → bool | |
| `entity:setOnFireFor(ticks)` | — | |
| `entity.removed` | r/o bool | Whether entity is removed |
| `entity.pos.x/y/z` | r/w now live | Previously snapshot; now reads/writes current pos |

### New player methods

| Method | Signature |
|--------|-----------|
| `player:sendActionBar(text)` | |
| `player:sendTitle(title, subtitle?, fadeIn?, stay?, fadeOut?)` | |
| `player:playSound(id, volume?, pitch?)` | |
| `player:getItem(slot)` → item table or nil | |
| `player:setItem(slot, item)` | item from `mc.createItem` or nil |
| `player:clear()` | Clear inventory |
| `player.sidebar` | r/w property — set to `{title="...", lines={...}}` or `nil` to clear |
| `player.sidebar.title` | r/w string |
| `player.sidebar[i]` | r/w lines (1-indexed) |

### New world methods

| Method | Signature | Notes |
|--------|-----------|-------|
| `world:particle(id, pos, opts)` | **Moved from `mc.particle`** | Particle visible to all in that world |
| `world:broadcastInRange(text, pos, range, overlay?)` | **Moved from `mc.broadcastInRange`** | |
| `world:getEntities(pos, radius, typeFilter?)` | → table of entity tables | Spatial entity query |

### Removed from `mc.*`

- `mc.particle(...)` → use `world:particle(...)`
- `mc.broadcastInRange(...)` → use `world:broadcastInRange(...)`

### New events (10 added, 16 total)

Cancellable:
- `player_block_break(player, pos, blockId)`
- `player_block_place(player, pos, blockId)`
- `player_use_item(player, hand, item, itemId)`
- `player_attack_entity(player, entity)`
- `player_interact_entity(player, entity)`
- `player_hurt(player, source, amount)`
- `entity_hurt(entity, source, amount)`

Non-cancellable:
- `player_damage(player, source, damageTaken, blocked)`
- `entity_damage(entity, source, damageTaken, blocked)`
- `player_kill(player, killedEntity, source)`

### New types/values

- **Structure table** — `.size` (vector), `:place(world, pos, opts?)` (rotation, mirror, on_entity)
- **Shared metatables** — `mc.getMetatable("entity"|"player"|"world"|"structure")`
- **`item.custom_model_data`** — now readable on item tables
- **`mc.createItem(id, {attackDamage = N})`** — new option to set attack damage

### Other

- `world:spawn(id, pos, overrides?)` now returns entity with all new methods (raycast, damage, effects, NBT)
- `entity.dir`, `entity.bodyDir` now return `{x,y,z}` vector tables (previously Java userdata)
- Player cache: `mc.players()` and `world.players` reuse wrappers across lookups
- Sidebar persists across world changes and reconnects (restored 2 ticks after join)
- `player.block_break`/`player.block_place` migrated from mixins to Fabric API events

---

## 0.4.0 — World/Entity API, tags, item creation

### New `mc.*` functions

| Function | Signature |
|----------|-----------|
| `mc.world(name)` | string → world table |
| `mc.createItem(id, countOrTable?)` | → item table |
| `mc.setBlock(x,y,z,block,world)` | **Removed in 0.5.0** — use `world:setBlock` |
| `mc.getBlock(x,y,z,world)` | **Removed in 0.5.0** — use `world:getBlock` |
| `mc.fill(x1,y1,x2,y2,z1,z2,block,world)` | **Removed in 0.5.0** — use `world:fill` |

### New world methods

| Method | Notes |
|--------|-------|
| `world:spawn(id, pos, overrides?)` | → entity table |
| `world:setBlock(pos, blockId)` | |
| `world:getBlock(pos)` | → block id string |
| `world:fill(pos1, pos2, blockId)` | |
| `world.name`, `world.time`, `world.raining`, `world.thundering` | r/w properties |

### New entity properties

Entity table returned by `world:spawn()` and `player` delegation:

| Property | Type | Notes |
|----------|------|-------|
| `uuid` | string | |
| `type` | string | e.g. `"minecraft:zombie"` |
| `name`, `displayName`, `customName` | string | |
| `world` | table | World wrapper (was string in 0.3.0) |
| `pos` | `{x,y,z}` vector | r/w (snapshot in 0.4.0; live proxy in 0.5.0) |
| `dir`, `bodyDir` | `{x,y,z}` vector | |
| `health`, `maxHealth` | number | |
| `fallDistance`, `fireTicks` | number | |
| `glowing`, `invulnerable` | boolean | |
| `isSneaking`, `isSprinting` | boolean | |
| `air`, `maxAir` | number | |
| `tags` | boolean proxy | `pairs(entity.tags)`, `entity.tags["tag"] = true/false` |
| `speed`, `armor`, `attackDamage`, `maxHealth_attr`, `followRange`, `knockbackResistance`, `luck`, `horseJump`, `flyingSpeed`, `armorToughness`, `movementEfficiency`, `scale`, `stepHeight` | number | Attribute accessors (r/w where vanilla permits) |
| `mainhand`, `offhand`, `head`, `chest`, `legs`, `feet` | item table or nil | Equipment slots (writable) |

### Player changes

- `player.world` now returns a world table (was a world name string)
- `player:give(id, count)` also accepts item table from `mc.createItem`
- All entity properties delegated to entity metatable (player inherits pos, health, tags, equipment, etc.)

### New command argument types

- `word` — single-word string (`StringArgumentType.word()`)

### Other

- Block IDs auto-prefixed with `minecraft:` if no namespace given
- `demo.lua` no longer requires `return {...}` at file end
- Shadow relocates `org.luaj` → `ru.pyxiion.lib.luaj` and Permissions API

---

## 0.3.0 — Scheduler, teleport fix, Lua arg fallback

### New `mc.*` functions

| Function | Signature |
|----------|-----------|
| `mc.schedule(delayTicks, callback)` | → task id |
| `mc.scheduleRepeating(delayTicks, intervalTicks, callback)` | → task id |
| `mc.cancelTask(id)` | → bool |
| `mc.data`, `player.data` | Persistent key-value storage |

### Events

- `server_start`, `server_stop`
- `player_join(player)` — cancellable
- `player_leave(player)`, `player_death(player, source, message)`
- `player_chat(player, message)` — cancellable

### Player (initial API)

- `player:send(msg)`, `player:teleport(x,y,z)`, `player:kick(reason?)`
- `player:give(id, count)`, `player:damage(amount)`
- `player:getInventory()` → table of item stacks
- `player:getBlockPos()` → `{x,y,z}`
- `player.name`, `player.uuid`, `player.world`, `player.pos` (read-only snapshot)
- `player.health` (r/w), `player.displayName` (r/w)

### Commands

- `register(syntax, handler, permission?)` — literal/arg syntax with `<name:type>` and `[optional]`
- Types: `text`, `player` (alias `target`), `int`, `double`, `float`, `bool`, `block_pos`, `choice=a,b,c`
- Reserved commands: `stop`, `reload`, `op`, `deop`, `ban`, `ban-ip`, `pardon`, `pardon-ip`, `save-all`, `save-on`, `save-off`, `whitelist`, `pxrp`

### Storage

- JSON storage: `mc.data` → global, `player.data` → per-player
- Saved on server stop, player disconnect, `/pxrp reload`
- Nested tables require re-assignment (`data.nested = t`)

### Other

- `require "format"` — `format(template)`, `broadcastFormat(template)`
- `require "simple"` — `registerSimple(syntax, template, range?, overlay?)`
- `Vec(x, y, z)` constructor with arithmetic metamethods
