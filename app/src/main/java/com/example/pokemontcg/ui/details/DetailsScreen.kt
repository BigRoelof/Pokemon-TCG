package com.example.pokemontcg.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.data.api.model.CardDto
import com.example.pokemontcg.ui.ViewModelFactory
import com.example.pokemontcg.ui.components.CardCornerShape
import com.example.pokemontcg.ui.components.CardImage
import com.example.pokemontcg.ui.components.ErrorView
import com.example.pokemontcg.ui.components.LoadingIndicator
import com.example.pokemontcg.ui.components.PokeBall
import com.example.pokemontcg.ui.components.PokedexHeader
import com.example.pokemontcg.ui.theme.CaughtYellow
import com.example.pokemontcg.ui.theme.Ink

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
            PokedexHeader(
                title = (uiState as? DetailsUiState.Success)?.card?.name ?: "Card",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        when (val state = uiState) {
            is DetailsUiState.Loading -> LoadingIndicator(contentModifier)
            is DetailsUiState.Error -> ErrorView(
                message = state.message,
                onRetry = viewModel::retry,
                modifier = contentModifier
            )
            is DetailsUiState.Success -> CardDetails(
                card = state.card,
                isSaved = isSaved,
                isCaught = isObtained,
                onToggleChaseList = viewModel::toggleChaseList,
                onCaughtChange = viewModel::setObtained,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun CardDetails(
    card: CardDto,
    isSaved: Boolean,
    isCaught: Boolean,
    onToggleChaseList: () -> Unit,
    onCaughtChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CardImage(
            imageUrl = card.images?.large ?: card.images?.small,
            contentDescription = card.name,
            caught = isSaved && isCaught,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .fillMaxWidth(0.8f)
                .shadow(12.dp, CardCornerShape)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = card.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = card.set?.name ?: "Unknown set",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Card #${card.number}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.widthIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSaved) {
                CaughtToggle(caught = isCaught, onCaughtChange = onCaughtChange)
                OutlinedButton(onClick = onToggleChaseList, modifier = Modifier.fillMaxWidth()) {
                    Text("Remove from chase list")
                }
            } else {
                Button(onClick = onToggleChaseList, modifier = Modifier.fillMaxWidth()) {
                    Text("Add to chase list")
                }
            }
        }
    }
}

@Composable
private fun CaughtToggle(caught: Boolean, onCaughtChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .toggleable(value = caught, role = Role.Switch, onValueChange = onCaughtChange)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PokeBall(filled = caught, size = 32.dp, outlineColor = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Caught",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This card is in my collection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = caught,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = CaughtYellow,
                    checkedThumbColor = Ink,
                    checkedBorderColor = CaughtYellow
                )
            )
        }
    }
}
