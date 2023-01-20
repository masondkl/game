package me.mason.game.components

import me.mason.game.*

interface Inventory : Entity {

}

private val TOP_LEFT = vec(-1280f/2f, 720f/2f)

fun Window.inventory(cols: Int, rows: Int): Inventory {
    val inset = 32f
    val slotSize = 24f
    val padding = inset * 2f
    val scale = vec(slotSize * cols + padding, slotSize * rows + padding)
    val background = sliced(
        TOP_LEFT + vec(scale.x, -scale.y) / 2f + vec(16f, -16f), scale, inset,
        vec(4, 91), 24
    )
    val slots = Array(rows * cols) {
        val x = it % cols
        val y = it / cols
        val position = TOP_LEFT +
                vec(inset, -inset) +
                vec(slotSize / 2, -slotSize / 2) +
                vec((scale.x - 64f) / cols, (-scale.y + 64f) / rows) * vec(x, y).float() +
                vec(16f, -16f)
        val animation = animation(state(0, uv(vec(0, 91), vec(4, 4)), vec(slotSize, slotSize)))
        entity(
            position,
            vec(slotSize, slotSize),
            animation
        )
    }
    val items = Array(rows * cols) {
        val x = it % cols
        val y = it / cols
        val position = TOP_LEFT +
                vec(inset, -inset) +
                vec(slotSize / 2, -slotSize / 2) +
                vec((scale.x - 64f) / cols, (-scale.y + 64f) / rows) * vec(x, y).float() +
                vec(16f, -16f)
        entity(
            position,
            vec(slotSize, slotSize),
            null
        )
    }
    return object : Inventory, Entity by group(background, *slots, *items, ui = true) {

    }
}