package com.example.pokemontcg.ui.binders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.data.model.TrackedCard
import com.example.pokemontcg.ui.components.CardCornerShape
import com.example.pokemontcg.ui.components.CardImage
import com.example.pokemontcg.ui.components.HeaderSearchField
import com.example.pokemontcg.ui.components.LoadingIndicator
import com.example.pokemontcg.ui.components.MessageView
import com.example.pokemontcg.ui.components.PokedexHeader
import com.example.pokemontcg.ui.components.SetChip
import com.example.pokemontcg.ui.components.SetPickerSheet
import com.example.pokemontcg.ui.theme.BallWhite
import com.example.pokemontcg.ui.theme.Ink

/** Pick collection cards to put in a binder. */
@Composable
fun BinderPickerScreen(
    onNavigateBack: () -> Unit,
    factory: ViewModelProvider.Factory,
    viewModel: BinderPickerViewModel = viewModel(factory = factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    var showSetPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.done.collect { onNavigateBack() }
    }

    val current = state
    if (showSetPicker && current != null) {
        SetPickerSheet(
            sets = current.sets,
            selectedSetId = current.setName,
            onSetSelected = { name ->
                viewModel.onSetSelected(name)
                showSetPicker = false
            },
            onDismiss = { showSetPicker = false },
            title = "Show one set",
            allSetsDetail = "Your whole collection",
            countLabel = { count -> if (count == 1) "1 card owned" else "$count cards owned" }
        )
    }

    Scaffold(
        // safeDrawing includes the keyboard, so cards stay scrollable above it
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            PokedexHeader(
                title = current?.binderName?.takeIf { it.isNotEmpty() }?.let { "Add to $it" } ?: "Add cards",
                onNavigateBack = onNavigateBack
            ) {
                if (current != null && current.collectionSize > 0) {
                    HeaderSearchField(query = query, onQueryChange = viewModel::onQueryChanged, placeholder = "Name, set or number")
                    if (current.sets.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SetChip(
                            selectedSet = current.sets.firstOrNull { it.name == current.setName },
                            onOpenPicker = { showSetPicker = true },
                            onClear = { viewModel.onSetSelected(null) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selected.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::addSelected,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (selected.size == 1) "Add 1 card" else "Add ${selected.size} cards") },
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
        when {
            current == null -> LoadingIndicator(contentModifier)
            current.collectionSize == 0 -> MessageView(
                title = "Your collection is empty",
                message = "Binders hold cards you own. Add cards to your collection first, then put them in a binder.",
                modifier = contentModifier
            )
            current.cards.isEmpty() -> MessageView(
                title = "No cards found",
                message = "None of your cards match “${query.trim()}”.",
                modifier = contentModifier
            )
            else -> {
                val layoutDirection = LocalLayoutDirection.current
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    contentPadding = PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                        end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                        top = innerPadding.calculateTopPadding() + 12.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(current.cards, key = { it.id }) { card ->
                        val order = selected.indexOf(card.id)
                        PickTile(
                            card = card,
                            order = order.takeIf { it >= 0 }?.plus(1),
                            inBinder = card.id in current.inBinder,
                            onToggle = { viewModel.toggle(card.id) }
                        )
                    }
                }
            }
        }
    }
}

/** A collection card to pick; [order] numbers picked cards in the order they'll fill the pockets. */
@Composable
private fun PickTile(card: TrackedCard, order: Int?, inBinder: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .toggleable(value = order != null, enabled = !inBinder, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics(mergeDescendants = true) {
                contentDescription = "${card.name}, ${card.setName} #${card.number}" + if (inBinder) ", already in this binder" else ""
            }
            .padding(2.dp)
    ) {
        Box {
            CardImage(
                imageUrl = card.imageUrl,
                contentDescription = card.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (inBinder) 0.4f else 1f)
                    .then(if (order != null) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CardCornerShape) else Modifier)
            )
            when {
                order != null -> Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(26.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$order", style = MaterialTheme.typography.labelLarge, color = BallWhite)
                }
                inBinder -> Text(
                    text = "In binder",
                    style = MaterialTheme.typography.labelSmall,
                    color = BallWhite,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(Ink, RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = card.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${card.setName} #${card.number}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
