package ru.pyxiion.pxrp

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LuaMixinManagerTest {

    @AfterTest
    fun cleanup() {
        LuaMixinManager.clearHooks()
    }

    @Test
    fun `handleMixinEnter invokes registered callback with instance and args`() {
        val received = AtomicReference<Pair<Any?, Array<Any?>?>?>(null)
        LuaMixinManager.registerForTest("test.A", "m") { instance, args ->
            received.set(Pair(instance, args))
        }

        val inst = Any()
        val arr: Array<Any?> = arrayOf("x", 42, inst)
        LuaMixinManager.handleMixinEnter(inst, "test.A", "m", arr)

        val r = received.get()!!
        assertEquals(inst, r.first)
        assertEquals(3, r.second!!.size)
        assertEquals("x", r.second!![0])
        assertEquals(42, r.second!![1])
        assertEquals(inst, r.second!![2])
    }

    @Test
    fun `handleMixinEnter with null className is a no-op`() {
        val called = AtomicBoolean(false)
        LuaMixinManager.registerForTest("test.A", "m") { _, _ -> called.set(true) }
        LuaMixinManager.handleMixinEnter(null, null, "m", null)
        assertFalse(called.get())
    }

    @Test
    fun `handleMixinEnter with null methodName is a no-op`() {
        val called = AtomicBoolean(false)
        LuaMixinManager.registerForTest("test.A", "m") { _, _ -> called.set(true) }
        LuaMixinManager.handleMixinEnter(null, "test.A", null, null)
        assertFalse(called.get())
    }

    @Test
    fun `handleMixinEnter with null args passes null through`() {
        val received = AtomicReference<Array<Any?>?>(null)
        LuaMixinManager.registerForTest("test.A", "m") { _, args ->
            received.set(args)
        }
        LuaMixinManager.handleMixinEnter(null, "test.A", "m", null)
        assertNull(received.get())
    }

    @Test
    fun `handleMixinEnter with unregistered class is a no-op`() {
        LuaMixinManager.handleMixinEnter(null, "NotRegistered", "m", null)
    }

    @Test
    fun `handleMixinEnter with unregistered method on registered class is a no-op`() {
        val called = AtomicBoolean(false)
        LuaMixinManager.registerForTest("test.A", "other") { _, _ -> called.set(true) }
        LuaMixinManager.handleMixinEnter(null, "test.A", "missing", null)
        assertFalse(called.get())
    }

    @Test
    fun `handleMixinEnter swallows callback exceptions and does not propagate`() {
        LuaMixinManager.registerForTest("test.A", "boom") { _, _ ->
            throw RuntimeException("intentional")
        }
        LuaMixinManager.handleMixinEnter(null, "test.A", "boom", null)
    }

    @Test
    fun `handleMixinEnter swallows LuaError and does not propagate`() {
        LuaMixinManager.registerForTest("test.A", "boom") { _, _ ->
            throw org.luaj.vm2.LuaError("script bug")
        }
        LuaMixinManager.handleMixinEnter(null, "test.A", "boom", null)
    }

    @Test
    fun `clearHooks removes all registered callbacks`() {
        val called = AtomicBoolean(false)
        LuaMixinManager.registerForTest("test.A", "m") { _, _ -> called.set(true) }
        LuaMixinManager.clearHooks()
        LuaMixinManager.handleMixinEnter(null, "test.A", "m", null)
        assertFalse(called.get())
    }

    @Test
    fun `clearHooks is safe to call on empty registry`() {
        LuaMixinManager.clearHooks()
        LuaMixinManager.clearHooks()
    }

    @Test
    fun `clearHooks does not throw when uninitialized`() {
        LuaMixinManager.clearHooks()
    }

    @Test
    fun `removeHook returns false when manager is uninitialized`() {
        assertFalse(LuaMixinManager.removeHook("named", "anything", "anything"))
    }

    @Test
    fun `multiple methods on same class are independent`() {
        val aCalled = AtomicBoolean(false)
        val bCalled = AtomicBoolean(false)
        LuaMixinManager.registerForTest("test.A", "alpha") { _, _ -> aCalled.set(true) }
        LuaMixinManager.registerForTest("test.A", "beta") { _, _ -> bCalled.set(true) }

        LuaMixinManager.handleMixinEnter(null, "test.A", "alpha", null)
        assertTrue(aCalled.get())
        assertFalse(bCalled.get())

        LuaMixinManager.handleMixinEnter(null, "test.A", "beta", null)
        assertTrue(bCalled.get())
    }

    @Test
    fun `re-registering a method replaces the previous callback`() {
        val firstCalled = AtomicBoolean(false)
        val secondCalled = AtomicBoolean(false)
        LuaMixinManager.registerForTest("test.A", "m") { _, _ -> firstCalled.set(true) }
        LuaMixinManager.registerForTest("test.A", "m") { _, _ -> secondCalled.set(true) }

        LuaMixinManager.handleMixinEnter(null, "test.A", "m", null)
        assertFalse(firstCalled.get())
        assertTrue(secondCalled.get())
    }
}
