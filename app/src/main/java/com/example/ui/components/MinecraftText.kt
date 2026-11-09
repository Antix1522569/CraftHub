package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

object MinecraftColorParser {
    private val colorMap = mapOf(
        '0' to Color(0xFF000000),
        '1' to Color(0xFF0000AA),
        '2' to Color(0xFF00AA00),
        '3' to Color(0xFF00AAAA),
        '4' to Color(0xFFAA0000),
        '5' to Color(0xFFAA00AA),
        '6' to Color(0xFFFFAA00),
        '7' to Color(0xFFAAAAAA),
        '8' to Color(0xFF555555),
        '9' to Color(0xFF5555FF),
        'a' to Color(0xFF55FF55),
        'b' to Color(0xFF55FFFF),
        'c' to Color(0xFFFF5555),
        'd' to Color(0xFFFF55FF),
        'e' to Color(0xFFFFFF55),
        'f' to Color(0xFFFFFFFF)
    )

    fun parse(rawText: String, defaultColor: Color = Color(0xFFE2E8F0)): AnnotatedString {
        if (rawText.isBlank()) return AnnotatedString("")

        return buildAnnotatedString {
            var currentColor = defaultColor
            var isBold = false
            var isItalic = false
            var i = 0

            while (i < rawText.length) {
                val c = rawText[i]
                if ((c == '§' || c == '&') && i + 1 < rawText.length) {
                    val code = rawText[i + 1].lowercaseChar()
                    when {
                        colorMap.containsKey(code) -> {
                            currentColor = colorMap[code] ?: defaultColor
                            isBold = false
                            isItalic = false
                        }
                        code == 'l' -> isBold = true
                        code == 'o' -> isItalic = true
                        code == 'r' -> {
                            currentColor = defaultColor
                            isBold = false
                            isItalic = false
                        }
                    }
                    i += 2
                } else {
                    val spanStyle = SpanStyle(
                        color = currentColor,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
                    )
                    pushStyle(spanStyle)
                    append(c)
                    pop()
                    i++
                }
            }
        }
    }
}

@Composable
fun MinecraftFormattedText(
    rawText: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    val annotated = MinecraftColorParser.parse(rawText)
    Text(
        text = annotated,
        modifier = modifier,
        fontSize = fontSize,
        maxLines = maxLines
    )
}
