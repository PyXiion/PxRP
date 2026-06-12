---
title: World API
description: Lua wrapper for Minecraft worlds — blocks, entities, particles, weather, and raycasting.
---

The world wrapper provides access to a Minecraft `ServerWorld`. Obtain worlds via
`mc.world(name)` or from entity/player `.world` properties.

---

## Properties

| Property | Type | Description |
|---|---|---|
| `world.name` | `string` | World identifier (e.g., `"minecraft:overworld"`) |
| `world.time` | `number` | World time in ticks (read/write) |
| `world.raining` | `boolean` | Whether it is raining (read/write) |
| `world.thundering` | `boolean` | Whether it is thundering (read/write) |
| `world.players` | `table` | Array of player wrappers in this world |

```lua
local world = mc.world("minecraft:overworld")
mc.broadcast("Time: " .. world.time)
world.time = 1000  -- set to morning
```

---

## Block Methods

### `world:setBlock(pos, blockId)`
Sets a block at the given position. Accepts `blockId` with or without `minecraft:` prefix.

```lua
world:setBlock({ x = 0, y = 64, z = 0 }, "diamond_block")
world:setBlock({ x = 0, y = 65, z = 0 }, "minecraft:torch")
```

### `world:getBlock(pos)`
Returns the block ID at the given position as a string.

```lua
local block = world:getBlock({ x = 0, y = 64, z = 0 })
mc.broadcast("Block: " .. block)
```

### `world:fill(pos1, pos2, blockId)`
Fills a cuboid region between two positions with a block.

```lua
world:fill({ x = -10, y = 60, z = -10 }, { x = 10, y = 70, z = 10 }, "stone")
```

---

## Entity Methods

### `world:spawn(entityId, pos, overrides?)`
Spawns an entity at the given position. Optional `overrides` table sets NBT data.

```lua
-- Simple spawn
local pig = world:spawn("pig", { x = 10, y = 64, z = 10 })

-- With overrides
local zombie = world:spawn("zombie", { x = 0, y = 64, z = 0 }, {
  CustomName = "{\"text\":\"Bob\"}",
  Health = 40.0
})
```

Returns the entity wrapper of the spawned entity.

### `world:getEntities(pos, radius, typeFilter?)`
Gets entities within a radius of a position. Optional `typeFilter` limits by entity type.

```lua
-- All entities within 10 blocks
local entities = world:getEntities({ x = 0, y = 64, z = 0 }, 10)

-- Only pigs
local pigs = world:getEntities({ x = 0, y = 64, z = 0 }, 10, "minecraft:pig")
```

---

## Effects

### `world:particle(id, pos, opts?)`
Spawns particles at a position. Optional `opts` table supports:

| Option | Type | Default | Description |
|---|---|---|---|
| `count` | `number` | `1` | Number of particles |
| `delta` | `table` | `{0, 0, 0}` | Spread vector |
| `speed` | `number` | `0` | Particle speed |

```lua
-- Simple particle
world:particle("minecraft:flame", { x = 0, y = 65, z = 0 })

-- With options
world:particle("minecraft:heart", { x = 0, y = 65, z = 0 }, {
  count = 5,
  delta = { 0.5, 0.5, 0.5 },
  speed = 0.1
})
```

### `world:playSound(id, x, y, z, volume?, pitch?)`
Plays a sound at a position for all players in the world.

```lua
world:playSound("minecraft:entity.experience_orb.pickup", 0, 64, 0)
world:playSound("minecraft:entity.ender_dragon.growl", 0, 64, 0, 2.0, 0.5)
```

---

## Broadcasting

### `world:broadcastInRange(text, x, y, z, range, overlay?)`
Broadcasts a chat message (or overlay if `overlay` is `true`) only to players within
`range` blocks of the given position.

```lua
world:broadcastInRange("&cDanger nearby!", 0, 64, 0, 20, false)
```

---

## Raycasting

### `world:raycast(startVec, dirVec, range, includeFluids?, includeEntities?)`
Performs a raycast from `startVec` in the direction of `dirVec`. Returns a hit result
or `nil`.

```lua
local start = { x = 0, y = 64, z = 0 }
local dir = { x = 0, y = -1, z = 0 }
local hit = world:raycast(start, dir, 10, false, false)

if hit then
  mc.broadcast("Hit at " .. hit.pos.x .. ", " .. hit.pos.y .. ", " .. hit.pos.z)
  -- hit.pos: intersection point
  -- hit.entity: hit entity (if includeEntities and hit an entity)
  -- hit.block: block position table {x, y, z}
end
```
