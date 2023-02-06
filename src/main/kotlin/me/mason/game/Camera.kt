package me.mason.game

import org.joml.Matrix4f
import org.joml.Vector3f

interface Camera {
    var position: FloatVector
    val projection: Matrix4f
    val view: Matrix4f
}

fun camera(position: FloatVector) = object : Camera {
    private val _position = position
    override var position
        get() = _position
        set(value) {
            _position.x = value.x
            _position.y = value.y
            look()
        }
    override val projection = Matrix4f().setOrtho(-1280f/2f, 1280.0f/2, -720f/2f, 720.0f/2, 0.0f, 100.0f)
    override val view = Matrix4f()
    private fun look() {
        view.identity()
        view.lookAt(
            Vector3f(_position.x, _position.y, 20f),
            Vector3f(_position.x, _position.y, -1f),
            Vector3f(0f, 1f, 0f)
        )
    }
    init { look() }
}