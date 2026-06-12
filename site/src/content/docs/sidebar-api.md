---
title: Sidebar API
description: Per-player scoreboard sidebars with packet-based rendering.
---

Access and control a per-player sidebar through the `player.sidebar` smart property. Sidebars use a **local `Scoreboard()` instance** and direct packets — the server's global scoreboard is never touched, so other players never see it.

## Creating a Sidebar

Assign a config table to `player.sidebar`. This creates (or updates) the sidebar and shows it immediately.

```lua
player.sidebar = {
    title = "My Server",
    lines = {"Welcome!", "", "Online: 42"}
}
```

## Partial Updates

You can update individual properties without rebuilding the whole sidebar:

```lua
player.sidebar = { title = "New Title" }      -- update title only
player.sidebar = { lines = {"Line 1", "Line 2"} }  -- replace all lines
player.sidebar = { visible = false }          -- hide the sidebar
```

## Destroying

```lua
player.sidebar = nil  -- destroys the sidebar entirely
```

## Sidebar Object

When you read `player.sidebar`, you get a sidebar object with the following interface:

### Properties

| Property | Type | Access | Description |
|----------|------|--------|-------------|
| `sb.title` | string | r/w | Current title (sends update packet if visible) |
| `sb.lines` | table | r/w | Current lines (array of strings). Replaces all lines on write |
| `sb.visible` | boolean | r/o | Whether the sidebar is currently displayed |
| `sb.lineCount` | number | r/o | Number of lines |

### Methods

```lua
sb:setLine(n, text)  -- set or update a specific line (1-indexed)
sb:show()            -- show the sidebar
sb:hide()            -- hide the sidebar
sb:destroy()         -- destroy the sidebar permanently
```

### Examples

```lua
-- Get the sidebar object
local sb = player.sidebar

-- Modify properties directly
sb.title = "Updated Title"
sb.lines = {"Line A", "Line B", "Line C"}
sb:setLine(2, "Modified Line B")

-- Toggle visibility
sb:hide()
-- ... later ...
sb:show()

-- Destroy
sb:destroy()
```

## Lifetime

Sidebars are automatically destroyed when the player **disconnects** and on **`/pxrp reload`**. You do not need to clean them up manually.
