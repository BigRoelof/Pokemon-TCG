package com.example.pokemontcg.ui.lists

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pokemontcg.ui.components.AppBottomBar
import com.example.pokemontcg.ui.components.PokedexHeader
import com.example.pokemontcg.ui.components.TopLevelSection

/** The frame shared by the top-level screens: Pokédex header, bottom bar, "Add cards" (or [addLabel]) button. */
@Composable
fun ListScreenScaffold(
    title: String,
    section: TopLevelSection,
    onSelectSection: (TopLevelSection) -> Unit,
    onAddCards: () -> Unit,
    showAddButton: Boolean,
    progress: Float?,
    addLabel: String = "Add cards",
    addIcon: ImageVector = Icons.Default.Search,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    headerContent: @Composable ColumnScope.() -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { PokedexHeader(title = title, progress = progress, content = headerContent) },
        bottomBar = { AppBottomBar(selected = section, onSelect = onSelectSection) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showAddButton) {
                ExtendedFloatingActionButton(
                    onClick = onAddCards,
                    icon = { Icon(addIcon, contentDescription = null) },
                    text = { Text(addLabel) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }
        },
        content = content
    )
}
