---
title: Storage
description: Persistent JSON data with mc.data, ctx.player.data, and atomic file writes.
---

PxIgnis provides persistent JSON key-value storage for global and per-player data. Data is saved automatically on server stop, player disconnect, and `/pxrp reload`.

## Storage Locations

| Scope | File |
|-------|------|
| Global | `config/pxrp/storage/global.json` |
| Per-player | `config/pxrp/storage/players/<uuid>.json` |

## API

```lua
-- Global data (available everywhere)
mc.data.key = "value"
local v = mc.data.key

-- Per-player data (in command/event handlers)
ctx.player.data.coins = 100
local coins = ctx.player.data.coins
```

Supports numbers, strings, booleans, tables, and `nil` (deletes the key).

## Nested Tables

Nested table assignments require **re-assignment of the parent**. Direct nested writes are not persisted:

```lua
-- ❌ Won't work:
data.stats.kills = 5

-- ✅ Correct:
local stats = data.stats or {}
stats.kills = 5
data.stats = stats
```

This limitation exists because the storage layer cannot observe modifications inside nested subtables.

## Data Validation

The following types are **not** allowed in storage and will raise an error:
- Functions
- Userdata
- Threads
- Tables with cyclic references

## Persistence

Data is written to disk on:
- **Server stop**
- **Player disconnect** (per-player data only)
- **`/pxrp reload`**

Per-player data is removed from the in-memory storage map on disconnect (but remains on disk).

## Atomic Writes

All file writes use an **atomic write pattern**: data is written to a temporary file first, then atomically moved to the target path. This prevents data corruption if the server crashes mid-write.
