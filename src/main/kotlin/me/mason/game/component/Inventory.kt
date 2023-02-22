package me.mason.game.component

import me.mason.game.*

interface Inventory : Mesh {
    val tick: Tick
}

private const val SLOTS_X = 9
private const val SLOTS_Y = 4
private const val SLOTS = SLOTS_X * SLOTS_Y

fun Window.inventory(shader: Shader): Inventory {
    val mesh = mesh(9 + SLOTS * 9 + SLOTS, shader)
    val slot = 24f
    val padding = 2f
    val margin = 8f
    val inventorySize = vec(
        SLOTS_X * slot + (SLOTS_X - 1) * padding + 2 * margin,
        SLOTS_Y * slot + (SLOTS_Y - 1) * padding + 2 * margin
    )
    val inventoryPosition = vec(-496f, 288f)
    val inventory = sliced(
        shader, inventoryPosition,
        inventorySize,
        slot / 3f, vec(4, 91), 12, true
    )
    val slots = Array(36) {
        val x = it % 9
        val y = it / 9
        val size = slot + padding
        val start = vec(
            inventoryPosition.x - inventorySize.x / 2 + slot / 2 + margin,
            inventoryPosition.y + inventorySize.y / 2 - slot / 2 - margin
        )
        val position = vec(start.x + size * x, start.y - size * y)
        sliced(shader, position, vec(slot), slot / 3, vec(4, 91), 12, true)
    }
    val items = Array(36) {
        val x = it % 9
        val y = it / 9
        val size = slot + padding
        val start = vec(
            inventoryPosition.x - inventorySize.x / 2 + slot / 2 + margin,
            inventoryPosition.y + inventorySize.y / 2 - slot / 2 - margin
        )
        val position = vec(start.x + size * x, start.y - size * y)
        sliced(shader, position, vec(slot), slot / 3, vec(4, 91), 12, true)
    }
    return object : Inventory, Mesh by mesh {
        override val tick: Tick = {
            inventory.tick(this)
            slots.forEach { it.tick(this) }
            mesh.copy(inventory.data, 0, 0, 9)
            slots.forEachIndexed { index, slot ->
                mesh.copy(slot.data, 0, 9 + index * 9, 9)
            }
//            items.forEachIndexed { index, item -> item.tick(this); mesh.copyBounds(item.data, 0, 9 + slots.size * 9 + index) }
        }
    }
}