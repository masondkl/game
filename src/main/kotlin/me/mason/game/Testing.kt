//package me.mason.game
//
//import org.lwjgl.BufferUtils.createDoubleBuffer
//import org.lwjgl.BufferUtils.createIntBuffer
//import org.lwjgl.glfw.GLFW.*
//import java.nio.file.Paths
//import kotlin.math.PI
//import kotlin.math.atan2
//import kotlin.math.cos
//import kotlin.math.sin
//
//val WINDOW_SCALE = vec(1280f, 720f)
//
//val SWORD_SPRITE = vec(43, 168)
//val SWORD_SPRITE_SCALE = vec(2, 10)
//
////val POINTER_UV_SCALE = 14
////val POINTER_UV_SCALE_VEC = vec(POINTER_UV_SCALE)
////val POINTER_SCALE = 14f
////val POINTER_SCALE_VEC = vec(POINTER_SCALE)
//
//fun mouseCoords(id: Long): FloatVector {
//    val x = createDoubleBuffer(1)
//    val y = createDoubleBuffer(1)
//    val height = createIntBuffer(1)
//    glfwGetCursorPos(id, x, y)
//    glfwGetWindowSize(id, height, height)
//    return vec(x[0].toFloat(), height[0] - y[0].toFloat()) - WINDOW_SCALE / 2f
//}
//
//fun main() = window("Game", 1280, 720) {
//    val rotated = shader(Paths.get("rotated.glsl"), 2, 2, 1, 1)
//    val texture = shader(Paths.get("texture.glsl"), 2, 1)
//    val sheet = texture(Paths.get("sheet512.png"))
//    val sword = mesh(1, rotated)
//    val center = mesh(1, texture)
//    val cursor = mesh(1, texture)
//
//    scene(sheet) {
//        val mouse = mouseCoords(id)
//        val rotation = atan2(mouse.y, mouse.x)
//        val position = vec(cos(rotation), sin(rotation))
//        sword.rotatedSprite(0, SWORD_SPRITE, SWORD_SPRITE_SCALE)
//        sword.rotatedBounds(
//            0,
//            position * 50f,
//            vec(20f, 100f),
//            position * 50f,
//            rotation * (180f / PI).toFloat() + if (position.x > 0) 0f else 180f
//        )
//
//        //window center purple dot
//        center.sprite(0, vec(176, 11), vec(1))
//        center.bounds(0, vec(0f), vec(8f, 8f))
//
//        cursor.sprite(0, POINTER_SPRITE, POINTER_UV_SCALE_VEC)
//        cursor.bounds(0, mouse, POINTER_SCALE_VEC, true)
//
//        draw(center, sword, cursor)
//    }
//}