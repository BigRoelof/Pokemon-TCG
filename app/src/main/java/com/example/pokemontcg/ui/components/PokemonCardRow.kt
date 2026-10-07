package com.example.pokemontcg.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PokemonCardRow(
    id: String,
    name: String,
    setName: String,
    number: String,
    imageUrl: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    obtained: Boolean = false,
    // When set, the row shows a checkbox to mark the card as obtained
    onObtainedChange: ((Boolean) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick(id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "$name card image",
                modifier = Modifier
                    .width(80.dp)
                    .height(110.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = setName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "No. $number",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (obtained) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ Obtained",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (onObtainedChange != null) {
                Checkbox(
                    checked = obtained,
                    onCheckedChange = onObtainedChange,
                    modifier = Modifier.semantics {
                        contentDescription = "Mark $name as obtained"
                    }
                )
            }
        }
    }
}
