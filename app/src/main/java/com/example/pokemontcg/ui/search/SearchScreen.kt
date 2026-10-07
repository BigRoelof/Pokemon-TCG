package com.example.pokemontcg.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.data.catalog.SyncState
import com.example.pokemontcg.ui.components.CardTile
import com.example.pokemontcg.ui.components.ErrorView
import com.example.pokemontcg.ui.components.MessageView
import com.example.pokemontcg.ui.components.PokedexHeader
import com.example.pokemontcg.ui.theme.BallWhite
import com.example.pokemontcg.ui.theme.Ink
import com.example.pokemontcg.ui.theme.SlateText
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
    val savedCardIds by viewModel.savedCardIds.collectAsStateWithLifecycle()

    Scaffold(
        // safeDrawing includes the keyboard, so results stay scrollable above it
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            PokedexHeader(title = "Find cards", onNavigateBack = onNavigateBack) {
                SearchField(query = query, onQueryChange = viewModel::onQueryChanged)
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
                    savedCardIds = savedCardIds,
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
    savedCardIds: Set<String>,
    onCardClick: (String) -> Unit,
    bottomPadding: Dp
) {
    when (state) {
        is SearchUiState.Idle -> MessageView(
            title = "Search the card database",
            message = "Look up any of ${"%,d".format(cardCount)} cards by name, like Charizard ex or Umbreon VMAX.",
            modifier = Modifier.padding(bottom = bottomPadding)
        )
        is SearchUiState.Success -> {
            if (state.results.isEmpty()) {
                MessageView(
                    title = "No cards found",
                    message = "Nothing matches \u201c${state.query.trim()}\u201d. Check the spelling or use fewer words.",
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
                    items(state.results, key = { it.id }) { card ->
                        CardTile(
                            name = card.name,
                            setName = card.setName,
                            number = card.number,
                            imageUrl = card.imageSmall,
                            onClick = { onCardClick(card.id) },
                            onList = card.id in savedCardIds
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

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Open the keyboard on first entry only, not when coming back from a card
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoFocused) {
            focusRequester.requestFocus()
            autoFocused = true
        }
    }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text("Card name") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = BallWhite,
            unfocusedContainerColor = BallWhite,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            cursorColor = Ink,
            focusedLeadingIconColor = Ink,
            unfocusedLeadingIconColor = SlateText,
            focusedTrailingIconColor = Ink,
            unfocusedTrailingIconColor = SlateText,
            focusedPlaceholderColor = SlateText,
            unfocusedPlaceholderColor = SlateText,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
