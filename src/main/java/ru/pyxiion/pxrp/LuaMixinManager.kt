package ru.pyxiion.pxrp

import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.matcher.ElementMatchers
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.MappingResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.instrument.Instrumentation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

fun interface MixinCallback {
    fun call(instance: Any?, args: Array<Any?>?)
}

object LuaMixinManager {
    private val logger: Logger = LoggerFactory.getLogger(LuaMixinManager::class.java)

    private lateinit var mappingResolver: MappingResolver
    private lateinit var runtimeNamespace: String
    private lateinit var instrumentation: Instrumentation

    private val registry = ConcurrentHashMap<String, ConcurrentMap<String, MixinCallback>>()
    private val installedKeys = ConcurrentHashMap.newKeySet<String>()

    @Volatile private var initialized = false
    private val initLock = Any()

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(initLock) {
            if (initialized) return
            mappingResolver = FabricLoader.getInstance().mappingResolver
            runtimeNamespace = mappingResolver.currentRuntimeNamespace
            instrumentation = ByteBuddyAgent.install()
            initialized = true
            logger.info("LuaMixinManager: ByteBuddy and Mappings initialized. Runtime namespace is '$runtimeNamespace'")
        }
    }

    fun runtimeNamespace(): String {
        ensureInitialized()
        return runtimeNamespace
    }

    @JvmStatic
    fun handleMixinEnter(instance: Any?, className: String?, methodName: String?, args: Array<Any?>?) {
        if (className == null || methodName == null) return
        val classMixins = registry[className] ?: return
        val luaFunction = classMixins[methodName] ?: return
        try {
            luaFunction.call(instance, args)
        } catch (e: Throwable) {
            logger.warn("LuaMixinManager: error in observe callback for $className#$methodName: ${e.message}", e)
        }
    }

    object MixinTransformer {
        @JvmStatic
        @Advice.OnMethodEnter
        fun enter(
            @Advice.This(optional = true) instance: Any?,
            @Advice.Origin("#t") className: String?,
            @Advice.Origin("#m") methodName: String?,
            @Advice.AllArguments args: Array<Any?>?
        ) {
            handleMixinEnter(instance, className, methodName, args)
        }
    }

    fun observeHook(sourceNamespace: String, className: String, methodName: String, callback: MixinCallback) {
        try {
            ensureInitialized()
        } catch (e: Throwable) {
            logger.warn("LuaMixinManager: failed to initialize ByteBuddy agent: ${e.message}")
            return
        }

        val runtimeClassName = try {
            mappingResolver.mapClassName(sourceNamespace, className)
        } catch (e: Throwable) {
            logger.warn("LuaMixinManager: failed to map class '$className' from namespace '$sourceNamespace': ${e.message}")
            return
        }

        val targetClass = try {
            Class.forName(runtimeClassName)
        } catch (e: ClassNotFoundException) {
            logger.warn("LuaMixinManager: class '$runtimeClassName' (mapped from '$className') not found")
            return
        }

        val matchingMethods = targetClass.methods.filter { it.name == methodName }
        when {
            matchingMethods.isEmpty() -> {
                logger.warn("LuaMixinManager: method '$methodName' not found in class '$runtimeClassName'")
                return
            }
            matchingMethods.size > 1 -> {
                logger.warn("LuaMixinManager: ambiguous hook — '$methodName' in '$runtimeClassName' has ${matchingMethods.size} overloads; this version requires a single match by name")
                return
            }
        }

        registry.getOrPut(runtimeClassName) { ConcurrentHashMap() }[methodName] = callback

        val installKey = "$runtimeClassName#$methodName"
        if (installedKeys.add(installKey)) {
            try {
                AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.`is`(targetClass))
                    .transform { builder, _, _, _, _ ->
                        builder.visit(Advice.to(MixinTransformer::class.java).on(ElementMatchers.named(methodName)))
                    }
                    .installOn(instrumentation)
                logger.info("LuaMixinManager: installed observe hook for $runtimeClassName#$methodName")
            } catch (e: Throwable) {
                installedKeys.remove(installKey)
                registry[runtimeClassName]?.remove(methodName)
                logger.warn("LuaMixinManager: failed to install hook for $runtimeClassName#$methodName: ${e.message}")
            }
        }
    }

    fun removeHook(sourceNamespace: String, className: String, methodName: String): Boolean {
        if (!initialized) return false
        val runtimeClassName = try {
            mappingResolver.mapClassName(sourceNamespace, className)
        } catch (_: Throwable) {
            return false
        }
        val removed = registry[runtimeClassName]?.remove(methodName) != null
        if (removed) {
            logger.info("LuaMixinManager: removed observe hook for $runtimeClassName#$methodName")
        }
        return removed
    }

    fun clearHooks() {
        registry.clear()
        logger.info("LuaMixinManager: cleared all observe hooks")
    }

    internal fun registerForTest(className: String, methodName: String, callback: MixinCallback) {
        registry.getOrPut(className) { ConcurrentHashMap() }[methodName] = callback
    }
}
