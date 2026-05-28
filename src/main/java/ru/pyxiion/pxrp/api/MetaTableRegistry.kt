package ru.pyxiion.pxrp.api

import org.luaj.vm2.LuaTable

object MetaTableRegistry {
    private var _entity = LuaTable()
    private var _player = LuaTable()
    private var _world = LuaTable()
    private var _structure = LuaTable()
    private var _item = LuaTable().also { it.set("__index", it) }
    private var _vec = LuaTable()
    private var _inventory = LuaTable()
    private var _container = LuaTable()

    val ENTITY: LuaTable get() = _entity
    val PLAYER: LuaTable get() = _player
    val WORLD: LuaTable get() = _world
    val STRUCTURE: LuaTable get() = _structure
    val ITEM: LuaTable get() = _item
    val VEC: LuaTable get() = _vec
    val INVENTORY: LuaTable get() = _inventory
    val CONTAINER: LuaTable get() = _container

    private var byName = mapOf(
        "entity" to _entity,
        "player" to _player,
        "world" to _world,
        "structure" to _structure,
        "item" to _item,
        "vec" to _vec,
        "inventory" to _inventory,
        "container" to _container,
    )

    fun init() {
        _entity = LuaTable()
        _player = LuaTable()
        _world = LuaTable()
        _structure = LuaTable()
        _item = LuaTable().also { it.set("__index", it) }
        _vec = LuaTable()
        _inventory = LuaTable()
        _container = LuaTable()
        byName = mapOf(
            "entity" to _entity,
            "player" to _player,
            "world" to _world,
            "structure" to _structure,
            "item" to _item,
            "vec" to _vec,
            "inventory" to _inventory,
            "container" to _container,
        )
        initVecMeta(_vec)
        EntityWrapper.initMeta(_entity)
        PlayerWrapper.initMeta(_player)
        WorldWrapper.initMeta(_world)
        StructureWrapper.initMeta(_structure)
        InvWrapper.initMeta(_inventory)
        ContainerWrapper.initMeta(_container)
    }

    fun get(name: String): LuaTable = byName[name]
        ?: throw IllegalArgumentException("Unknown metatable type: '$name'")
}
