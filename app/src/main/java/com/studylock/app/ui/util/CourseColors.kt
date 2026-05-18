package com.studylock.app.ui.util

import androidx.compose.ui.graphics.Color

object CourseColors {
    
    val PALETTE = listOf(
        Color(0xFFE57373),
        Color(0xFFFF8A65),
        Color(0xFFFFB74D),
        Color(0xFFFFD54F),
        Color(0xFF81C784),
        Color(0xFF4DB6AC),
        Color(0xFF64B5F6),
        Color(0xFF7986CB),
        Color(0xFFBA68C8),
        Color(0xFFF06292)
    )
    
    val PALETTE_HEX = listOf(
        "#FFE57373",
        "#FFFF8A65",
        "#FFFFB74D",
        "#FFFFD54F",
        "#FF81C784",
        "#FF4DB6AC",
        "#FF64B5F6",
        "#FF7986CB",
        "#FFBA68C8",
        "#FFF06292"
    )
    
    fun getColorFromHex(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            PALETTE[0]
        }
    }
    
    fun getHexFromIndex(index: Int): String {
        return PALETTE_HEX.getOrElse(index) { PALETTE_HEX[0] }
    }
    
    fun getColorFromIndex(index: Int): Color {
        return PALETTE.getOrElse(index) { PALETTE[0] }
    }
    
    fun getOnColor(color: Color): Color {
        val r = color.red
        val g = color.green
        val b = color.blue
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return if (luminance > 0.5f) Color(0xFF1C1B1F) else Color.White
    }
}
