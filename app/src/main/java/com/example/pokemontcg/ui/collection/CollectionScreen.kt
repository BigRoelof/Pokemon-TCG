package com.example.pokemontcg.ui.collection

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.pokemontcg.ui.lists.valueLine

@Composable
fun CollectionScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onSelectSection: (TopLevelSection) -> Unit,
    factory: ViewModelProvider.Factory,
    viewModel: CollectionViewModel = viewModel(factory = factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSetPicker by rememberSaveable { mutableStateOf(false) }

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

    ListScreenScaffold(
        title = "Collection",
        section = TopLevelSection.COLLECTION,
        onSelectSection = onSelectSection,
        onAddCards = onNavigateToSearch,
        showAddButton = current != null && current.totalCount > 0,
        // With a set chosen, the band shows how complete that set is
        progress = current?.setCompletion?.fraction,
        headerContent = {
            if (current != null && current.totalCount > 0) {
                val count = current.cards.size
                val completion = current.setCompletion
                HeaderLines(
                    when {
                        completion != null -> "You own ${completion.owned} of ${completion.total} in ${completion.setName}"
                        current.setName != null -> "$count ${if (count == 1) "card" else "cards"} in ${current.setName}"
                        else -> "$count ${if (count == 1) "card" else "cards"}"
                    },
                    valueLine(current.value, "on Cardmarket")
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
                title = "Your collection is empty",
                message = "Catch cards from your chase list, or search for cards you own and add them.",
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
                onCardClick = onNavigateToDetails
            )
        }
    }
}
