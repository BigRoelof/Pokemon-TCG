package com.example.pokemontcg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pokemontcg.data.model.CardType
import com.example.pokemontcg.ui.theme.BallWhite
import com.example.pokemontcg.ui.theme.Ink

/**
 * A filter chip: [placeholder] when nothing is chosen, or the choice with a button to clear it.
 * [onHeader] styles it for the red header; otherwise for the page background.
 */
@Composable
fun PickerChip(
    placeholder: String,
    selectedLabel: String?,
    onOpen: () -> Unit,
    onClear: () -> Unit,
    clearDescription: String,
    onHeader: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(50)
    val outlineColor = if (onHeader) BallWhite else MaterialTheme.colorScheme.outline
    val labelColor = if (onHeader) BallWhite else MaterialTheme.colorScheme.onSurfaceVariant
    val selectedContainer = if (onHeader) BallWhite else MaterialTheme.colorScheme.secondary
    val selectedContent = if (onHeader) Ink else MaterialTheme.colorScheme.onSecondary
    Row(
        modifier = Modifier
            .clip(shape)
            .then(
                if (selectedLabel != null) Modifier.background(selectedContainer, shape)
                else Modifier.border(1.5.dp, outlineColor, shape)
            )
            .clickable(role = Role.Button, onClick = onOpen)
            .heightIn(min = 36.dp)
            .padding(start = 14.dp, end = if (selectedLabel != null) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedLabel == null) {
            Text(placeholder, style = MaterialTheme.typography.labelLarge, color = labelColor, maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = labelColor)
        } else {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.labelLarge,
                color = selectedContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Not weight(): chips may sit in a horizontally scrolling row with unbounded width
                modifier = Modifier.widthIn(max = 220.dp)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shape)
                    .clickable(role = Role.Button, onClick = onClear)
                    .semantics { contentDescription = clearDescription },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = selectedContent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

data class PickerOption(
    val key: String,
    val label: String,
    val detail: String? = null,
    val leading: (@Composable () -> Unit)? = null
)

/** A bottom sheet with "any" plus [options]; [onSelected] gets null for "any". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionPickerSheet(
    title: String,
    anyLabel: String,
    options: List<PickerOption>,
    selectedKey: String?,
    onSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
        )
        LazyColumn(modifier = Modifier.navigationBarsPadding()) {
            item(key = "any") {
                OptionRow(PickerOption("", anyLabel), selected = selectedKey == null, onClick = { onSelected(null) })
            }
            items(options, key = { it.key }) { option ->
                OptionRow(option, selected = option.key == selectedKey, onClick = { onSelected(option.key) })
            }
        }
    }
}

@Composable
private fun OptionRow(option: PickerOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            option.leading?.invoke()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(option.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (option.detail != null) {
                Text(option.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** The colour the card game uses for each energy type; none for Trainer and Energy cards. */
fun CardType.color(): Color? = when (this) {
    CardType.GRASS -> Color(0xFF5DAA3A)
    CardType.FIRE -> Color(0xFFE4572E)
    CardType.WATER -> Color(0xFF3A8DDE)
    CardType.LIGHTNING -> Color(0xFFF2C318)
    CardType.PSYCHIC -> Color(0xFF9B5DB5)
    CardType.FIGHTING -> Color(0xFFC0612B)
    CardType.DARKNESS -> Color(0xFF2E4A58)
    CardType.METAL -> Color(0xFF8E9AA6)
    CardType.FAIRY -> Color(0xFFE07BB0)
    CardType.DRAGON -> Color(0xFFB49A3A)
    CardType.COLORLESS -> Color(0xFFD9D9CF)
    CardType.TRAINER, CardType.ENERGY -> null
}

/** A dot in the type's colour, outlined so light colours stay visible. */
@Composable
fun TypeDot(type: CardType, size: Int = 16) {
    val color = type.color() ?: return
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color, CircleShape)
            .border(1.dp, Ink.copy(alpha = 0.25f), CircleShape)
    )
}
