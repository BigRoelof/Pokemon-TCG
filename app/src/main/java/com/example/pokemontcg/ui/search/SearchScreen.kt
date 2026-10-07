package com.example.pokemontcg.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.data.catalog.SyncState
import com.example.pokemontcg.data.model.CardType
import com.example.pokemontcg.ui.components.CardTile
import com.example.pokemontcg.ui.components.ErrorView
import com.example.pokemontcg.ui.components.HeaderSearchField
import com.example.pokemontcg.ui.components.MessageView
import com.example.pokemontcg.ui.components.OptionPickerSheet
import com.example.pokemontcg.ui.components.PickerChip
import com.example.pokemontcg.ui.components.PickerOption
import com.example.pokemontcg.ui.components.PokedexHeader
import com.example.pokemontcg.ui.components.SetChip
import com.example.pokemontcg.ui.components.SetPickerSheet
import com.example.pokemontcg.ui.components.TypeDot
import com.example.pokemontcg.ui.toUserMessage

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    factory: ViewModelProvider.Factory,
    viewModel: SearchViewModel = viewModel(factory = factory)
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalogStatus.collectAsStateWithLifecycle()
    val trackedCards by viewModel.trackedCards.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val selectedSet by viewModel.selectedSet.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedRarity by viewModel.selectedRarity.collectAsStateWithLifecycle()
    val rarities by viewModel.rarities.collectAsStateWithLifecycle()
    // Which filter sheet is open, if any
    var openPicker by rememberSaveable { mutableStateOf<FilterPicker?>(null) }

    when (openPicker) {
        FilterPicker.SET -> SetPickerSheet(
            sets = sets,
            selectedSetId = selectedSet?.id,
            onSetSelected = { setId ->
                viewModel.onSetSelected(setId)
                openPicker = null
            },
            onDismiss = { openPicker = null }
        )
        FilterPicker.TYPE -> OptionPickerSheet(
            title = "Choose a type",
            anyLabel = "Any type",
            options = CardType.entries.map { type ->
                PickerOption(key = type.name, label = type.label, leading = { TypeDot(type) })
            },
            selectedKey = selectedType?.name,
            onSelected = { key ->
                viewModel.onTypeSelected(CardType.entries.firstOrNull { it.name == key })
                openPicker = null
            },
            onDismiss = { openPicker = null }
        )
        FilterPicker.RARITY -> OptionPickerSheet(
            title = "Choose a rarity",
            anyLabel = "Any rarity",
            options = rarities.map { PickerOption(key = it.name, label = it.name, detail = "${"%,d".format(it.count)} cards") },
            selectedKey = selectedRarity,
            onSelected = { rarity ->
                viewModel.onRaritySelected(rarity)
                openPicker = null
            },
            onDismiss = { openPicker = null }
        )
        null -> Unit
    }

    Scaffold(
        // safeDrawing includes the keyboard, so results stay scrollable above it
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            PokedexHeader(title = "Find cards", onNavigateBack = onNavigateBack) {
                HeaderSearchField(
                    query = query,
                    placeholder = "Card name",
                    onQueryChange = viewModel::onQueryChanged,
                    autoFocus = !viewModel.openedForSet
                )
                if (sets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Scrolls sideways when the chosen filters don't fit
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SetChip(
                            selectedSet = selectedSet,
                            onOpenPicker = { openPicker = FilterPicker.SET },
                            onClear = { viewModel.onSetSelected(null) }
                        )
                        PickerChip(
                            placeholder = "Any type",
                            selectedLabel = selectedType?.label,
                            onOpen = { openPicker = FilterPicker.TYPE },
                            onClear = { viewModel.onTypeSelected(null) },
                            clearDescription = "Show every type",
                            leading = selectedType?.let { type -> { TypeDot(type) } }
                        )
                        PickerChip(
                            placeholder = "Any rarity",
                            selectedLabel = selectedRarity,
                            onOpen = { openPicker = FilterPicker.RARITY },
                            onClear = { viewModel.onRaritySelected(null) },
                            clearDescription = "Show every rarity"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        val sync = catalog.sync
        when {
            // Nothing to search until the first download has brought in some cards
            catalog.cardCount == 0 && sync is SyncState.Failed -> ErrorView(
                message = sync.error.toUserMessage(),
                onRetry = viewModel::retrySync,
                modifier = contentModifier
            )
            catalog.cardCount == 0 -> CatalogDownloadView(sync = sync, modifier = contentModifier)
            // Bottom inset goes into the grid's contentPadding so results scroll behind the nav bar
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                    )
                    .consumeWindowInsets(innerPadding)
            ) {
                if (sync is SyncState.Running && sync.total > 0) {
                    CatalogUpdateBanner(done = sync.done, total = sync.total)
                }
                SearchContent(
                    state = uiState,
                    cardCount = catalog.cardCount,
                    trackedCards = trackedCards,
                    onCardClick = onNavigateToDetails,
                    bottomPadding = innerPadding.calculateBottomPadding()
                )
            }
        }
    }
}

