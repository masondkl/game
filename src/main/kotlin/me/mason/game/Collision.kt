package me.mason.game

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
