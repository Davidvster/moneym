package com.dv.moneym.feature.overview.a2ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmMoney
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_ai_widget_invalid_body
import moneym.feature.overview.generated.resources.overview_ai_widget_invalid_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OverviewA2UiWidgetCard(
    json: String,
    context: OverviewWidgetContext,
    modifier: Modifier = Modifier,
) {
    when (val result = OverviewA2UiValidator(context.allowedBindings).validate(json)) {
        is OverviewA2UiValidationResult.Valid -> OverviewA2UiNodeRenderer(
            node = result.node,
            context = context,
            modifier = modifier,
            insideCard = false,
        )

        is OverviewA2UiValidationResult.Invalid -> OverviewInvalidAiWidgetCard(modifier)
    }
}

@Composable
private fun OverviewA2UiNodeRenderer(
    node: OverviewA2UiNode,
    context: OverviewWidgetContext,
    modifier: Modifier = Modifier,
    insideCard: Boolean,
) {
    when (node.type) {
        "card" -> {
            if (insideCard) {
                OverviewA2UiColumn(node, context, modifier)
            } else {
                MmCard(modifier = modifier, padded = true, shape = MM.dimen.radius_2x) {
                    OverviewA2UiColumn(node, context, Modifier.fillMaxWidth())
                }
            }
        }

        "column" -> OverviewA2UiColumn(node, context, modifier)
        "row" -> OverviewA2UiRow(node, context, modifier)
        "text" -> OverviewA2UiText(node, context, modifier)
        "moneyMetric" -> OverviewA2UiMoneyMetric(node, context, modifier)
        "percentage" -> OverviewA2UiPercentage(node, context, modifier)
        "progress" -> OverviewA2UiProgress(node, context, modifier)
        "barList" -> OverviewA2UiList(node, context, modifier, showBars = true)
        "categoryList" -> OverviewA2UiList(node, context, modifier, showBars = false)
        "spacer" -> Spacer(modifier.height(((node.value ?: 12.0).coerceIn(4.0, 32.0)).dp))
        "divider" -> HorizontalDivider(modifier = modifier, color = MM.colors.border)
    }
}

@Composable
private fun OverviewA2UiColumn(
    node: OverviewA2UiNode,
    context: OverviewWidgetContext,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x)) {
        node.title?.let { OverviewA2UiTitle(it) }
        node.children.forEach { child ->
            OverviewA2UiNodeRenderer(child, context, Modifier.fillMaxWidth(), insideCard = node.type == "card")
        }
    }
}

@Composable
private fun OverviewA2UiRow(
    node: OverviewA2UiNode,
    context: OverviewWidgetContext,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        node.children.forEach { child ->
            OverviewA2UiNodeRenderer(child, context, Modifier.weight(1f), insideCard = true)
        }
    }
}

