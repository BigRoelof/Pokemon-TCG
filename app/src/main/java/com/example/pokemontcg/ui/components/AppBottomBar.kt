package com.example.pokemontcg.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/** The app's top-level sections, switched with the bottom bar. */
enum class TopLevelSection(val label: String) {
    COLLECTION("Collection"),
    CHASE("Chase list"),
    BINDERS("Binders")
}

@Composable
fun AppBottomBar(selected: TopLevelSection, onSelect: (TopLevelSection) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        TopLevelSection.entries.forEach { section ->
            val isSelected = section == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(section) },
                icon = {
                    when (section) {
                        TopLevelSection.COLLECTION -> CardStackIcon(
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TopLevelSection.CHASE -> PokeBall(
                            filled = isSelected,
                            size = 22.dp,
                            outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TopLevelSection.BINDERS -> BinderIcon(
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            filled = isSelected
                        )
                    }
                },
                label = { Text(section.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/** Two fanned cards: the collection icon. */
@Composable
private fun CardStackIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = size.minDimension * 0.09f
        val cardSize = Size(size.width * 0.5f, size.height * 0.7f)
        val corner = CornerRadius(stroke * 1.2f)
        rotate(degrees = -12f, pivot = Offset(size.width * 0.35f, size.height * 0.9f)) {
            drawRoundRect(color, Offset(size.width * 0.14f, size.height * 0.16f), cardSize, corner, style = Stroke(stroke))
        }
        rotate(degrees = 10f, pivot = Offset(size.width * 0.65f, size.height * 0.9f)) {
            drawRoundRect(color, Offset(size.width * 0.38f, size.height * 0.12f), cardSize, corner)
        }
    }
}

/** A ring binder seen from the front: cover, spine line and three rings. */
@Composable
private fun BinderIcon(color: Color, filled: Boolean) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = size.minDimension * 0.09f
        val cover = Size(size.width * 0.66f, size.height * 0.8f)
        val topLeft = Offset(size.width * 0.22f, size.height * 0.1f)
        val corner = CornerRadius(stroke * 1.2f)
        if (filled) {
            drawRoundRect(color, topLeft, cover, corner)
        } else {
            drawRoundRect(color, topLeft, cover, corner, style = Stroke(stroke))
        }
        val spineX = topLeft.x + cover.width * 0.28f
        if (!filled) drawLine(color, Offset(spineX, topLeft.y), Offset(spineX, topLeft.y + cover.height), stroke)
        // Rings stick out over the cover's left edge
        listOf(0.25f, 0.5f, 0.75f).forEach { y ->
            val ringY = topLeft.y + cover.height * y
            drawLine(color, Offset(size.width * 0.1f, ringY), Offset(topLeft.x + stroke * 2, ringY), stroke * 1.3f)
        }
    }
}
