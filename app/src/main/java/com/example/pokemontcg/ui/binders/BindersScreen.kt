package com.example.pokemontcg.ui.binders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.pokemontcg.data.model.POCKETS_PER_PAGE
import com.example.pokemontcg.ui.components.CARD_ASPECT_RATIO
import com.example.pokemontcg.ui.components.LoadingIndicator
import com.example.pokemontcg.ui.components.MessageView
import com.example.pokemontcg.ui.components.TopLevelSection
import com.example.pokemontcg.ui.formatEuro
import com.example.pokemontcg.ui.lists.HeaderLines
import com.example.pokemontcg.ui.lists.ListScreenScaffold
import com.example.pokemontcg.ui.theme.BinderCoverColors

@Composable
fun BindersScreen(
    onOpenBinder: (Long) -> Unit,
    onSelectSection: (TopLevelSection) -> Unit,
    factory: ViewModelProvider.Factory,
    viewModel: BindersViewModel = viewModel(factory = factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewBinder by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.createdBinders.collect(onOpenBinder)
    }

    if (showNewBinder) {
        BinderNameDialog(
            title = "New binder",
            confirmLabel = "Create",
            onConfirm = { name ->
                showNewBinder = false
                viewModel.createBinder(name)
            },
            onDismiss = { showNewBinder = false }
        )
    }

    val binders = state
    ListScreenScaffold(
        title = "Binders",
        section = TopLevelSection.BINDERS,
        onSelectSection = onSelectSection,
        onAddCards = { showNewBinder = true },
        showAddButton = !binders.isNullOrEmpty(),
        progress = null,
        addLabel = "New binder",
        addIcon = Icons.Default.Add,
        headerContent = {
            if (!binders.isNullOrEmpty()) {
                HeaderLines(if (binders.size == 1) "1 binder" else "${binders.size} binders")
            }
        }
    ) { innerPadding ->
        when {
            binders == null -> LoadingIndicator(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            )
            binders.isEmpty() -> MessageView(
                title = "No binders yet",
                message = "Make a binder for a set, a Pokémon or your favourite cards, then fill its pages from your collection.",
                actionLabel = "New binder",
                onAction = { showNewBinder = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            )
            else -> {
                val layoutDirection = LocalLayoutDirection.current
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
                        end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(binders, key = { it.id }) { binder ->
                        BinderCover(binder = binder, onClick = { onOpenBinder(binder.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

/** A binder seen from the front: a coloured cover with rings on the spine, showing its first page. */
@Composable
private fun BinderCover(binder: BinderSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val coverColor = BinderCoverColors[(binder.id % BinderCoverColors.size).toInt()]
    val shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp)
    val details = when (binder.cardCount) {
        0 -> "Empty"
        1 -> "1 card"
        else -> "${binder.cardCount} cards"
    } + if (binder.value > 0) " · ${formatEuro(binder.value)}" else ""

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "${binder.name}, $details" }
    ) {
        MiniPage(
            images = binder.firstPage,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(coverColor)
                .drawBehind { drawSpine() }
                .padding(start = SpineWidth + 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = binder.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = details,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private val SpineWidth = 18.dp

/** A darker strip down the left edge with three rings. */
private fun DrawScope.drawSpine() {
    val width = SpineWidth.toPx()
    drawRect(Color.Black.copy(alpha = 0.22f), size = Size(width, size.height))
    listOf(0.2f, 0.5f, 0.8f).forEach { y ->
        drawCircle(Color.White.copy(alpha = 0.55f), radius = 4.dp.toPx(), center = Offset(width / 2, size.height * y))
    }
}

/** A tiny 3 × 3 page: the cards in the first nine pockets. */
@Composable
private fun MiniPage(images: List<String?>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        images.chunked(POCKETS_PER_PAGE / 3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { imageUrl ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(CARD_ASPECT_RATIO)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