@Composable
private fun OverviewA2UiTitle(text: String) {
    Text(
        text = text,
        style = MM.type.title3,
        color = MM.colors.text,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun OverviewA2UiText(
    node: OverviewA2UiNode,
    context: OverviewWidgetContext,
    modifier: Modifier = Modifier,
) {
    val text = node.binding?.let { context.labels[it] } ?: node.text.orEmpty()
    Text(
        text = text,
        modifier = modifier,
        style = MM.type.body,
        color = MM.colors.text2,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun OverviewA2UiMoneyMetric(
    node: OverviewA2UiNode,
    context: OverviewWidgetContext,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        node.label?.let {
            Text(text = it, style = MM.type.caption, color = MM.colors.text2)
        }
        MmMoney(
            value = node.binding?.let { context.moneyValues[it] } ?: node.value ?: 0.0,
            currency = context.currencyCode,
            size = 22.sp,
            weight = FontWeight.SemiBold,
            color = MM.colors.text,
        )
    }
}

@Composable
private fun OverviewA2UiPercentage(
    node: OverviewA2UiNode,
    context: OverviewWidgetContext,
    modifier: Modifier = Modifier,
) {
    val value = node.binding?.let { context.numberValues[it] } ?: node.value ?: 0.0
    Text(
        text = "${(value.coerceIn(0.0, 1.0) * 100).toInt()}%",
        modifier = modifier,
        style = MM.type.title3,
        color = MM.colors.text,
    )
}

@Composable
private fun OverviewA2UiProgress(
    node: OverviewA2UiNode,
    context: OverviewWidgetContext,
    modifier: Modifier = Modifier,
) {
    val value = (node.binding?.let { context.numberValues[it] } ?: node.value ?: 0.0).coerceIn(0.0, 1.0)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_0_5x)) {
        node.label?.let { Text(text = it, style = MM.type.caption, color = MM.colors.text2) }
        LinearProgressIndicator(
            progress = { value.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(MM.dimen.radius_1x),
            color = MM.colors.accent,
            trackColor = MM.colors.border,
        )
    }
}

@Composable
private fun OverviewA2UiList(
    node: OverviewA2UiNode,
    context: OverviewWidgetContext,
    modifier: Modifier = Modifier,
    showBars: Boolean,
) {
    val items = node.binding?.let { context.listValues[it] }.orEmpty()
        .take(node.limit ?: OverviewA2UiValidator.MAX_LIST_ITEMS)
    val max = items.maxOfOrNull { kotlin.math.abs(it.value) } ?: 0.0
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)) {
        node.title?.let { OverviewA2UiTitle(it) }
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.label,
                    modifier = Modifier.weight(1f),
                    style = MM.type.body,
                    color = MM.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(MM.dimen.padding_1x))
                MmMoney(
                    value = item.value,
                    currency = context.currencyCode,
                    size = 14.sp,
                    weight = FontWeight.SemiBold,
                    color = MM.colors.text,
                )
            }
            if (showBars && max > 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(MM.dimen.radius_1x)
                        .background(MM.colors.border),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((kotlin.math.abs(item.value) / max).toFloat().coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(MM.dimen.radius_1x)
                            .background(MM.colors.accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewInvalidAiWidgetCard(modifier: Modifier = Modifier) {
    MmCard(modifier = modifier, padded = true, shape = MM.dimen.radius_2x) {
        Column(verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)) {
            Text(
                text = stringResource(Res.string.overview_ai_widget_invalid_title),
                style = MM.type.title3,
                color = MM.colors.text,
            )
            Text(
                text = stringResource(Res.string.overview_ai_widget_invalid_body),
                style = MM.type.body,
                color = MM.colors.text2,
            )
        }
    }
}

@Preview
@Composable
private fun OverviewA2UiWidgetCardPreview_Light() {
    MoneyMTheme(darkTheme = false) {
        OverviewA2UiWidgetCard(
            json = sampleA2UiJson,
            context = BuildOverviewWidgetContextUseCase.sample(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun OverviewA2UiWidgetCardPreview_Dark() {
    MoneyMTheme(darkTheme = true) {
        OverviewA2UiWidgetCard(
            json = sampleA2UiJson,
            context = BuildOverviewWidgetContextUseCase.sample(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun OverviewA2UiInvalidWidgetCardPreview_Light() {
    MoneyMTheme(darkTheme = false) {
        OverviewA2UiWidgetCard(
            json = """{"type":"script","code":"bad"}""",
            context = BuildOverviewWidgetContextUseCase.sample(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun OverviewA2UiInvalidWidgetCardPreview_Dark() {
    MoneyMTheme(darkTheme = true) {
        OverviewA2UiWidgetCard(
            json = """{"type":"script","code":"bad"}""",
            context = BuildOverviewWidgetContextUseCase.sample(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

internal val sampleA2UiJson = """
{
  "type": "card",
  "title": "Cash flow",
  "children": [
    {
      "type": "row",
      "children": [
        {"type": "moneyMetric", "label": "Income", "binding": "income"},
        {"type": "moneyMetric", "label": "Expenses", "binding": "expenses"}
      ]
    },
    {"type": "progress", "label": "Budget used", "binding": "budget.progressAverage"},
    {"type": "barList", "title": "Top expenses", "binding": "category.expenseBreakdown", "limit": 3}
  ]
}
""".trimIndent()
