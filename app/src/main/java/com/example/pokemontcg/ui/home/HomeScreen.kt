package com.example.pokemontcg.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.ui.ViewModelFactory
import com.example.pokemontcg.ui.components.ErrorView
import com.example.pokemontcg.ui.components.LoadingIndicator
import com.example.pokemontcg.ui.components.PokemonCardRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    factory: ViewModelFactory,
    viewModel: HomeViewModel = viewModel(factory = factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Chase List") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToSearch) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search Cards")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LoadingIndicator()
                }
                is HomeUiState.Error -> {
                    ErrorView(message = state.message)
                }
                is HomeUiState.Success -> {
                    if (state.totalCount == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Your chase list is empty.\nTap the search button to add cards.",
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            ChaseProgressHeader(
                                obtainedCount = state.obtainedCount,
                                totalCount = state.totalCount,
                                selectedFilter = state.filter,
                                onFilterSelected = viewModel::onFilterSelected
                            )
                            if (state.cards.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (state.filter == ChaseFilter.OBTAINED) {
                                            "You haven't obtained any of these cards yet."
                                        } else {
                                            "You've obtained every card on your list! 🎉"
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(state.cards, key = { it.id }) { card ->
                                        PokemonCardRow(
                                            id = card.id,
                                            name = card.name,
                                            setName = card.setName,
                                            number = card.number,
                                            imageUrl = card.imageUrl,
                                            onClick = { onNavigateToDetails(card.id) },
                                            obtained = card.obtained,
                                            onObtainedChange = { obtained ->
                                                viewModel.setObtained(card.id, obtained)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChaseProgressHeader(
    obtainedCount: Int,
    totalCount: Int,
    selectedFilter: ChaseFilter,
    onFilterSelected: (ChaseFilter) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "$obtainedCount of $totalCount obtained",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { obtainedCount.toFloat() / totalCount },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChaseFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) }
                )
            }
        }
    }
}