@Composable
private fun SearchContent(
    state: SearchUiState,
    cardCount: Int,
    trackedCards: Map<String, Boolean>,
    onCardClick: (String) -> Unit,
    bottomPadding: Dp
) {
    when (state) {
        is SearchUiState.Idle -> MessageView(
            title = "Search the card database",
            message = "Look up any of ${"%,d".format(cardCount)} cards by name. Add a set name or code " +
                "to narrow it down, like charizard 151 or charizard OBF, or browse by set, type or rarity above.",
            modifier = Modifier.padding(bottom = bottomPadding)
        )
        is SearchUiState.Success -> {
            if (state.results.isEmpty()) {
                MessageView(
                    title = "No cards found",
                    message = when {
                        state.query.isBlank() -> "No cards match these filters. Try removing one."
                        !state.filters.isEmpty -> "Nothing matches \u201c${state.query.trim()}\u201d with these " +
                            "filters. Check the spelling or remove a filter."
                        else -> "Nothing matches \u201c${state.query.trim()}\u201d. Check the spelling or use fewer words."
                    },
                    modifier = Modifier.padding(bottom = bottomPadding)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = bottomPadding + 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!state.filters.isEmpty || state.isCapped) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ResultSummary(state)
                        }
                    }
                    items(state.results, key = { it.id }) { card ->
                        CardTile(
                            name = card.name,
                            setName = card.setName,
                            number = card.number,
                            imageUrl = card.imageSmall,
                            onClick = { onCardClick(card.id) },
                            tag = when (trackedCards[card.id]) {
                                true -> "In collection"
                                false -> "On chase list"
                                null -> null
                            }
                        )
                    }
                }
            }
        }
    }
}

/** First launch: the catalog is empty until the first sets are downloaded. */
@Composable
private fun CatalogDownloadView(sync: SyncState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Downloading the card database",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This happens once and takes about a minute. After that, search works offline.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (sync is SyncState.Running && sync.total > 0) {
            LinearProgressIndicator(
                progress = { sync.done.toFloat() / sync.total },
                modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth()
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth())
        }
    }
}

@Composable
private fun CatalogUpdateBanner(done: Int, total: Int) {
    Text(
        text = "Updating card database: $done of $total sets",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

private enum class FilterPicker { SET, TYPE, RARITY }

/** "237 cards in Evolving Skies", or a note that only the first results are shown. */
@Composable
private fun ResultSummary(state: SearchUiState.Success) {
    val count = state.results.size
    val inSet = state.set?.let { " in ${it.name}" }.orEmpty()
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = if (state.isCapped) "First $count cards$inSet" else "$count ${if (count == 1) "card" else "cards"}$inSet",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.isCapped) {
            Text(
                text = "Add words or filters to narrow it down.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

