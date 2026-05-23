package ru.pyxiion.pxrp

import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import ru.pyxiion.pxrp.PxRp.Companion.logger

class Scheduler {
    private var nextId = 0
    private val tasks = mutableListOf<ScheduledTask>()

    fun tick() {
        if (tasks.isEmpty()) return
        val iterator = tasks.listIterator()
        while (iterator.hasNext()) {
            val task = iterator.next()
            task.remaining--
            if (task.remaining <= 0) {
                try {
                    task.callback.invoke()
                } catch (e: Throwable) {
                    logger.warn("Ошибка в задании планировщика #${task.id}: ${e.message}")
                }
                if (task.repeating) {
                    task.remaining = task.interval
                } else {
                    iterator.remove()
                }
            }
        }
    }

    fun schedule(delay: Int, callback: LuaFunction): Int {
        val id = nextId++
        tasks.add(ScheduledTask(id, delay.coerceAtLeast(0), 0, false, callback))
        return id
    }

    fun scheduleRepeating(delay: Int, interval: Int, callback: LuaFunction): Int {
        val id = nextId++
        tasks.add(ScheduledTask(id, delay.coerceAtLeast(0), interval.coerceAtLeast(0), true, callback))
        return id
    }

    fun cancel(id: Int): Boolean {
        return tasks.removeAll { it.id == id }
    }

    fun clear() {
        tasks.clear()
    }
}

data class ScheduledTask(
    val id: Int,
    var remaining: Int,
    val interval: Int,
    val repeating: Boolean,
    val callback: LuaFunction
)
