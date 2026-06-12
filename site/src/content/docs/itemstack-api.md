---
title: ItemStack API
description: Lua wrapper for Minecraft item stacks — read properties, create items with components, safe reference handling.
---

The ItemStack wrapper provides read-only access to Minecraft item stacks. All wrappers
returned to Lua are **copies** — raw references never leak from the Lua boundary.

---

## Properties

| Property | Type | Description |
|---|---|---|
| `item.id` | `string` | Item identifier (e.g., `"minecraft:diamond"`) |
| `item.count` | `number` | Stack size |
| `item.custom_model_data` | `number` or `nil` | Custom model data integer |

All properties are **read-only**.

```lua
local item = player.mainhand
if item then
  mc.broadcast("Holding " .. item.id .. " x" .. item.count)
  if item.custom_model_data == 1001 then
    mc.broadcast("Special item detected!")
  end
end
```

---

## Creating Items

### `mc.createItem(id, count)`

```lua
local diamonds = mc.createItem("diamond", 16)
```

### `mc.createItem(id, components)`

Create items with full component data:

| Component | Type | Description |
|---|---|---|
| `count` | `number` | Stack size |
| `name` | `string` | Custom name (supports `&` color codes) |
| `lore` | `table` | Array of lore lines |
| `custom_model_data` | `number` | Custom model data |
| `unbreakable` | `boolean` | Unbreakable flag |
| `attackDamage` | `number` | Attack damage component |

```lua
local sword = mc.createItem("diamond_sword", {
  count = 1,
  name = "&bLegendary Blade",
  lore = {
    "&7A mighty weapon",
    "",
    "&6+10 Attack Damage"
  },
  custom_model_data = 500,
  unbreakable = true,
  attackDamage = 10
})
```

---

## Safety

`ItemStackWrapper.unwrap()` always calls `.copy()` before returning the underlying
`ItemStack`. This ensures:

- Lua scripts can **never** mutate the server's internal item stacks
- Modifications to an item in Lua must go through API methods (`player:setItem()`,
  `inv:setItem()`, etc.)
- Multiple reads of the same slot return independent copies

```lua
-- Safe — no risk of leaking raw references
local item = player.mainhand
item.count = 99  -- this does nothing, properties are read-only
player:setItem(player.selectedSlot, mc.createItem("diamond", 99))  -- correct
```
