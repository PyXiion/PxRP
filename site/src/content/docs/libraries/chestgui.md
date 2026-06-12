---
title: chestgui
description: Create interactive chest GUIs with Lua callbacks.
---

The `chestgui` library lets you create chest-based GUIs with click callbacks.

## Loading

```lua
local chestgui = require "chestgui"
```

## chestgui.create(rows, title)

Creates a new GUI. `rows` is 1–6 (each row is 9 slots). Returns a GUI table.

```lua
local gui = chestgui.create(3, "Shop")
```

## Methods

### gui:set(row, col, item, callback)

Set an item at a grid position (1-based) with a click callback.

```lua
gui:set(1, 5, mc.createItem("diamond"), function(player, container)
    player:sendMessage("You clicked the diamond!")
end)
```

### gui:decorate(row, col, item)

Set a decorative item with no callback (cannot be picked up).

```lua
gui:decorate(3, 5, mc.createItem("black_stained_glass_pane"))
```

### gui:button(slot, item, callback)

Set an item at a raw slot index (0-based, matches Minecraft slot numbering).

```lua
gui:button(13, mc.createItem("emerald"), function(player, container)
    player:sendMessage("Button clicked!")
end)
```

### gui:fill(item)

Fill all empty slots with an item.

```lua
gui:fill(mc.createItem("gray_stained_glass_pane"))
```

### gui:clear()

Remove all items from the GUI.

### gui:open(player)

Opens the GUI for a player, returning a [Container](/container-api) object.

```lua
local container = gui:open(player)
```

### gui:close(player)

Closes the GUI for a player.

## Full Example

```lua
local chestgui = require "chestgui"

local function openShop(player)
    local gui = chestgui.create(3, "Item Shop")

    gui:decorate(1, 1, mc.createItem("black_stained_glass_pane"))
    gui:decorate(1, 9, mc.createItem("black_stained_glass_pane"))
    gui:decorate(3, 1, mc.createItem("black_stained_glass_pane"))
    gui:decorate(3, 9, mc.createItem("black_stained_glass_pane"))

    gui:set(2, 3, mc.createItem("diamond", 1), function(p, container)
        p:sendMessage("You bought a diamond!")
        container:close()
    end)

    gui:set(2, 5, mc.createItem("emerald", 1), function(p, container)
        p:sendMessage("You bought an emerald!")
        container:close()
    end)

    gui:set(2, 7, mc.createItem("netherite_ingot", 1), function(p, container)
        p:sendMessage("You bought netherite!")
        container:close()
    end)

    gui:fill(mc.createItem("gray_stained_glass_pane"))
    gui:open(player)
end

register("shop", function(ctx)
    openShop(ctx.player)
end)
```
