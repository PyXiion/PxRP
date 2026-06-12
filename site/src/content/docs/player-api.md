---
title: Player API
description: Lua wrapper for Minecraft players — properties, inventory, effects, sidebar, and more.
---

The player wrapper provides access to a connected player. Players are obtained via
`mc.players()`, `world.players`, or as event handler arguments.

Player wrapper extends entity — all [Entity API](/docs/entity-api) properties and methods
are available on players.

---

## Properties

### Identity

| Property | Type | Description |
|---|---|---|
| `player.name` | `string` | Player's username |
| `player.uuid` | `string` | UUID as string |
| `player.displayName` | `string` | Display name |
| `player.customName` | `string` | Custom name |
| `player.isOp` | `boolean` | Operator status |
| `player.ping` | `number` | Latency in ms |

### Position & Movement

| Property | Type | Description |
|---|---|---|
| `player.world` | `table` | World wrapper |
| `player.pos` | `table` | Position vector (`{x, y, z}`) |
| `player.dir` | `table` | Look direction vector |
| `player.bodyDir` | `table` | Body direction vector |
| `player.isSneaking` | `boolean` | Sneaking state |
| `player.isSprinting` | `boolean` | Sprinting state |
| `player.isFlying` | `boolean` | Flying state |
| `player.fallDistance` | `number` | Current fall distance |
| `player.selectedSlot` | `number` | Hotbar slot (0–8) |

```lua
local pos = player.pos
mc.broadcast(player.name .. " is at " .. pos.x .. ", " .. pos.y .. ", " .. pos.z)
```

### Health & Stats

| Property | Type | Description |
|---|---|---|
| `player.health` | `number` | Current health |
| `player.maxHealth` | `number` | Maximum health |
| `player.food` | `number` | Food level |
| `player.saturation` | `number` | Saturation |
| `player.xpLevel` | `number` | Experience level |
| `player.xpProgress` | `number` | Progress to next level (0–1) |
| `player.air` | `number` | Remaining air |
| `player.maxAir` | `number` | Maximum air |

### Equipment

| Property | Type | Description |
|---|---|---|
| `player.mainhand` | `table` | Main hand item or `nil` |
| `player.offhand` | `table` | Off hand item or `nil` |
| `player.head` | `table` | Helmet slot or `nil` |
| `player.chest` | `table` | Chestplate slot or `nil` |
| `player.legs` | `table` | Leggings slot or `nil` |
| `player.feet` | `table` | Boots slot or `nil` |

### Attributes

| Property | Type | Description |
|---|---|---|
| `player.armor` | `number` | Armor value |
| `player.armorToughness` | `number` | Armor toughness |
| `player.attackDamage` | `number` | Attack damage |
| `player.attackSpeed` | `number` | Attack speed |
| `player.blockBreakSpeed` | `number` | Block break speed |
| `player.flyingSpeed` | `number` | Flying speed |
| `player.gravity` | `number` | Gravity multiplier |
| `player.knockbackResistance` | `number` | Knockback resistance |
| `player.luck` | `number` | Luck attribute |
| `player.safeFallDistance` | `number` | Safe fall distance |
| `player.scale` | `number` | Entity scale |
| `player.speed` | `number` | Movement speed |
| `player.stepHeight` | `number` | Step height |

### Tags

| Property | Type | Description |
|---|---|---|
| `player.tags` | `table` | Scoreboard tags proxy — read/write |

```lua
for _, tag in ipairs(player.tags) do
  print(tag)
end
```

### Other

| Property | Type | Description |
|---|---|---|
| `player.gamemode` | `string` | `"survival"`, `"creative"`, `"adventure"`, `"spectator"` |
| `player.removed` | `boolean` | Whether the entity has been removed |

---

## Methods

### Messaging

```lua
player:sendMessage("Hello!")
player:sendActionBar("&eHotbar message")
player:sendTitle("&cWarning", "&7You are in a dangerous area", 10, 70, 20)
```

`sendTitle(title, subtitle?, fadeIn?, stay?, fadeOut?)` — all duration args in ticks.

### Teleport & Damage

```lua
player:teleport(mc.world("minecraft:overworld"), { x = 0, y = 64, z = 0 })
player:damage(10)              -- fall damage source
player:damage(5, attacker)     -- with attacking entity
player:heal(20)
```

### Kick

```lua
player:kick("You have been kicked!")
```

### Permission

```lua
if player:hasPermission("myplugin.admin") then
  -- grant access
end
```

### Sound

```lua
player:playSound("minecraft:entity.ender_dragon.growl", 1.0, 1.0)
```

### Inventory

```lua
player:give(mc.createItem("diamond", 1))
player:setItem(slot, itemStack)
local item = player:getItem(slot)
player:clear()
```

### Commands

```lua
player:executeCommand("say Hello!")
```

### Effects

```lua
-- Entity API methods (inherited):
player:addEffect(1, 600, 2, true, true)  -- speed III, 30s, particles + icon
player:removeEffect(1)
if player:hasEffect(1) then ...
```

---

## Sidebar

Each player has a per-player sidebar that uses a local `Scoreboard` instance — it does
**not** touch the server's global scoreboard, so other players never see it.

### `player.sidebar = {...}`

Creating or updating the sidebar via assignment:

```lua
player.sidebar = {
  title = "&6My Server",
  lines = { "&aWelcome!", "", "&7Players: " .. mc.onlineCount }
}
```

Partial updates merge with existing state:

```lua
player.sidebar = { title = "&6Updated Title" }  -- title only
player.sidebar = { lines = { "Line 1", "Line 2" } }  -- lines only
player.sidebar = { visible = false }  -- hide
player.sidebar = nil  -- destroy
```

### Sidebar Object

Reading `player.sidebar` returns the sidebar object:

| Property | Type | Description |
|---|---|---|
| `sb.title` | `string` | Current title (write to update) |
| `sb.lines` | `table` | Lines array (write to replace all) |
| `sb.visible` | `boolean` | Whether sidebar is shown |
| `sb.lineCount` | `number` | Number of lines |

```lua
local sb = player.sidebar
sb.title = "&6New Title"
sb.lines = { "a", "b", "c" }
```

### Sidebar Methods

```lua
sb:setLine(1, "&cRed line")  -- update a single line
sb:show()                     -- show sidebar
sb:hide()                     -- hide sidebar
sb:destroy()                  -- destroy the sidebar completely
```

---

## Inheritance

Player inherits all [Entity API](/docs/entity-api) properties and methods. Any property
not listed above falls through to the entity metatable (`entity.health`, `entity:raycast()`,
etc.).
