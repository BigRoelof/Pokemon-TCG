package com.example.pokemontcg.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.ui.ViewModelFactory
import com.example.pokemontcg.ui.components.CardTile
import com.example.pokemontcg.ui.components.ErrorView
import com.example.pokemontcg.ui.components.LoadingIndicator
import com.example.pokemontcg.ui.components.MessageView
import com.example.pokemontcg.ui.components.PokedexHeader
import com.example.pokemontcg.ui.theme.BallWhite
import com.example.pokemontcg.ui.theme.Ink
import com.example.pokemontcg.ui.theme.SlateText

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    factory: ViewModelFactory,
    viewModel: SearchViewModel = viewModel(factory = factory)
) {
    val query by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val savedCardIds by viewModel.savedCardIds.collectAsState()

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
        when (val state = uiState) {
            is SearchUiState.Idle -> MessageView(
                title = "Search the card database",
                message = "Type a card name, like Charizard ex or Umbreon VMAX.",
                modifier = contentModifier
            )
            is SearchUiState.Loading -> LoadingIndicator(contentModifier)
            is SearchUiState.Error -> ErrorView(
                message = state.message,
                onRetry = viewModel::retry,
                modifier = contentModifier
            )
            is SearchUiState.Success -> {
                if (state.results.isEmpty()) {
                    MessageView(
                        title = "No cards found",
                        message = "Nothing matches “${query.trim()}”. Check the spelling or use fewer words.",
                        modifier = contentModifier
                    )
                } else {
                    val layoutDirection = LocalLayoutDirection.current
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(
                            start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                            end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.results, key = { it.id }) { card ->
                            CardTile(
                                name = card.name,
                                setName = card.set?.name ?: "Unknown set",
                                number = card.number,
                                imageUrl = card.images?.small,
                                onClick = { onNavigateToDetails(card.id) },
                                onList = card.id in savedCardIds
                            )
                        }
                    }
                }
            }
        }
    }
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
