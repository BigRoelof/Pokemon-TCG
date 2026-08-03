package com.example.pokemontcg.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    if (state.cards.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Your chase list is empty.\nClick the search button to add cards.")
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val obtainedCount = state.cards.count { it.obtained }
                            val totalCount = state.cards.size
                            val percentage = if (totalCount > 0) (obtainedCount * 100) / totalCount else 0
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$obtainedCount / $totalCount Chase Cards collected ($percentage%)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

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
                                        onObtainedClick = { viewModel.toggleObtainedStatus(card.id, card.obtained) }
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
