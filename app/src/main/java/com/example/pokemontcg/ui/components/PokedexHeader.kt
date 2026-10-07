package com.example.pokemontcg.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.pokemontcg.ui.theme.BallWhite
import com.example.pokemontcg.ui.theme.CaughtYellow
import com.example.pokemontcg.ui.theme.Ink

private val BandHeight = 14.dp
private val KnobSize = 40.dp

/**
 * The red top half of a Poké Ball: a header that runs up behind the status bar and ends in the
 * ball's dark band. When [progress] is set, the band doubles as a progress bar and the ball's
 * button slides along it; otherwise the button sits in the middle.
 */
@Composable
fun PokedexHeader(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    progress: Float? = null,
    /** Icon buttons at the end of the title row. */
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    // Short windows (phones in landscape): the content goes beside the title to save height
    val compact = isCompactHeight()
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = if (compact) 0.dp else 8.dp, bottom = if (compact) 8.dp else 12.dp)
        ) {
            val backButton = @Composable {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.offset(x = (-12).dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BallWhite
                        )
                    }
                }
            }
            val titleMaxWidth = maxWidth * 0.4f
            if (compact) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    backButton()
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = BallWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = titleMaxWidth)
                            .padding(end = 24.dp, top = 4.dp, bottom = 4.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) { content() }
                    actions()
                }
            } else {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        backButton()
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = BallWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp)
                        )
                        actions()
                    }
                    content()
                }
            }
        }
        BallBand(progress = progress)
    }
}

/** Whether the window is short (a phone in landscape): screens then save height where they can. */
@Composable
fun isCompactHeight(): Boolean =
    with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height < CompactHeight.roundToPx() }

private val CompactHeight = 480.dp

@Composable
private fun BallBand(progress: Float?) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0.5f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 120f),
        label = "bandProgress"
    )
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(KnobSize)
            .then(
                if (progress != null) {
                    Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
                } else {
                    Modifier.clearAndSetSemantics { }
                }
            )
    ) {
        // Red continues down to the middle of the band, so the band reads as the ball's equator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(KnobSize / 2)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(BandHeight)
                .background(Ink)
        ) {
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(CaughtYellow)
                )
            }
        }
        val knobInset = 16.dp
        val travel = maxWidth - KnobSize - knobInset * 2
        Box(
            modifier = Modifier
                // Lambda offset: the knob moves every animation frame without recomposing
                .offset { IntOffset((knobInset + travel * animatedProgress).roundToPx(), 0) }
                .size(KnobSize)
                .background(BallWhite, CircleShape)
                .border(5.dp, Ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .border(2.5.dp, Ink, CircleShape)
            )
        }
    }
}
