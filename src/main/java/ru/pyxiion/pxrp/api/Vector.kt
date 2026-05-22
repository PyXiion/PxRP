package ru.pyxiion.pxrp.api

import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

data class Vector(
    @JvmField
    val x: Double,
    @JvmField
    val y: Double,
    @JvmField
    val z: Double,
) {
    companion object {
        fun fromMc(vec: Vec3d): Vector {
            return Vector(vec.x, vec.y, vec.z)
        }

        fun fromRotation(yaw: Float, pitch: Float): Vector {
            val f = pitch * (Math.PI / 180.0).toDouble()
            val g = -yaw * (Math.PI / 180.0).toDouble()
            val h = MathHelper.cos(g)
            val i = MathHelper.sin(g)
            val j = MathHelper.cos(f)
            val k = MathHelper.sin(f)
            return Vector((i * j).toDouble(), -k.toDouble(), (h * j).toDouble())
        }
    }
}
