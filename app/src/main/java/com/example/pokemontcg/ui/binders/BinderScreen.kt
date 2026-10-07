package com.example.pokemontcg.ui.binders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pokemontcg.data.model.POCKETS_PER_PAGE
import com.example.pokemontcg.data.model.TrackedCard
import com.example.pokemontcg.ui.components.CARD_ASPECT_RATIO
import com.example.pokemontcg.ui.components.CardCornerShape
import com.example.pokemontcg.ui.components.CardImage
import com.example.pokemontcg.ui.components.LoadingIndicator
import com.example.pokemontcg.ui.components.MessageView
import com.example.pokemontcg.ui.components.PokedexHeader
import com.example.pokemontcg.ui.lists.HeaderLines
import com.example.pokemontcg.ui.lists.valueLine
import com.example.pokemontcg.ui.theme.BallWhite
import kotlinx.coroutines.launch

private const val COLUMNS = 3

private enum class BinderDialog { RENAME, DELETE }

@Composable
fun BinderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    /** Opens the card picker; picked cards fill the empty pockets from this one on. */
    onAddCards: (fromPocket: Int) -> Unit,
    factory: ViewModelProvider.Factory,
    viewModel: BinderViewModel = viewModel(factory = factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val movingCardId by viewModel.movingCardId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialog by rememberSaveable { mutableStateOf<BinderDialog?>(null) }

    BackHandler(enabled = movingCardId != null, onBack = viewModel::cancelMoving)

    LaunchedEffect(Unit) {
        viewModel.removedCards.collect { removed ->
            val result = snackbarHostState.showSnackbar(
                message = "${removed.card.name} taken out of the binder",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(removed)
        }
    }

    val binder = (uiState as? BinderUiState.Ready)?.binder
    when (dialog) {
        BinderDialog.RENAME -> if (binder != null) {
            BinderNameDialog(
                title = "Rename binder",
                confirmLabel = "Rename",
                initialName = binder.name,
                onConfirm = { name ->
                    dialog = null
                    viewModel.rename(name)
                },
                onDismiss = { dialog = null }
            )
        }
        BinderDialog.DELETE -> if (binder != null) {
            DeleteBinderDialog(
                name = binder.name,
                onConfirm = {
                    dialog = null
                    viewModel.delete()
                    onNavigateBack()
                },
                onDismiss = { dialog = null }
            )
        }
        null -> Unit
    }

    Scaffold(
        topBar = {
            PokedexHeader(
                title = binder?.name ?: "Binder",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (binder != null) {
                        BinderMenu(
                            hasCards = binder.cardCount > 0,
                            onRename = { dialog = BinderDialog.RENAME },
                            onCloseGaps = viewModel::closeGaps,
                            onSortBySet = viewModel::sortBySetAndNumber,
                            onDelete = { dialog = BinderDialog.DELETE }
                        )
                    }
                }
            ) {
                if (binder != null && binder.cardCount > 0) {
                    val pages = binder.pageCount
                    HeaderLines(
                        "${binder.cardCount} ${if (binder.cardCount == 1) "card" else "cards"} on " +
                            "$pages ${if (pages == 1) "page" else "pages"}",
                        valueLine(binder.value, "on Cardmarket")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
        when (val state = uiState) {
            BinderUiState.Loading -> LoadingIndicator(contentModifier)
            BinderUiState.NotFound -> MessageView(
                title = "Binder not found",
                message = "This binder has been deleted.",
                modifier = contentModifier
            )
            is BinderUiState.Ready -> BinderPages(
                binder = state.binder,
                movingCardId = movingCardId,
                onCardClick = onNavigateToDetails,
                onEmptyPocketClick = onAddCards,
                onStartMoving = viewModel::startMoving,
                onMoveTo = viewModel::moveTo,
                onRemoveMoving = viewModel::removeMovingCard,
                onCancelMoving = viewModel::cancelMoving,
                modifier = contentModifier
            )
        }
    }
}

@Composable
private fun BinderMenu(
    hasCards: Boolean,
    onRename: () -> Unit,
    onCloseGaps: () -> Unit,
    onSortBySet: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Binder options", tint = BallWhite)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val item = @Composable { label: String, enabled: Boolean, action: () -> Unit ->
                DropdownMenuItem(
                    text = { Text(label) },
                    enabled = enabled,
                    onClick = {
                        expanded = false
                        action()
                    }
                )
            }
            item("Rename", true, onRename)
            item("Remove empty pockets", hasCards, onCloseGaps)
            item("Sort by set and number", hasCards, onSortBySet)
            item("Delete binder", true, onDelete)
        }
    }
}

@Composable
private fun BinderPages(
    binder: BinderState,
    movingCardId: String?,
    onCardClick: (String) -> Unit,
    onEmptyPocketClick: (Int) -> Unit,
    onStartMoving: (String) -> Unit,
    onMoveTo: (Int) -> Unit,
    onRemoveMoving: () -> Unit,
    onCancelMoving: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moving = movingCardId?.let { id -> binder.pockets.values.firstOrNull { it.id == id } }
    // Where a card was just dropped: that page stays until the binder data catches up, so
    // dropping on the extra page doesn't flick the pager back for a frame
    var droppedPage by remember { mutableIntStateOf(-1) }
    LaunchedEffect(binder) { droppedPage = -1 }
    // While moving, an extra empty page lets a card go past the last page
    val pageCount = maxOf(binder.pageCount + if (moving != null) 1 else 0, droppedPage + 1)
    val pagerState = rememberPagerState { pageCount }
    val haptics = LocalHapticFeedback.current

    val pager = @Composable { pagerModifier: Modifier ->
        HorizontalPager(state = pagerState, modifier = pagerModifier, key = { it }) { page ->
            BinderPage(
                page = page,
                pockets = binder.pockets,
                movingCardId = moving?.id,
                onPocketClick = { pocket, card ->
                    when {
                        moving != null -> {
                            droppedPage = pocket / POCKETS_PER_PAGE
                            onMoveTo(pocket)
                        }
                        card != null -> onCardClick(card.id)
                        else -> onEmptyPocketClick(pocket)
                    }
                },
                onPocketLongClick = { card ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStartMoving(card.id)
                }
            )
        }
    }
    val hint = @Composable {
        Text(
            text = if (binder.cardCount == 0) "Tap a pocket to fill it from your collection."
            else "Long-press a card to move it or take it out.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }

    BoxWithConstraints(modifier = modifier) {
        if (maxWidth > maxHeight) {
            // Landscape: controls beside the page, so the page gets the full height
            Row {
                pager(Modifier.weight(1f).fillMaxHeight())
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (moving != null) {
                        MoveBar(card = moving, onRemove = onRemoveMoving, onCancel = onCancelMoving)
                    } else {
                        PageBar(pagerState = pagerState, onAddCards = null)
                        AddCardsButton(onClick = { onEmptyPocketClick(0) }, modifier = Modifier.padding(vertical = 8.dp))
                        hint()
                    }
                }
            }
        } else {
            Column {
                pager(Modifier.weight(1f).fillMaxWidth())
                if (moving != null) {
                    MoveBar(card = moving, onRemove = onRemoveMoving, onCancel = onCancelMoving)
                } else {
                    PageBar(pagerState = pagerState, onAddCards = { onEmptyPocketClick(0) })
                    hint()
                }
            }
        }
    }
}

/** One sleeve page: 3 × 3 pockets, as large as the space allows at the real card shape. */
@Composable
private fun BinderPage(
    page: Int,
    pockets: Map<Int, TrackedCard>,
    movingCardId: String?,
    onPocketClick: (pocket: Int, card: TrackedCard?) -> Unit,
    onPocketLongClick: (TrackedCard) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val sheetPadding = 8.dp
        val gap = 6.dp
        val rows = POCKETS_PER_PAGE / COLUMNS
        val widthFit = (maxWidth - sheetPadding * 2 - gap * (COLUMNS - 1)) / COLUMNS
        val heightFit = (maxHeight - sheetPadding * 2 - gap * (rows - 1)) / rows * CARD_ASPECT_RATIO
        val pocketWidth: Dp = min(widthFit, heightFit)

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(sheetPadding),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            repeat(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(COLUMNS) { column ->
                        val pocket = page * POCKETS_PER_PAGE + row * COLUMNS + column
                        val card = pockets[pocket]
                        Pocket(
                            pocket = pocket,
                            card = card,
                            width = pocketWidth,
                            isMoving = card != null && card.id == movingCardId,
                            moveMode = movingCardId != null,
                            onClick = { onPocketClick(pocket, card) },
                            onLongClick = card?.let { { onPocketLongClick(it) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Pocket(
    pocket: Int,
    card: TrackedCard?,
    width: Dp,
    isMoving: Boolean,
    moveMode: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val scale by animateFloatAsState(if (isMoving) 0.9f else 1f, label = "pocketScale")
    val border = when {
        isMoving -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        moveMode -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
    val number = pocket % POCKETS_PER_PAGE + 1
    Box(
        modifier = Modifier
            .size(width = width, height = width / CARD_ASPECT_RATIO)
            .scale(scale)
            .clip(CardCornerShape)
            .background(MaterialTheme.colorScheme.background)
            .border(border, CardCornerShape)
            .combinedClickable(
                onClickLabel = when {
                    moveMode -> "Move here"
                    card != null -> "Open card"
                    else -> "Add cards"
                },
                onLongClickLabel = if (onLongClick != null && !moveMode) "Move or take out" else null,
                onLongClick = if (moveMode) null else onLongClick,
                onClick = onClick
            )
            .semantics {
                contentDescription = if (card != null) "${card.name}, pocket $number" else "Empty pocket $number"
            },
        contentAlignment = Alignment.Center
    ) {
        if (card != null) {
            CardImage(imageUrl = card.imageUrl, contentDescription = card.name, modifier = Modifier.fillMaxSize())
        } else if (!moveMode) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Page number with previous/next buttons (swiping does the same), and "Add cards" when [onAddCards] is set. */
@Composable
private fun PageBar(pagerState: PagerState, onAddCards: (() -> Unit)?) {
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    Row(
        modifier = Modifier
            .then(if (onAddCards != null) Modifier.fillMaxWidth() else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
            enabled = page > 0
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
        }
        Text(
            text = "Page ${page + 1} of ${pagerState.pageCount}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
            enabled = page < pagerState.pageCount - 1
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
        }
        if (onAddCards != null) {
            Spacer(modifier = Modifier.weight(1f))
            AddCardsButton(onClick = onAddCards, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
private fun AddCardsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add cards")
    }
}

/** Shown while a card is picked up: tap a pocket to drop it there. */
@Composable
private fun MoveBar(card: TrackedCard, onRemove: () -> Unit, onCancel: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Moving ${card.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tap a pocket on any page",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            OutlinedButton(onClick = onRemove) { Text("Take out") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
