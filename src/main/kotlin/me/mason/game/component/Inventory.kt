package me.mason.game.component

import me.mason.game.*
import org.lwjgl.glfw.GLFW.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val MAX_ITEM_AMOUNT = 100
private const val SLOTS_X = 9
private const val SLOTS_Y = 4
private const val SLOTS = SLOTS_X * SLOTS_Y
private val UNSELECTED_SPRITE = vec(0, 91)
private val SELECTED_SPRITE = vec(12, 91)

typealias Use = Window.(Player, World, Inventory, Item) -> (Unit)
val EMPTY_USE: Use = { _, _, _, _ -> }
val PLACE: Use = use@{ player, world, _, item ->
    val collider = world.collider(cursor)
    println(world[cursor])
    if (collider.let { player.collider.collides(it) } || cursor.distance(player.position) > 96f || world[cursor] != MATERIAL_AIR)
        return@use
    world[cursor] = item.material
    item.remove(1)
}

interface Item {
    var material: Byte
    var amount: Int
    var left: Use
    var right: Use
}

fun Item.clear() {
    material = MATERIAL_AIR
    amount = 0
    left = EMPTY_USE
    right = EMPTY_USE
}

fun Item.remove(amount: Int): Int {
    val next = this.amount - amount
    this.amount = maxOf(0, next)
    if (this.amount == 0) {
        material = MATERIAL_AIR
        left = EMPTY_USE
        right = EMPTY_USE
    }
    return if (next < 0) abs(next) else 0
}

fun Item.clone() = object : Item {
    override var material = this@clone.material
    override var amount = this@clone.amount
    override var left = this@clone.left
    override var right = this@clone.right

}

fun Item.add(amount: Int): Int {
    val next = this.amount + amount
    this.amount = minOf(MAX_ITEM_AMOUNT, next)
    return if (next > MAX_ITEM_AMOUNT) next - MAX_ITEM_AMOUNT else 0
}

fun materialItem(type: Byte, block: Item.() -> (Unit) = {}) = object : Item {
    override var material = type
    override var amount = 1
    override var left = EMPTY_USE
    override var right = PLACE
    init { block(this) }
}

interface Inventory : Mesh {
    val items: Array<Item>
    val hand: Item?
    var selected: Int
    val tick: Tick
    operator fun set(index: Int, item: Item)
    operator fun get(index: Int): Item
    fun remove(index: Int, amount: Int): Int
}

fun Inventory.add(item: Item) {
    var index = 0
    var amount = item.amount
    while (amount != 0 && index < SLOTS) {
        val at = get(index)
        if (at.material == MATERIAL_AIR) {
            at.material = item.material
            at.amount = item.amount
            at.left = item.left
            at.right = item.right
            amount = 0
        } else if (at.material == item.material) {
            val before = at.amount
            at.add(item.amount)
            amount = maxOf(0, item.amount - (at.amount - before))
        }; index++
    }
}

