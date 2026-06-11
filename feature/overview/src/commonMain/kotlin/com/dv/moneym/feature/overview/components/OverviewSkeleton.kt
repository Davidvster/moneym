package com.dv.moneym.feature.overview.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmSkeleton
import com.dv.moneym.core.ui.MmSkeletonCircle

@Composable
internal fun OverviewSkeleton(modifier: Modifier = Modifier) {
    val space = MM.dimen
    Column(modifier = modifier.fillMaxWidth()) {
        MmCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = space.padding_2x),
            padded = true,
            shape = MM.dimen.radius_2x,
        ) {
            repeat(3) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 0.dp else space.padding_2x),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MmSkeleton(modifier = Modifier.width(80.dp).height(12.dp))
                    Spacer(Modifier.weight(1f))
                    MmSkeleton(modifier = Modifier.width(96.dp).height(14.dp))
                }
            }
        }
        MmCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = space.padding_1_5x),
            padded = true,
            shape = MM.dimen.radius_2x,
        ) {
            MmSkeleton(modifier = Modifier.width(140.dp).height(12.dp))
            Spacer(Modifier.height(space.padding_2x))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MmSkeletonCircle(size = 180.dp)
            }
            Spacer(Modifier.height(space.padding_2x))
            repeat(3) {
                MmSkeleton(
                    modifier = Modifier
                        .padding(top = space.padding_1x)
                        .fillMaxWidth()
                        .height(12.dp),
                )
            }
        }
        MmCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = space.padding_2x),
            padded = true,
            shape = MM.dimen.radius_2x,
        ) {
            MmSkeleton(modifier = Modifier.width(120.dp).height(12.dp))
            Spacer(Modifier.height(space.padding_2x))
            MmSkeleton(modifier = Modifier.fillMaxWidth().height(120.dp))
        }
        Spacer(Modifier.height(space.padding_2x))
    }
}

@Preview
@Composable
private fun OverviewSkeletonPreview() {
    MoneyMTheme {
        OverviewSkeleton()
    }
}
