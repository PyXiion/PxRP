---
title: Structure API
description: Load and place structure templates with mc.loadStructure and mc.loadStructureFile.
---

Load structure templates (`.nbt` files from the `structures/` folder under the server's root or from absolute paths) and place them in the world with full rotation, mirror, and entity callback support.

## Loading Structures

```lua
local struct = mc.loadStructure("my_house")
local struct = mc.loadStructureFile("config/pxrp/my_build.nbt")
```

- `mc.loadStructure(id)` — loads from the server's `structures/<id>.nbt` folder (same as structure blocks).
- `mc.loadStructureFile(path)` — loads from an absolute or server-relative file path.
- Returns a structure wrapper or `nil` if not found.

## Properties

| Property | Type | Description |
|----------|------|-------------|
| `struct.size` | table | `{x, y, z}` dimensions of the structure |

## Methods

### `place(world, pos, params?)`

Places the structure in the world.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `world` | world wrapper | — | Target world |
| `pos` | table | — | `{x, y, z}` block position |
| `params` | table or nil | `nil` | Optional parameters table |

**Parameters table (`params`):**

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `rotation` | string | `"none"` | `"none"` / `"0"`, `"clockwise_90"` / `"90"`, `"clockwise_180"` / `"180"`, `"counterclockwise_90"` / `"270"` |
| `mirror` | string | `"none"` | `"none"`, `"left_right"`, `"front_back"` |
| `on_entity` | function | `nil` | Callback fired for each entity; receives entity wrapper, return `false` to skip spawning |

```lua
struct:place(world, Vec(100, 64, 200), {
    rotation = "clockwise_90",
    mirror = "left_right",
    on_entity = function(entity)
        if entity:isMob() then
            return false  -- don't spawn mobs
        end
    end
})
```

Entity UUIDs are regenerated automatically when placing a structure — no duplicates.
