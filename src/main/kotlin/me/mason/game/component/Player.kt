package me.mason.game.component

import me.mason.game.*

private val QUADS = 1
private val UV_SCALE = vec(15, 20)
private val SCALE = vec(48f, 64f)

val LEFT =       arrayOf(vec(0,   0), vec(15,  0), vec(30,  0), vec(45,   0))
val RIGHT =      arrayOf(vec(60,  0), vec(75,  0), vec(90,  0), vec(105,  0))
private val WALK_LEFT =  arrayOf(vec(0,  20), vec(15, 20), vec(30, 20), vec(45,  20))
private val WALK_RIGHT = arrayOf(vec(60, 20), vec(75, 20), vec(90, 20), vec(105, 20))

//fun Window.player(position: FloatVector): MeshAdapter {
//    var animation = LEFT
//    var sprite = 0
//    return { mesh, index ->
//        mesh.uv(index, animation[sprite], UV_SCALE)
//        mesh.vertices(index, position, SCALE)
//        QUADS
//    }
//}


//private val LEFT_1_SPRITE = uv(vec(0, 0), vec(15, 20))
//private val LEFT_2_SPRITE = uv(vec(15, 0), vec(15, 20))
//private val LEFT_3_SPRITE = uv(vec(30, 0), vec(15, 20))
//private val LEFT_4_SPRITE = uv(vec(45, 0), vec(15, 20))
//private val LEFT_SPRITES = arrayOf(LEFT_1_SPRITE, LEFT_2_SPRITE, LEFT_3_SPRITE, LEFT_4_SPRITE)
//
//private val RIGHT_1_SPRITE = uv(vec(60, 0), vec(15, 20))
//private val RIGHT_2_SPRITE = uv(vec(75, 0), vec(15, 20))
//private val RIGHT_3_SPRITE = uv(vec(90, 0), vec(15, 20))
//private val RIGHT_4_SPRITE = uv(vec(105, 0), vec(15, 20))
//private val RIGHT_SPRITES = arrayOf(RIGHT_1_SPRITE, RIGHT_2_SPRITE, RIGHT_3_SPRITE, RIGHT_4_SPRITE)
//
//private val WALK_LEFT_1_SPRITE = uv(vec(0, 20), vec(15, 20))
//private val WALK_LEFT_2_SPRITE = uv(vec(15, 20), vec(15, 20))
//private val WALK_LEFT_3_SPRITE = uv(vec(30, 20), vec(15, 20))
//private val WALK_LEFT_4_SPRITE = uv(vec(45, 20), vec(15, 20))
//private val WALK_LEFT_SPRITES = arrayOf(WALK_LEFT_1_SPRITE, WALK_LEFT_2_SPRITE, WALK_LEFT_3_SPRITE, WALK_LEFT_4_SPRITE)
//
//private val WALK_RIGHT_1_SPRITE = uv(vec(60, 20), vec(15, 20))
//private val WALK_RIGHT_2_SPRITE = uv(vec(75, 20), vec(15, 20))
//private val WALK_RIGHT_3_SPRITE = uv(vec(90, 20), vec(15, 20))
//private val WALK_RIGHT_4_SPRITE = uv(vec(105, 20), vec(15, 20))
//private val WALK_RIGHT_SPRITES = arrayOf(WALK_RIGHT_1_SPRITE, WALK_RIGHT_2_SPRITE, WALK_RIGHT_3_SPRITE, WALK_RIGHT_4_SPRITE)
