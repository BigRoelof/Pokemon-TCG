package com.example.pokemontcg.ui.chase

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.ui.components.LoadingIndicator
import com.example.pokemontcg.ui.components.MessageView
import com.example.pokemontcg.ui.components.SetPickerSheet
import com.example.pokemontcg.ui.components.TopLevelSection
import com.example.pokemontcg.ui.lists.CardList
import com.example.pokemontcg.ui.lists.HeaderLines
import com.example.pokemontcg.ui.lists.ListScreenScaffold
import com.example.pokemontcg.ui.lists.ownedLine
import com.example.pokemontcg.ui.lists.valueLine

@Composable
fun ChaseListScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onSelectSection: (TopLevelSection) -> Unit,
    factory: ViewModelProvider.Factory,
    viewModel: ChaseListViewModel = viewModel(factory = factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSetPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.caughtEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = "${event.original.name} is now in your collection",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoCatch(event)
        }
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
            allSetsDetail = "Your whole chase list",
            countLabel = { count -> if (count == 1) "1 card to catch" else "$count cards to catch" }
        )
    }

    ListScreenScaffold(
        title = "Chase list",
        section = TopLevelSection.CHASE,
        onSelectSection = onSelectSection,
        onAddCards = onNavigateToSearch,
        showAddButton = current != null && current.totalCount > 0,
        progress = current?.setCompletion?.fraction,
        snackbarHostState = snackbarHostState,
        headerContent = {
            if (current != null && current.totalCount > 0) {
                val count = current.cards.size
                HeaderLines(
                    "$count ${if (count == 1) "card" else "cards"} to catch" + current.setName?.let { " in $it" }.orEmpty(),
                    current.setCompletion?.ownedLine(),
                    valueLine(current.value, "still to chase on Cardmarket")
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
            current.totalCount == 0 -> MessageView(
                title = "Nothing to chase",
                message = "Search for the cards you're hunting and add them to your chase list.",
                actionLabel = "Add cards",
                onAction = onNavigateToSearch,
                modifier = contentModifier
            )
            else -> CardList(
                state = current,
                innerPadding = innerPadding,
                onSortSelected = viewModel::onSortSelected,
                onOpenSetPicker = { showSetPicker = true },
                onClearSet = { viewModel.onSetSelected(null) },
                onCardClick = onNavigateToDetails,
                onCatch = viewModel::catchCard
            )
        }
    }
}
