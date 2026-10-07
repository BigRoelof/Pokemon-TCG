package com.example.pokemontcg.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.data.model.Card
import com.example.pokemontcg.ui.components.CardCornerShape
import com.example.pokemontcg.ui.components.CardImage
import com.example.pokemontcg.ui.components.LoadingIndicator
import com.example.pokemontcg.ui.components.MessageView
import com.example.pokemontcg.ui.components.PokeBall
import com.example.pokemontcg.ui.components.PokedexHeader
import com.example.pokemontcg.ui.formatEuro
import com.example.pokemontcg.ui.theme.CaughtYellow
import com.example.pokemontcg.ui.theme.Ink
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DetailsScreen(
    cardId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSet: (String) -> Unit,
    factory: ViewModelProvider.Factory,
    viewModel: DetailsViewModel = viewModel(factory = factory)
) {
    LaunchedEffect(cardId) {
        viewModel.loadCard(cardId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val isObtained by viewModel.isObtained.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()

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
            is DetailsUiState.NotFound -> MessageView(
                title = "Card not found",
                message = "This card isn't in the card database. It may have been removed from the dataset.",
                modifier = contentModifier
            )
            is DetailsUiState.Success -> CardDetails(
                card = state.card,
                isSaved = isSaved,
                isCaught = isObtained,
                onToggleChaseList = viewModel::toggleChaseList,
                onCaughtChange = viewModel::setObtained,
                onSetClick = onNavigateToSet,
                price = price,
                onRetryPrice = viewModel::refreshPrice,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun CardDetails(
    card: Card,
    isSaved: Boolean,
    isCaught: Boolean,
    onToggleChaseList: () -> Unit,
    onCaughtChange: (Boolean) -> Unit,
    onSetClick: (String) -> Unit,
    price: PriceUiState,
    onRetryPrice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CardImage(
            imageUrl = card.imageLarge ?: card.imageSmall,
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
        val setId = card.setId
        if (setId != null) {
            SetLink(setName = card.setName, onClick = { onSetClick(setId) })
        } else {
            Text(
                text = card.setName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
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
            PriceSection(card = card, state = price, onRetry = onRetryPrice)
            CardFacts(card)
            if (isSaved) {
                CaughtToggle(caught = isCaught, onCaughtChange = onCaughtChange)
                OutlinedButton(
                    onClick = onToggleChaseList,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
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

/** Facts from the dataset; rows the dataset doesn't have for this card are left out. */
@Composable
private fun CardFacts(card: Card) {
    val facts = listOfNotNull(
        card.rarity?.let { "Rarity" to it },
        card.artist?.let { "Illustrator" to it },
        card.setSeries?.let { "Series" to it },
        card.releaseDate?.let { "Released" to formatReleaseDate(it) }
    )
    if (facts.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            facts.forEach { (label, value) ->
                Row {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(96.dp)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** `2023/01/20` → `20 January 2023` in the user's locale; falls back to the raw text. */
private fun formatReleaseDate(date: String): String = runCatching {
    val parsed = SimpleDateFormat("yyyy/MM/dd", Locale.US).parse(date)!!
    DateFormat.getDateInstance(DateFormat.LONG).format(parsed)
}.getOrDefault(date)

/** The set name as a link to browse the whole set. */
@Composable
private fun SetLink(setName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = "Browse $setName", role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/** Cardmarket price in euros, with the last known price kept when offline. */
@Composable
private fun PriceSection(card: Card, state: PriceUiState, onRetry: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cardmarket price",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val row = state.price
            when {
                row?.price != null -> {
                    Text(
                        text = formatEuro(row.price),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val extras = listOfNotNull(
                        row.average30?.let { "30-day average ${formatEuro(it)}" },
                        row.low?.let { "From ${formatEuro(it)}" }
                    )
                    if (extras.isNotEmpty()) {
                        Text(
                            text = extras.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = when {
                            state.failed && row.sourceUpdated != null ->
                                "Couldn't update. Price from ${formatPriceDate(row.sourceUpdated)}"
                            row.sourceUpdated != null -> "Trend price, updated ${formatPriceDate(row.sourceUpdated)}"
                            else -> "Trend price"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val productId = row.cardmarketProductId
                    if (productId != null) {
                        TextButton(
                            onClick = { uriHandler.openUri(cardmarketUrl(productId)) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("View on Cardmarket")
                        }
                    }
                }
                row != null && !state.loading -> Text(
                    text = "Cardmarket has no price for this card.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                state.failed -> {
                    Text(
                        text = "Couldn't load the price. Check your internet connection.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                        Text("Try again")
                    }
                }
                else -> Text(
                    text = "Checking the price\u2026",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun cardmarketUrl(productId: Int) = "https://www.cardmarket.com/en/Pokemon/Products?idProduct=$productId"

/** `2026-10-07T09:52:36.679Z` -> `7 October 2026` in the user's locale. */
private fun formatPriceDate(iso: String): String = runCatching {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.take(10))!!
    DateFormat.getDateInstance(DateFormat.LONG).format(parsed)
}.getOrDefault(iso.take(10))

