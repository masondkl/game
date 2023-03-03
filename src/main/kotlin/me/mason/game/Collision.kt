package me.mason.game

import kotlin.math.abs

interface Collider {
    val position: FloatVector
    val scale: FloatVector
}

fun collider(
    position: FloatVector,
    scale: FloatVector
) = object : Collider {
    override val position = position
    override val scale = scale
}

fun Collider.collides(
    other: Collider,
    change: FloatVector = vec(0f, 0f),
    expand: FloatVector = vec(0f, 0f)
): Boolean {
    return position.y + change.y - (scale.y + expand.y) / 2 < other.position.y + other.scale.y / 2 &&
            position.y + change.y + (scale.y + expand.y) / 2 > other.position.y - other.scale.y / 2 &&
            position.x + change.x - (scale.x + expand.x) / 2 < other.position.x + other.scale.x / 2 &&
            position.x + change.x + (scale.x + expand.x) / 2 > other.position.x - other.scale.x / 2
}

fun Collider.move(motion: FloatVector, collisions: List<Collider>): FloatVector {
    val change = vec(motion.x, motion.y)
    val intersectsHor = collisions.find { collides(it, change = vec(change.x, 0f)) }
    val intersectsDia = collisions.find { collides(it, change = vec(change.x, change.y)) }
    val intersectVer = collisions.find { collides(it, change = vec(0f, change.y)) }
    if (intersectVer != null) {
        val max = intersectVer.position.y + intersectVer.scale.y / 2
        val min = position.y + change.y - scale.y / 2
        if (max - min < abs(change.y)) change.y += (max - min) * 1.05f
        else change.y = 0f
    }
    if (intersectsHor != null || intersectVer == null && intersectsDia != null) {
        val with = intersectsHor ?: intersectsDia!!
        val withMax = with.position.x + with.scale.x / 2
        val withMin = with.position.x - with.scale.x / 2
        val max = position.x + change.x + scale.x / 2
        val min = position.x + change.x - scale.x / 2
        if (withMax - min > change.x && withMax - min < with.scale.x) {
            change.x += (withMax - min) * 1.1f
        } else if (max - withMin < change.x && max - withMin < with.scale.x) {
            change.x -= (max - withMin) * 1.1f
        } else change.x = 0f
    }; return change
}