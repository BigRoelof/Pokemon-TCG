package com.example.pokemontcg.ui.binders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A binder and whether it holds the card on screen. */
data class BinderChoice(val id: Long, val name: String, val holdsCard: Boolean)

/** Ticks the binders a card is in; changes apply right away. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinderChoiceSheet(
    cardName: String,
    binders: List<BinderChoice>,
    onToggle: (binderId: Long, holdsCard: Boolean) -> Unit,
    onCreateBinder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showNewBinder by rememberSaveable { mutableStateOf(false) }
    if (showNewBinder) {
        BinderNameDialog(
            title = "New binder",
            confirmLabel = "Create",
            onConfirm = { name ->
                showNewBinder = false
                onCreateBinder(name)
            },
            onDismiss = { showNewBinder = false }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Put $cardName in",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            binders.forEach { binder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = binder.holdsCard, role = Role.Checkbox, onValueChange = { onToggle(binder.id, it) })
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = binder.holdsCard, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                    Text(binder.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { showNewBinder = true }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(modifier = Modifier.padding(12.dp).size(24.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text("New binder", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
