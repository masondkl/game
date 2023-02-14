package me.mason.game

import org.joml.Matrix4f
import org.joml.Vector3f

interface Camera {
    val position: FloatVector
    val projection: Matrix4f
    val view: Matrix4f
    fun look()
}

fun camera() = object : Camera {
    override val position = vec(0f, 0f)
    override val projection = Matrix4f().setOrtho(-20f, 20f, -11.25f, 11.25f, 0.0f, 100.0f)
    override val view = Matrix4f()
    override fun look() {
        view.identity()
        view.lookAt(
            Vector3f(position.x, position.y, 20f),
            Vector3f(position.x, position.y, -1f),
            Vector3f(0f, 1f, 0f)
        )
    }
    init { look() }
}