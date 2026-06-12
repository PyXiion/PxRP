# PxLuaNova

A modernized, maintained fork of LuaJ 3.0.2 with bug fixes extracted from [wagyourtail/luaj](https://github.com/wagyourtail/luaj) and [Cobalt](https://github.com/cc-tweaked/Cobalt), targeting modern Java platforms.

## Features

- **Modern Java**: Requires Java 21+, uses virtual threads for coroutines
- **Bug fixes**: Comprehensive fixes from wagyourtail/luaj and Cobalt
- **Enhanced stdlib**: Additional Lua 5.3+ features (math.type, multi-arg math.min/max)
- **Virtual threads**: Millions of concurrent coroutines with minimal memory overhead
- **Yield from anywhere**: Coroutines can yield from any Java method, not just Lua code
- **Java interop**: Full Java integration via luajava library
- **API compatible**: Drop-in replacement for LuaJ 3.0.2 (same package names)

## Requirements

- Java 21 or higher

## Quick Start

```java
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

// Create globals with standard libraries
Globals globals = JsePlatform.standardGlobals();

// Run a Lua script
globals.load("print('hello, world')").call();

// Call Lua from Java
LuaValue result = globals.load("return 2 + 2").call();
System.out.println(result.toint()); // prints 4
```

## Coroutines & Virtual Threads

PxLuaNova uses Java virtual threads for coroutines by default, enabling millions of concurrent coroutines with minimal memory overhead (~1-10KB per coroutine vs ~1MB for platform threads).

```lua
-- Create 10,000 coroutines without memory issues
local threads = {}
for i = 1, 10000 do
    threads[i] = coroutine.create(function()
        coroutine.yield(i)
    end)
end

-- Resume all of them
for i = 1, 10000 do
    coroutine.resume(threads[i])
end
```

### Platform Threads (Opt-out)

If you need platform threads (e.g., for debugging or compatibility), you can opt-out:

```java
Globals globals = JsePlatform.standardGlobals();
globals.coroutineThreadFactory = LuaThread.PLATFORM_THREAD_FACTORY;
```

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

## What's New

### Phase 7: Virtual Thread Migration (Java 21+)
- Coroutines now use virtual threads by default
- `synchronized`/`wait()`/`notify()` replaced with `ReentrantLock`/`Condition`
- Added configurable `ThreadFactory` (virtual or platform threads)
- `globals.running` is now `volatile` for cross-thread visibility

### Previous Phases
- **Phase 1**: Project setup, Gradle migration, JME removal
- **Phase 2**: Bug fixes from wagyourtail/luaj (Enyby batch)
- **Phase 3**: Error reporting improvements
- **Phase 4**: Bug fixes from Cobalt (ReDoS, string format, etc.)
- **Phase 5**: Stdlib enhancements (math.type, math.atan varargs)
- **Phase 6**: Remaining fixes (numeric, error messages, weak tables)

See [TODO.md](TODO.md) for the complete roadmap and deferred features.

## Migration from LuaJ

PxLuaNova is API-compatible with LuaJ 3.0.2. The package names remain `org.luaj.vm2.*`, so you can drop it in as a replacement.

**Breaking changes:**
- Minimum Java version raised from 11 to 21
- JME (Java ME) support removed
- Coroutines use virtual threads by default (can be disabled)

## Modules

- **pxluanova-core**: Core Lua implementation (interpreter, compiler, standard libraries)
- **pxluanova-jse**: Java SE platform support (JSE libraries, luajava integration)
- **pxluanova-test**: Test suite

## Credits

- [LuaJ](https://github.com/luaj/luaj) - Original LuaJ implementation
- [wagyourtail/luaj](https://github.com/wagyourtail/luaj) - Bug fixes and improvements
- [Cobalt](https://github.com/cc-tweaked/Cobalt) - Additional bug fixes and stdlib enhancements
- [Lua](https://www.lua.org/) - The Lua programming language

## License

MIT License - See [LICENSE](LICENSE) for details.
