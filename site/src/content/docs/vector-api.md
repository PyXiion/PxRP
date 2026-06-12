---
title: Vector API
description: 3D vector arithmetic with Vec(x, y, z).
---

`Vec` is a global constructor for 3D vectors. It creates a table with `{x, y, z}` and the vector metatable (`mc.getMetatable("vec")`).

```lua
local v = Vec(1, 2, 3)
```

## Properties

| Property | Type | Description |
|----------|------|-------------|
| `v.x` | number | X component |
| `v.y` | number | Y component |
| `v.z` | number | Z component |

## Operators

All operators return a new vector; the originals are unchanged.

| Operator | Example | Behaviour |
|----------|---------|-----------|
| `+` | `v1 + v2` | Component-wise addition |
| `-` | `v1 - v2` | Component-wise subtraction |
| `*` | `v1 * v2` | Component-wise multiplication |
| `*` | `v * n` or `n * v` | Scalar multiplication |
| `/` | `v / n` | Scalar division (vector must be first) |
| `unm` | `-v` | Negation |
| `==` | `v1 == v2` | Equality (all components equal) |
| `tostring` | `tostring(v)` | Returns `"(x, y, z)"` |

### Examples

```lua
local a = Vec(1, 2, 3)
local b = Vec(4, 5, 6)

local sum     = a + b         -- Vec(5, 7, 9)
local diff    = a - b         -- Vec(-3, -3, -3)
local compMul = a * b         -- Vec(4, 10, 18)
local scalar  = a * 10        -- Vec(10, 20, 30)
local also    = 10 * a        -- Vec(10, 20, 30)
local div     = a / 2         -- Vec(0.5, 1, 1.5)
local neg     = -a            -- Vec(-1, -2, -3)
local eq      = a == Vec(1, 2, 3)  -- true
print(tostring(a))            -- "(1, 2, 3)"
```

## Metatable

Access the vector metatable for direct manipulation:

```lua
local vecMeta = mc.getMetatable("vec")
```
