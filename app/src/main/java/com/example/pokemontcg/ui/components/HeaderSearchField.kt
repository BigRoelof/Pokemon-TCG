package com.example.pokemontcg.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.example.pokemontcg.ui.theme.BallWhite
import com.example.pokemontcg.ui.theme.Ink
import com.example.pokemontcg.ui.theme.SlateText

/** White, pill-shaped search field for the red header. [autoFocus] opens the keyboard on first entry. */
@Composable
fun HeaderSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    autoFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Open the keyboard on first entry only, not when coming back from a card
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (autoFocus && !autoFocused) {
            focusRequester.requestFocus()
            autoFocused = true
        }
    }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = BallWhite,
            unfocusedContainerColor = BallWhite,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            cursorColor = Ink,
            focusedLeadingIconColor = Ink,
            unfocusedLeadingIconColor = SlateText,
            focusedTrailingIconColor = Ink,
            unfocusedTrailingIconColor = SlateText,
            focusedPlaceholderColor = SlateText,
            unfocusedPlaceholderColor = SlateText,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
