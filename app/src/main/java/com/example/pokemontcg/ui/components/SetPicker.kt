package com.example.pokemontcg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.ui.theme.BallWhite
import com.example.pokemontcg.ui.theme.Ink

/**
 * "All sets", or the chosen set with a button to clear it. [onHeader] styles it for the red
 * header (Search); otherwise for the page background (Home).
 */
@Composable
fun SetChip(
    selectedSet: CardSetWithCount?,
    onOpenPicker: () -> Unit,
    onClear: () -> Unit,
    onHeader: Boolean = true
) {
    val shape = RoundedCornerShape(50)
    val outlineColor = if (onHeader) BallWhite else MaterialTheme.colorScheme.outline
    val labelColor = if (onHeader) BallWhite else MaterialTheme.colorScheme.onSurfaceVariant
    val selectedContainer = if (onHeader) BallWhite else MaterialTheme.colorScheme.secondary
    val selectedContent = if (onHeader) Ink else MaterialTheme.colorScheme.onSecondary
    Row(
        modifier = Modifier
            .clip(shape)
            .then(
                if (selectedSet != null) Modifier.background(selectedContainer, shape)
                else Modifier.border(1.5.dp, outlineColor, shape)
            )
            .clickable(role = Role.Button, onClick = onOpenPicker)
            .heightIn(min = 36.dp)
            .padding(start = 14.dp, end = if (selectedSet != null) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedSet == null) {
            Text("All sets", style = MaterialTheme.typography.labelLarge, color = labelColor)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = labelColor)
        } else {
            SetSymbol(selectedSet.symbolUrl, size = 18)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedSet.name,
                style = MaterialTheme.typography.labelLarge,
                color = selectedContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shape)
                    .clickable(role = Role.Button, onClick = onClear)
                    .semantics { contentDescription = "Show all sets" },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = selectedContent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPickerSheet(
    sets: List<CardSetWithCount>,
    selectedSetId: String?,
    onSetSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Choose a set",
    allSetsDetail: String = "Search every card",
    countLabel: (Int) -> String = { "$it cards" }
) {
    var filter by rememberSaveable { mutableStateOf("") }
    val visibleSets = remember(sets, filter) { filterSets(sets, filter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.9f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text("Set name or code, like 151 or OBF") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyColumn(modifier = Modifier.navigationBarsPadding()) {
                if (filter.isBlank()) {
                    item(key = "all") {
                        SetRow(
                            name = "All sets",
                            detail = allSetsDetail,
                            symbolUrl = null,
                            selected = selectedSetId == null,
                            onClick = { onSetSelected(null) }
                        )
                    }
                }
                items(visibleSets, key = { it.id }) { set ->
                    SetRow(
                        name = set.name,
                        detail = setDetail(set, countLabel),
                        symbolUrl = set.symbolUrl,
                        selected = set.id == selectedSetId,
                        onClick = { onSetSelected(set.id) }
                    )
                }
                if (visibleSets.isEmpty() && filter.isNotBlank()) {
                    item(key = "none") {
                        Text(
                            text = "No sets match “${filter.trim()}”.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetRow(
    name: String,
    detail: String,
    symbolUrl: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (symbolUrl != null) SetSymbol(symbolUrl, size = 28)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SetSymbol(url: String?, size: Int) {
    AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(size.dp))
}

/** "Scarlet & Violet, 2023, OBF, 230 cards" with the parts the data has. */
private fun setDetail(set: CardSetWithCount, countLabel: (Int) -> String): String = listOfNotNull(
    set.series,
    set.releaseDate?.take(4),
    set.ptcgoCode,
    countLabel(set.cardCount)
).joinToString(", ")

/** Matches the start of any word in the set name or series, or the set code. */
internal fun filterSets(sets: List<CardSetWithCount>, filter: String): List<CardSetWithCount> {
    val words = filter.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return sets
    return sets.filter { set ->
        val haystack = " ${set.name} ${set.series.orEmpty()}".lowercase()
        words.all { word -> " $word" in haystack || set.ptcgoCode.equals(word, ignoreCase = true) }
    }
}
