//package me.mason.game.components
//
//import me.mason.game.SHEET_SIZE
//import me.mason.game.uv
//import me.mason.game.vec
//import org.joml.Vector2f
//
//fun alphabet() = Array(36) {
//    val x = it % 12
//    val y = it / 12
//    entity(
//        uv(vec(x * 5, 76 + y * 5), vec(5, 5)),
//        vec(25f, 25f), vec(0f, 0f)
//    )
//}
//
//fun Array<Entity>.actorOf(char: Char): Entity = this[
//        if (char.uppercaseChar() in 65.toChar()..90.toChar()) char.uppercaseChar() - 65.toChar()
//        else char.uppercaseChar() - 48.toChar() + 26
//]
//
//fun Array<Entity>.text(position: Vector2f, scale: Vector2f, text: String): List<Entity> {
//    return List(text.length) {
//        if (text[it] == ' ') return@List null
//        val character = actorOf(text[it]).clone()
//        character.scale.x = scale.x
//        character.scale.y = scale.y
//        character.position.x = position.x + (scale.x + 2) * it
//        character.position.y = position.y
//        character
//    }.filterNotNull()
//}
