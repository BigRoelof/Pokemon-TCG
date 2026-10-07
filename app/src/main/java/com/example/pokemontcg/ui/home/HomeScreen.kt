package com.example.pokemontcg.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.ui.components.CardTile
import com.example.pokemontcg.ui.components.ErrorView
import com.example.pokemontcg.ui.components.LoadingIndicator
import com.example.pokemontcg.ui.components.MessageView
import com.example.pokemontcg.ui.components.PokedexHeader
import com.example.pokemontcg.ui.components.SetChip
import com.example.pokemontcg.ui.components.SetPickerSheet
import com.example.pokemontcg.ui.theme.BallWhite

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    factory: ViewModelProvider.Factory,
    viewModel: HomeViewModel = viewModel(factory = factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val success = uiState as? HomeUiState.Success
    val hasCards = success != null && success.totalCount > 0
    var showSetPicker by rememberSaveable { mutableStateOf(false) }

    if (showSetPicker && success != null) {
        SetPickerSheet(
            sets = success.sets,
            selectedSetId = success.setName,
            onSetSelected = { name ->
                viewModel.onSetSelected(name)
                showSetPicker = false
            },
            onDismiss = { showSetPicker = false },
            title = "Show one set",
            allSetsDetail = "Your whole chase list",
            countLabel = { count -> if (count == 1) "1 card on your list" else "$count cards on your list" }
        )
    }

    Scaffold(
        topBar = {
            PokedexHeader(
                title = "Chase list",
                progress = success?.takeIf { hasCards }?.let { it.obtainedCount.toFloat() / it.totalCount }
            ) {
                if (success != null && hasCards) {
                    Text(
                        text = "${success.obtainedCount} of ${success.totalCount} caught" +
                            (success.setName?.let { " in $it" } ?: ""),
                        style = MaterialTheme.typography.titleMedium,
                        color = BallWhite
                    )
                }
            }
        },
        floatingActionButton = {
            if (hasCards) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToSearch,
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    text = { Text("Add cards") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        when (val state = uiState) {
            is HomeUiState.Loading -> LoadingIndicator(contentModifier)
            is HomeUiState.Error -> ErrorView(message = state.message, modifier = contentModifier)
            is HomeUiState.Success -> {
                if (state.totalCount == 0) {
                    MessageView(
                        title = "No cards yet",
                        message = "Search for the cards you're hunting and add them to your chase list.",
                        actionLabel = "Add cards",
                        onAction = onNavigateToSearch,
                        modifier = contentModifier
                    )
                } else {
                    ChaseGrid(
                        state = state,
                        innerPadding = innerPadding,
                        onFilterSelected = viewModel::onFilterSelected,
                        onOpenSetPicker = { showSetPicker = true },
                        onClearSet = { viewModel.onSetSelected(null) },
                        onCardClick = onNavigateToDetails,
                        onCaughtChange = viewModel::setObtained
                    )
                }
            }
        }
    }
}

@Composable
private fun ChaseGrid(
    state: HomeUiState.Success,
    innerPadding: PaddingValues,
    onFilterSelected: (ChaseFilter) -> Unit,
    onOpenSetPicker: () -> Unit,
    onClearSet: () -> Unit,
    onCardClick: (String) -> Unit,
    onCaughtChange: (String, Boolean) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        // Insets go into contentPadding so cards scroll behind the navigation bar;
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
        // Only worth offering once the list spans more than one set
        if (state.sets.size > 1 || state.setName != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    SetChip(
                        selectedSet = state.sets.firstOrNull { it.name == state.setName },
                        onOpenPicker = onOpenSetPicker,
                        onClear = onClearSet,
                        onHeader = false
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            FilterRow(selected = state.filter, onFilterSelected = onFilterSelected)
        }
        if (state.cards.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MessageView(
                    title = if (state.filter == ChaseFilter.OBTAINED) "Nothing caught yet" else "All caught!",
                    message = if (state.filter == ChaseFilter.OBTAINED) {
                        "Tap the ball on a card once it's in your collection."
                    } else {
                        "Every card on your list is in your collection. Time to add a new chase."
                    },
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
        items(state.cards, key = { it.id }) { card ->
            CardTile(
                name = card.name,
                setName = card.setName,
                number = card.number,
                imageUrl = card.imageUrl,
                onClick = { onCardClick(card.id) },
                caught = card.obtained,
                onCaughtChange = { caught -> onCaughtChange(card.id, caught) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun FilterRow(selected: ChaseFilter, onFilterSelected: (ChaseFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        ChaseFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = filter == selected,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }
}
