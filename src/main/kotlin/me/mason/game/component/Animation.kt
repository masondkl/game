package me.mason.game.component

import me.mason.game.Window
import kotlin.time.Duration

fun Window.frame(duration: Duration, frames: Int): Int {
    val seconds = (duration.inWholeMilliseconds / 1000f)
    return ((elapsed % seconds) * frames * (1f / seconds)).toInt()
}