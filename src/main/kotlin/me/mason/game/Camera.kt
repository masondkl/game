package me.mason.game

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f

interface Camera {
    val position: Vector2f
    val projection: Matrix4f
    val view: Matrix4f
}

fun camera(position: Vector2f) = object : Camera {
    private val _view = Matrix4f()
    override val position = position
    override val projection = Matrix4f().apply {
        identity()
        ortho(0.0f, 1280.0f, 0.0f, 720.0f, 0.0f, 100.0f)
    }
    val forward = Vector3f(0.0f, 0.0f, -1.0f)
    val up = Vector3f(0.0f, 1.0f, 0.0f)
    override val view: Matrix4f
        get() {
            _view.identity()
            _view.lookAt(
                Vector3f(position.x, position.y, 20.0f),
                forward.add(position.x, position.y, 0.0f),
                up
            )
            return _view
        }
}