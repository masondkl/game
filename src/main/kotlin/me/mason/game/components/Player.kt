package me.mason.game.components

import me.mason.game.*
import org.lwjgl.glfw.GLFW.*
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

private val LEFT_1_SPRITE = uv(vec(0, 0), vec(15, 20))
private val LEFT_2_SPRITE = uv(vec(15, 0), vec(15, 20))
private val LEFT_3_SPRITE = uv(vec(30, 0), vec(15, 20))
private val LEFT_4_SPRITE = uv(vec(45, 0), vec(15, 20))
private val LEFT_SPRITES = arrayOf(LEFT_1_SPRITE, LEFT_2_SPRITE, LEFT_3_SPRITE, LEFT_4_SPRITE)

private val RIGHT_1_SPRITE = uv(vec(60, 0), vec(15, 20))
private val RIGHT_2_SPRITE = uv(vec(75, 0), vec(15, 20))
private val RIGHT_3_SPRITE = uv(vec(90, 0), vec(15, 20))
private val RIGHT_4_SPRITE = uv(vec(105, 0), vec(15, 20))
private val RIGHT_SPRITES = arrayOf(RIGHT_1_SPRITE, RIGHT_2_SPRITE, RIGHT_3_SPRITE, RIGHT_4_SPRITE)

private val WALK_LEFT_1_SPRITE = uv(vec(0, 20), vec(15, 20))
private val WALK_LEFT_2_SPRITE = uv(vec(15, 20), vec(15, 20))
private val WALK_LEFT_3_SPRITE = uv(vec(30, 20), vec(15, 20))
private val WALK_LEFT_4_SPRITE = uv(vec(45, 20), vec(15, 20))
private val WALK_LEFT_SPRITES = arrayOf(WALK_LEFT_1_SPRITE, WALK_LEFT_2_SPRITE, WALK_LEFT_3_SPRITE, WALK_LEFT_4_SPRITE)

private val WALK_RIGHT_1_SPRITE = uv(vec(60, 20), vec(15, 20))
private val WALK_RIGHT_2_SPRITE = uv(vec(75, 20), vec(15, 20))
private val WALK_RIGHT_3_SPRITE = uv(vec(90, 20), vec(15, 20))
private val WALK_RIGHT_4_SPRITE = uv(vec(105, 20), vec(15, 20))
private val WALK_RIGHT_SPRITES = arrayOf(WALK_RIGHT_1_SPRITE, WALK_RIGHT_2_SPRITE, WALK_RIGHT_3_SPRITE, WALK_RIGHT_4_SPRITE)

private val JUMP_LEFT_SPRITE = LEFT_4_SPRITE
private val JUMP_RIGHT_SPRITE = RIGHT_1_SPRITE

private val FALL_LEFT_SPRITE = LEFT_1_SPRITE
private val FALL_RIGHT_SPRITE = RIGHT_4_SPRITE

private val LEFT = 0
private val RIGHT = 1
private val WALK_LEFT = 2
private val WALK_RIGHT = 3
private val JUMP_LEFT = 4
private val JUMP_RIGHT = 5
private val FALL_LEFT = 6
private val FALL_RIGHT = 7

private val SCALE = vec(47.9f, 63.9f)
private val ANIMATION = animation(
    state(0 until 4, LEFT_SPRITES, SCALE, rate = 0.5.seconds),
    state(4 until 8, RIGHT_SPRITES, SCALE, rate = 0.5.seconds),
    state(8 until 12, WALK_LEFT_SPRITES, SCALE, rate = 0.5.seconds),
    state(12 until 16, WALK_RIGHT_SPRITES, SCALE, rate = 0.5.seconds),
    state(16, JUMP_LEFT_SPRITE, SCALE), state(17, JUMP_RIGHT_SPRITE, SCALE),
    state(18, FALL_LEFT_SPRITE, SCALE), state(19, FALL_RIGHT_SPRITE, SCALE)
)

fun Window.player(
    world: World
): Entity {
    var jumped = false
    keys(GLFW_KEY_SPACE, GLFW_PRESS) { _, _ -> jumped = true }
    return entity(vec(0f, 0f), SCALE, ANIMATION) {
        val collisions = world.nearbyBlocks().filterNotNull()
        motion.x = (if (keys[GLFW_KEY_D]) 375f else 0f) + (if (keys[GLFW_KEY_A]) -375f else 0f)
        if (jumped && collisions.any { collides(it, vec(0f, -1f)) })
            motion.y = 450f
        if (abs(motion.x) > 1f / 32f || abs(motion.y) > 1f / 32f)
            motion(collisions)
        gravity()
        jumped = false

        val animation = animations[0]!!
        val state = animation.state()
        if (collisions.any { collides(it, vec(0f, -32f)) }) {
            if (abs(motion.x) > 0f) {
                if (motion.x < 0f && state != WALK_LEFT) animation.state(WALK_LEFT)
                if (motion.x > 0f && state != WALK_RIGHT) animation.state(WALK_RIGHT)
            }
            else if (state in arrayOf(WALK_LEFT, FALL_LEFT, JUMP_LEFT)) animation.state(LEFT)
            else if (state in arrayOf(WALK_RIGHT, FALL_RIGHT, JUMP_RIGHT)) animation.state(RIGHT)
        } else {
            if (motion.y > 0f) {
                if (abs(motion.x) > 0f) {
                    if (motion.x < 0f && state !in arrayOf(JUMP_LEFT)) animation.state(JUMP_LEFT)
                    if (motion.x > 0f && state !in arrayOf(JUMP_RIGHT)) animation.state(JUMP_RIGHT)
                }
                else if (state in arrayOf(WALK_LEFT, FALL_LEFT, LEFT)) animation.state(JUMP_LEFT)
                else if (state in arrayOf(WALK_RIGHT, FALL_RIGHT, RIGHT)) animation.state(JUMP_RIGHT)
            } else if (motion.y < 0f) {
                if (abs(motion.x) > 0f) {
                    if (motion.x < 0f && state != FALL_LEFT) animation.state(FALL_LEFT)
                    if (motion.x > 0f && state != FALL_RIGHT) animation.state(FALL_RIGHT)
                }
                else if (state in arrayOf(WALK_LEFT, JUMP_LEFT, LEFT)) animation.state(FALL_LEFT)
                else if (state in arrayOf(WALK_RIGHT, JUMP_RIGHT, RIGHT)) animation.state(FALL_RIGHT)
            }
        }
    }.apply { position.y = world.highest(0f) + scale.y / 2 + 32f }
}