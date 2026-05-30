package com.dv.moneym.feature.infopage

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val colors = MM.colors
    val annotated = remember(html) { parseBasicHtml(html) }
    Text(
        text = annotated,
        style = MM.type.body,
        color = colors.text,
        modifier = modifier,
    )
}

private fun parseBasicHtml(html: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val src = html.trim()

    while (i < src.length) {
        if (src[i] == '<') {
            val end = src.indexOf('>', i)
            if (end == -1) { append(src[i]); i++; continue }
            val tag = src.substring(i + 1, end).trim().lowercase()
            i = end + 1
            when {
                tag == "br" || tag == "br/" || tag == "/p" -> append('\n')
                tag == "p" -> { /* paragraph start — handled by /p */ }
                tag == "h2" -> {
                    append('\n')
                    val closeH2 = src.indexOf("</h2>", i, ignoreCase = true)
                    if (closeH2 != -1) {
                        val headingText = src.substring(i, closeH2)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                        append(headingText)
                        pop()
                        append('\n')
                        i = closeH2 + 5
                    }
                }
                tag == "/h2" -> { /* already consumed by h2 handler */ }
                tag == "b" -> pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                tag == "/b" -> { try { pop() } catch (_: Throwable) {} }
                tag == "ul" -> { /* list start */ }
                tag == "/ul" -> { append('\n') }
                tag == "li" -> append("\n• ")
                tag == "/li" -> { /* nothing */ }
            }
        } else {
            append(src[i])
            i++
        }
    }
}
