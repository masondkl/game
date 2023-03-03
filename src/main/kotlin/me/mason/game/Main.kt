package me.mason.game

import me.mason.game.component.*
import org.lwjgl.glfw.GLFW.*
import java.nio.file.Paths
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource.Monotonic.markNow

const val SHEET_SIZE = 512
val SHEET_SIZE_VEC = vec(SHEET_SIZE)
val WINDOW_SCALE = vec(1280f, 720f)
val WINDOW_RADIUS = WINDOW_SCALE / 2f

val POINTER_SPRITE = vec(13, 159)
val POINTER_UV_SCALE = 14
val POINTER_UV_SCALE_VEC = vec(POINTER_UV_SCALE)
val POINTER_SCALE = 14f
val POINTER_SCALE_VEC = vec(POINTER_SCALE)

interface Drop : Mesh {
    val material: Byte
    val collider: Collider
    val timestamp: TimeMark
    val tick: Tick
}

val DROP_SCALE = 12f
val DROP_SCALE_VEC = vec(DROP_SCALE)

fun MutableList<Drop>.drop(shader: Shader, world: World, position: FloatVector, material: Byte) {
    val delegate = mesh(shader, 1)
    val drop = object : Drop, Mesh by delegate {
        val self = this
        val _position = position.clone()
        override val collider = collider(_position, vec(DROP_SCALE_VEC.x, DROP_SCALE_VEC.y * 2))
        override val timestamp = markNow()
        override val material = material
        override val tick: Tick = {
            _position += collider.move(vec(0f, max(min(-1500 * dt, 512f * dt), -512f * dt)), world.tileColliders(_position, TILE_SCALE * 12f))
            sprite(0, materialSprite(self.material), materialScale(self.material))
            bounds(0, vec(_position.x, _position.y + sin(elapsed * 3f) * (DROP_SCALE / 2f)), DROP_SCALE_VEC)
        }
    }; add(drop)
}

fun main() = window("Game", 1280, 720) {
    val texture = shader(Paths.get("texture.glsl"), 2, 1)
    val sheet = texture(Paths.get("sheet512.png"))
    val alphabet = alphabet()
    val drops = ArrayList<Drop>()
    val world = world(texture, drops)
    val inventory = inventory(texture, alphabet)
    val player = player(texture, world, inventory, drops)
    val cursorMesh = mesh(texture, 1)
    val fps = alphabet.text(texture, WINDOW_SCALE / 2f - vec(200f, 50f), vec(10f), "MIN MAX FPS 0 0")
    var last = markNow()
    var fpsMin = Int.MAX_VALUE
    var lastMin = markNow()

    inventory.add(materialItem(MATERIAL_GRASS) { amount = MAX_ITEM_AMOUNT })
    inventory.add(materialItem(MATERIAL_DIRT) { amount = MAX_ITEM_AMOUNT })
    inventory.add(materialItem(MATERIAL_STONE) { amount = MAX_ITEM_AMOUNT })
    inventory.add(materialItem(MATERIAL_STONE) { amount = MAX_ITEM_AMOUNT })

    scene(sheet) {
        cursorMesh.sprite(0, POINTER_SPRITE, POINTER_UV_SCALE_VEC)
        cursorMesh.bounds(0, cursor, POINTER_SCALE_VEC)

        if (mouse[GLFW_MOUSE_BUTTON_LEFT] && cursor.distance(player.position) <= 96f)
            world.breakAt(cursor, dt * 50f)

        camera.position.set(player.position.int().float())
        camera.look()

        world.tick(this)
        player.tick(this)
        inventory.tick(this)
        fps.tick(this)
        drops.removeIf {
            it.tick(this)
            it.timestamp.elapsedNow() > 20.seconds
        }
        draw(world, player, inventory, cursorMesh, fps, *drops.toTypedArray())
        val frames = (1f / dt).toInt()
        if (frames < fpsMin) fpsMin = minOf(fpsMin, frames)
        if (last.elapsedNow() > 0.05.seconds) {
            fps.value = "MIN MAX FPS ${"% 5d".format(fpsMin)} ${"% 5d".format(frames)}"
            last = markNow()
        }
        if (lastMin.elapsedNow() > 5.seconds) {
            fpsMin = Int.MAX_VALUE
            lastMin = markNow()
        }
    }
}