package com.example.pokemontcg.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pokemontcg.ui.ViewModelFactory
import com.example.pokemontcg.ui.components.ErrorView
import com.example.pokemontcg.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    cardId: String,
    onNavigateBack: () -> Unit,
    factory: ViewModelFactory,
    viewModel: DetailsViewModel = viewModel(factory = factory)
) {
    LaunchedEffect(cardId) {
        viewModel.loadCard(cardId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val isObtained by viewModel.isObtained.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DetailsUiState.Loading -> {
                    LoadingIndicator()
                }
                is DetailsUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = viewModel::retry
                    )
                }
                is DetailsUiState.Success -> {
                    val card = state.card
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = card.images?.large ?: card.images?.small,
                            contentDescription = "${card.name} large image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Set: ${card.set?.name ?: "Unknown"}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Number: ${card.number}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        if (isSaved) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .toggleable(
                                        value = isObtained,
                                        role = Role.Switch,
                                        onValueChange = viewModel::setObtained
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "I've obtained this card",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(checked = isObtained, onCheckedChange = null)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Button(
                            onClick = { viewModel.toggleChaseList() },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text(if (isSaved) "Remove from Chase List" else "Add to Chase List")
                        }
                    }
                }
            }
        }
    }
}
