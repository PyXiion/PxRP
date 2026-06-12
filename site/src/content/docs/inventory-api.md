---
title: Inventory API
description: Lua wrapper for modifiable inventories — create, modify slots, open for players.
---

The inventory API lets you create and manage virtual inventories. Inventories can be
opened for players, returning a [Container](/docs/container-api) session.

---

## Creating Inventories

### `mc.createInventory(size)`
Creates a new inventory with the given number of slots. Size must be a multiple of 9
(9, 18, 27, 36, 45, 54).

```lua
local inv = mc.createInventory(27)  -- 3 rows
```

---

## Properties

| Property | Type | Description |
|---|---|---|
| `inv.size` | `number` | Number of slots |

---

## Methods

### `inv:getItem(slot)`
Returns the item in the given slot (0-indexed), or `nil` if empty.

```lua
local item = inv:getItem(0)
if item then
  mc.broadcast("Slot 0 contains " .. item.id)
end
```

### `inv:setItem(slot, item)`
Sets a slot to the given item stack. `item` can be `nil` to clear the slot.

```lua
inv:setItem(0, mc.createItem("diamond", 1))
inv:setItem(1, nil)  -- clear slot 1
```

### `inv:fill(item)`
Fills every empty slot with the given item.

```lua
inv:fill(mc.createItem("stone", 1))
```

### `inv:clear()`
Clears all slots from the inventory.

```lua
inv:clear()
```

---

## Opening for a Player

### `inv:open(player, title?)`
Opens the inventory for a player as a screen handler. Returns a [Container](/docs/container-api)
object.

```lua
local container = inv:open(player, "&6My Chest")
container:onClick(function(slot, clickType, p, slotItem, cursorItem)
  if slot == 0 then
    return false  -- prevent taking the first slot
  end
end)
```

The returned `Container` tracks the open session and allows click handling, forced close,
and access to the backing inventory and player.
