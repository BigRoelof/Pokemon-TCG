package com.example.pokemontcg.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pokemontcg.data.model.CardSort
import com.example.pokemontcg.ui.components.CardTile
import com.example.pokemontcg.ui.components.SetChip
import com.example.pokemontcg.ui.formatEuro
import com.example.pokemontcg.ui.theme.BallWhite

/**
 * The grid shared by the collection and the chase list: set chip and sort menu on top, then
 * the cards. With [onCatch] each tile gets a ball button that catches the card.
 */
@Composable
fun CardList(
    state: CardListState,
    innerPadding: PaddingValues,
    onSortSelected: (CardSort) -> Unit,
    onOpenSetPicker: () -> Unit,
    onClearSet: () -> Unit,
    onCardClick: (String) -> Unit,
    onCatch: ((String) -> Unit)? = null
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        // Insets go into contentPadding so cards scroll behind the bottom bar;
        // the extra bottom space keeps the last row clear of the FAB.
        contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
            end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
            top = innerPadding.calculateTopPadding() + 4.dp,
            bottom = innerPadding.calculateBottomPadding() + 88.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // A set filter is only worth offering once the list spans more than one set
                Box(modifier = Modifier.weight(1f)) {
                    if (state.sets.size > 1 || state.setName != null) {
                        SetChip(
                            selectedSet = state.sets.firstOrNull { it.name == state.setName },
                            onOpenPicker = onOpenSetPicker,
                            onClear = onClearSet,
                            onHeader = false
                        )
                    }
                }
                SortMenu(selected = state.sort, onSortSelected = onSortSelected)
            }
        }
        items(state.cards, key = { it.id }) { card ->
            CardTile(
                name = card.name,
                setName = card.setName,
                number = card.number,
                imageUrl = card.imageUrl,
                onClick = { onCardClick(card.id) },
                onCaughtChange = onCatch?.let { catch -> { caught -> if (caught) catch(card.id) } },
                price = state.prices[card.id]?.let(::formatEuro),
                modifier = Modifier.animateItem()
            )
        }
    }
}

/** White text lines under a list screen's title, in the red header. */
@Composable
fun HeaderLines(vararg lines: String?) {
    lines.filterNotNull().forEachIndexed { index, line ->
        Text(
            text = line,
            style = if (index == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = BallWhite
        )
    }
}

/** "You own 45 of 230", the line that goes with the set-completion band. */
fun SetCompletion.ownedLine() = "You own $owned of $total"

/** Price total as a header line, or null when no card in view has a price. */
fun valueLine(value: Double, suffix: String): String? = if (value > 0) "${formatEuro(value)} $suffix" else null

@Composable
private fun SortMenu(selected: CardSort, onSortSelected: (CardSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = "Sort by: ${selected.label}" }
        ) {
            Text(selected.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CardSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label) },
                    onClick = {
                        onSortSelected(sort)
                        expanded = false
                    },
                    trailingIcon = if (sort == selected) {
                        { Icon(Icons.Default.Check, contentDescription = "Selected") }
                    } else {
                        null
                    }
                )
            }
        }
    }
}
