package com.dv.moneym.feature.infopage

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM

private const val UrlTag = "URL"
private val hrefRegex = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val colors = MM.colors
    val uriHandler = LocalUriHandler.current
    val annotated = remember(html) {
        parseBasicHtml(
            html = html,
            linkStyle = SpanStyle(
                color = colors.accent,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
    ClickableText(
        text = annotated,
        style = MM.type.body.copy(color = colors.text),
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations(UrlTag, offset, offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        },
    )
}

private fun parseBasicHtml(
    html: String,
    linkStyle: SpanStyle,
): AnnotatedString = buildAnnotatedString {
    var i = 0
    val src = html.trim()

    while (i < src.length) {
        if (src[i] == '<') {
            val end = src.indexOf('>', i)
            if (end == -1) {
                append(src[i])
                i++
                continue
            }
            val rawTag = src.substring(i + 1, end).trim()
            val tag = rawTag.lowercase()
            i = end + 1
            when {
                tag == "br" || tag == "br/" || tag == "/p" -> append('\n')
                tag == "p" -> Unit
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
                tag == "/h2" -> Unit
                tag == "b" -> pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                tag == "/b" -> runCatching { pop() }
                tag == "a" || tag.startsWith("a ") -> {
                    val href = hrefRegex.find(rawTag)?.groupValues?.getOrNull(1)
                    if (href != null) {
                        pushStringAnnotation(tag = UrlTag, annotation = href)
                        pushStyle(linkStyle)
                    }
                }
                tag == "/a" -> {
                    runCatching { pop() }
                    runCatching { pop() }
                }
                tag == "ul" -> Unit
                tag == "/ul" -> append('\n')
                tag == "li" -> append("\n• ")
                tag == "/li" -> Unit
            }
        } else {
            append(src[i])
            i++
        }
    }
}
