---
title: Entity API
description: Lua wrapper for Minecraft entities — properties, attributes, equipment, effects, and raycasting.
---

The entity wrapper provides access to any Minecraft entity. Player entities extend this
wrapper — see [Player API](/docs/player-api).

---

## Properties

### Identity

| Property | Type | Description |
|---|---|---|
| `entity.uuid` | `string` | UUID as string |
| `entity.type` | `string` | Entity type (e.g., `"minecraft:zombie"`) |
| `entity.name` | `string` | Entity name |
| `entity.displayName` | `string` | Display name component |
| `entity.customName` | `string` | Custom name |

### Position & Movement

| Property | Type | Description |
|---|---|---|
| `entity.world` | `table` | World wrapper |
| `entity.pos` | `table` | Position vector (`{x, y, z}`) |
| `entity.dir` | `table` | Look direction vector |
| `entity.bodyDir` | `table` | Body direction vector |
| `entity.isSneaking` | `boolean` | Sneaking state |
| `entity.isSprinting` | `boolean` | Sprinting state |
| `entity.fallDistance` | `number` | Current fall distance |
| `entity.removed` | `boolean` | Whether the entity has been removed |

### Health & Stats

| Property | Type | Description |
|---|---|---|
| `entity.health` | `number` | Current health |
| `entity.maxHealth` | `number` | Maximum health |
| `entity.air` | `number` | Remaining air |
| `entity.maxAir` | `number` | Maximum air |
| `entity.fireTicks` | `number` | Remaining fire ticks (-1 = not on fire) |

### Status

| Property | Type | Description |
|---|---|---|
| `entity.glowing` | `boolean` | Glowing effect state |
| `entity.invulnerable` | `boolean` | Invulnerability state |

```lua
mc.broadcast("Found a " .. entity.type .. " at " .. entity.pos.x .. ", " .. entity.pos.y .. ", " .. entity.pos.z)
```

---

## Equipment

| Property | Type | Description |
|---|---|---|
| `entity.mainhand` | `table` or `nil` | Main hand item |
| `entity.offhand` | `table` or `nil` | Off hand item |
| `entity.head` | `table` or `nil` | Helmet slot |
| `entity.chest` | `table` or `nil` | Chestplate slot |
| `entity.legs` | `table` or `nil` | Leggings slot |
| `entity.feet` | `table` or `nil` | Boots slot |

Each equipment property returns an [ItemStack wrapper](/docs/itemstack-api) or `nil`.

---

## Attributes

| Property | Type | Description |
|---|---|---|
| `entity.speed` | `number` | Movement speed |
| `entity.armor` | `number` | Armor value |
| `entity.armorToughness` | `number` | Armor toughness |
| `entity.attackDamage` | `number` | Attack damage |
| `entity.attackSpeed` | `number` | Attack speed |
| `entity.knockbackResistance` | `number` | Knockback resistance |
| `entity.luck` | `number` | Luck attribute |
| `entity.stepHeight` | `number` | Step height |
| `entity.blockBreakSpeed` | `number` | Block break speed |
| `entity.gravity` | `number` | Gravity multiplier |
| `entity.scale` | `number` | Entity scale |
| `entity.safeFallDistance` | `number` | Safe fall distance |
| `entity.flyingSpeed` | `number` | Flying speed |

---

## Tags

```lua
for _, tag in ipairs(entity.tags) do
  print(tag)
end
```

The `tags` property returns a proxy table for the entity's scoreboard tags.

---

## Methods

### Damage

```lua
entity:damage(amount)               -- generic damage
entity:damage(amount, sourceEntity) -- damage attributed to source
```

### Raycasting

```lua
local hit = entity:raycast(range, includeFluids?)
if hit then
  -- hit.pos, hit.entity, hit.block
end
```

### Effects

```lua
entity:addEffect(effectId, duration, amplifier?, particles?, icon?)
entity:removeEffect(effectId)
if entity:hasEffect(effectId) then
  -- ...
end
```

`effectId` is the potion effect type ID (e.g., `1` for Speed, `5` for Strength).
`duration` is in ticks (20 ticks = 1 second). `amplifier` starts at 0.

```lua
-- Speed II, 30 seconds, with particles and icon
entity:addEffect(1, 600, 1, true, true)
```

### Fire

```lua
entity:setOnFireFor(ticks)  -- e.g., setOnFireFor(100) = 5 seconds
```

### NBT

```lua
local nbt = entity:readNbt()   -- returns a Lua table of the entity's NBT
entity:writeNbt({ CustomName = '{"text":"Bob"}', Health = 40.0 })
```
