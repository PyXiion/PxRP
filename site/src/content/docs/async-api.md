---
title: Async API
description: Coroutine-based HTTP requests and tick delays with mc.fetch and mc.sleep.
---

Coroutine-based async operations — sequential code without callback nesting. Under the hood, `mc.fetch` and `mc.sleep` yield the Lua coroutine and resume on the server thread when the operation completes.

## mc.sleep(ticks)

Yields the current coroutine and resumes after the specified number of ticks (20 ticks = 1 second).

```lua
mc.sendMessage("Waiting 2 seconds...")
mc.sleep(40)
mc.sendMessage("Done!")
```

## mc.fetch(url)

Simple GET request. Returns a response table.

```lua
local res = mc.fetch("https://api.example.com/data")
if res.ok then
    mc.sendMessage(res.text)
else
    mc.sendMessage("Error: " .. res.error)
end
```

## mc.fetch({...})

Full request with options:

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `url` | string | — | Request URL (required) |
| `method` | string | `"GET"` | HTTP method |
| `headers` | table | `{}` | Custom headers |
| `body` | string | `nil` | Raw request body |
| `json` | table | `nil` | Auto-encodes to JSON and sets `Content-Type: application/json` |
| `timeout` | number | `30` | Timeout in seconds |

```lua
local res = mc.fetch({
    url = "https://api.example.com/data",
    method = "POST",
    json = { key = "value" },
    headers = { Authorization = "Bearer token" },
    timeout = 10
})
```

## Response Table

| Field | Type | Description |
|-------|------|-------------|
| `res.ok` | boolean | `true` if status is 2xx |
| `res.status` | number | HTTP status code |
| `res.text` | string | Response body as string |
| `res.headers` | table | Response headers |
| `res.json` | table or nil | Lazy-parsed JSON (parsed on first access via `__index`) |
| `res.error` | string or nil | Error message if the request failed |

```lua
local res = mc.fetch("https://api.github.com/repos/user/repo")
if res.ok then
    local data = res.json
    mc.sendMessage("Stars: " .. data.stargazers_count)
end
```

## Sequential Example

No callbacks, no nesting — just sequential code:

```lua
register("fetch", function(ctx)
    -- Step 1: fetch a post
    local post = mc.fetch("https://jsonplaceholder.typicode.com/posts/1")
    local postData = post.json
    
    -- Step 2: wait a tick
    mc.sleep(1)
    
    -- Step 3: fetch comments
    local comments = mc.fetch("https://jsonplaceholder.typicode.com/posts/" .. postData.id .. "/comments")
    
    ctx.player:sendMessage("Fetched " .. #comments.json .. " comments")
end)
```

## Lifecycle

All pending coroutines (sleeps, in-flight HTTP requests) are **discarded** on `/pxrp reload`. The Lua state is completely torn down and rebuilt.