fun Window.inventory(shader: Shader, alphabet: Array<IntVector>): Inventory {
    var selected = 0
    val limit = 9 + SLOTS * 9 + SLOTS * 5 + 5
    val mesh = mesh(shader, limit)
    val slot = 32f
    val padding = 2f
    val margin = 8f
    val inventorySize = vec(
        SLOTS_X * slot + (SLOTS_X - 1) * padding + 2 * margin,
        SLOTS_Y * slot + (SLOTS_Y - 1) * padding + 2 * margin
    )
    val inventoryPosition = vec(-32f * 14, 32f * 8)
    val inventory = sliced(
        shader, inventoryPosition,
        inventorySize,
        slot / 3f, UNSELECTED_SPRITE, 12, true
    )
    val center = Array(SLOTS) {
        val x = it % 9
        val y = it / 9
        val size = slot + padding
        val start = vec(
            inventoryPosition.x - inventorySize.x / 2 + slot / 2 + margin,
            inventoryPosition.y + inventorySize.y / 2 - slot / 2 - margin
        )
        vec(start.x + size * x, start.y - size * y)
    }
    val slots = Array(SLOTS) {
        sliced(shader, center[it], vec(slot), slot / 3, UNSELECTED_SPRITE, 12, ui = true)
    }
    val text = Array(SLOTS) {
        alphabet.text(shader, center[it] + vec(slot * 1f / 3f, -slot * 1f / 3f), vec(7f), "", ui = true)
    }
    val items = Array<Item>(SLOTS) {
        object : Item {
            override var material = MATERIAL_AIR
            override var amount = 0
            override var left = EMPTY_USE
            override var right = EMPTY_USE
        }
    }
    val handText = alphabet.text(shader, vec(0f), vec(7f), "", ui = true)
    return object : Inventory, Mesh by mesh {
        override val items = items
        override var hand = materialItem(MATERIAL_AIR)
        override var selected: Int
            get() = selected
            set(value) {
                slots[selected].sprite = UNSELECTED_SPRITE
                selected = value
                slots[value].sprite = SELECTED_SPRITE
            }
        override val tick: Tick = {
            mesh.clear(0 until limit)
            inventory.tick(this)
            slots.forEach { it.tick(this) }
            mesh.copy(inventory.data, 0, 0, 9)
            slots.forEachIndexed { index, slot ->
                mesh.copy(slot.data, 0, 9 + index * 9, 9)
            }
            items.forEachIndexed { index, item ->
//                println("${index}: ${materialName(item.material)}")
                if (item.material != MATERIAL_AIR) {
                    text[index].value = item.amount.toString()
                    text[index].tick(this)
                    mesh.bounds(9 + SLOTS * 9 + index * 5, center[index], vec(slot * 2f / 3f), ui = true)
                    mesh.sprite(9 + SLOTS * 9 + index * 5, materialSprite(item.material), materialScale(item.material))
                    mesh.copy(text[index].data, 0, 9 + SLOTS * 9 + index * 5 + 1, 4)
                }
            }
            handText.position.set(uiCursor + vec(slot * 1f / 3f, -slot * 1f / 3f))
            handText.value = hand.amount.toString()
            handText.tick(this)
            if (hand.material != MATERIAL_AIR) {
                mesh.bounds(9 + SLOTS * 9 + SLOTS * 5, uiCursor, vec(slot * 2f / 3f), ui = true)
                mesh.sprite(9 + SLOTS * 9 + SLOTS * 5, materialSprite(hand.material), materialScale(hand.material))
                mesh.copy(handText.data, 0, 9 + SLOTS * 9 + SLOTS * 5 + 1, 4)
            }
        }
        override fun set(index: Int, item: Item) {
            items[index].apply {
                material = item.material
                amount = item.amount
            }
        }
        override fun get(index: Int) = items[index]
        override fun remove(index: Int, amount: Int) = items[index].remove(amount)
        init {
            slots[0].sprite = SELECTED_SPRITE
            keys(
                GLFW_KEY_1, GLFW_KEY_2, GLFW_KEY_3,
                GLFW_KEY_4, GLFW_KEY_5, GLFW_KEY_6,
                GLFW_KEY_7, GLFW_KEY_8, GLFW_KEY_9,
                action = GLFW_PRESS
            ) { key, _ ->
                this.selected = key - 49
            }
            mouse(GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS) { _, _ ->
                val index = items.indices.minBy { center[it].distance(uiCursor) }
                if (center[index].distanceX(uiCursor) > slot
                    || center[index].distanceY(uiCursor) > slot
                ) return@mouse
                if (hand.material == MATERIAL_AIR && items[index].material != MATERIAL_AIR) {
                    hand = items[index].clone()
                    items[index].clear()
                } else if (hand.material != MATERIAL_AIR &&
                    (items[index].material == MATERIAL_AIR || items[index].material == hand.material)
                ) {
                    val before = items[index].amount
                    items[index].material = hand.material
                    items[index].amount = min(hand.amount + items[index].amount, MAX_ITEM_AMOUNT)
                    hand.amount = hand.amount + (before - items[index].amount)
                    if (hand.amount == 0) hand.clear()
                }
            }

            mouse(GLFW_MOUSE_BUTTON_RIGHT, GLFW_PRESS) { _, _ ->
                val index = items.indices.minBy { center[it].distance(uiCursor) }
                if (center[index].distanceX(uiCursor) > slot
                    || center[index].distanceY(uiCursor) > slot
                ) return@mouse
                if (hand.material == MATERIAL_AIR && items[index].material != MATERIAL_AIR) {
                    if (items[index].amount == 1) {
                        hand = items[index].clone()
                        items[index].clear()
                        return@mouse
                    }
                    val before = items[index].amount
                    items[index].amount /= 2
                    hand = items[index].clone().also { it.amount = before - items[index].amount }
                    if (hand.amount == 0) hand.clear()
                } else if (hand.material != MATERIAL_AIR &&
                    (items[index].material == MATERIAL_AIR || items[index].material == hand.material) &&
                    items[index].amount != MAX_ITEM_AMOUNT
                ) {
                    items[index].material = hand.material
                    items[index].amount += 1
                    hand.amount -= 1
                    if (hand.amount == 0) hand.clear()
                }
            }
        }
    }
}