---
title: Container API
description: Lua wrapper for open container sessions — click callbacks, auto-locking, forced close.
---

A `Container` represents an open screen handler session. It is returned by
[`inv:open(player, title?)`](/docs/inventory-api/#invopenplayer-title).

---

## Properties

| Property | Type | Description |
|---|---|---|
| `container.player` | `table` | The player who opened the container |
| `container.inventory` | `table` | The backing inventory wrapper |

---

## Methods

### `container:close()`
Force-closes the container for the player.

```lua
container:close()
```

### `container:onClick(callback)` / `container:onClick(nil)`

Registers a click callback. When called with `nil`, removes the callback and **unlocks**
the inventory (free item movement).

```lua
-- Register callback (auto-locks inventory)
container:onClick(function(player, slot, clickType, slotItem, cursorItem)
  if slot == 0 then
    mc.broadcast(player.name .. " tried to take the special item!")
    return false  -- cancel this click
  end
end)

-- Remove callback and unlock
container:onClick(nil)
```

#### Click Callback Arguments

| Argument | Type | Description |
|---|---|---|
| `player` | `table` | Player who clicked |
| `slot` | `number` | Slot index (-1 = outside window) |
| `clickType` | `string` | Type of click (see below) |
| `slotItem` | `table` or `nil` | Item in the clicked slot |
| `cursorItem` | `table` or `nil` | Item on the cursor |

#### `clickType` Values

| Value | Description |
|---|---|
| `"pickup"` | Left click |
| `"quick_move"` | Shift + click |
| `"swap"` | Hotbar key swap |
| `"throw"` | Dropping item |
| `"quick_craft"` | Spread drag clicking |
| `"pickup_all"` | Double click |

---

## Auto-Locking

When a callback is registered via `container:onClick(fn)`, the backing inventory is
**automatically locked** using a `LockableInventory`. While locked:

- `removeStack()` returns `ItemStack.EMPTY` (prevents hoppers/take)
- `clear()` is a no-op
- `setStack()` remains allowed (Lua script modifications bypass via `unlocked {}`)

When `onClick(nil)` is called, the inventory unlocks, restoring normal item movement.

This prevents item theft when click callbacks are active but fails to process (e.g.,
during a reload or crash).
