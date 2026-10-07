package com.example.pokemontcg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pokemontcg.ui.theme.BallWhite
import com.example.pokemontcg.ui.theme.CaughtYellow
import com.example.pokemontcg.ui.theme.Ink

/** Width / height of a real trading card (63 × 88 mm). */
const val CARD_ASPECT_RATIO = 63f / 88f

/** Real cards have corners of about 3 mm on a 63 mm width. */
val CardCornerShape = RoundedCornerShape(7.dp)

@Composable
fun CardImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    caught: Boolean = false
) {
    Box(
        modifier = modifier
            .aspectRatio(CARD_ASPECT_RATIO)
            .clip(CardCornerShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (caught) Modifier.border(3.dp, CaughtYellow, CardCornerShape) else Modifier)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * A card in a grid. With [onCaughtChange] the tile shows a ball button to mark the card as
 * caught; [tag] shows a short label on the image instead (used in search results).
 */
@Composable
fun CardTile(
    name: String,
    setName: String,
    number: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caught: Boolean = false,
    onCaughtChange: ((Boolean) -> Unit)? = null,
    tag: String? = null,
    /** Formatted price, shown next to the name. */
    price: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box {
            CardImage(
                imageUrl = imageUrl,
                contentDescription = name,
                caught = caught,
                modifier = Modifier.fillMaxWidth()
            )
            if (onCaughtChange != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(48.dp)
                        .toggleable(value = caught, role = Role.Checkbox, onValueChange = onCaughtChange)
                        .semantics { contentDescription = "Caught $name" },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(shape = CircleShape, color = BallWhite, shadowElevation = 3.dp) {
                        PokeBall(
                            filled = caught,
                            size = 26.dp,
                            outlineColor = Ink.copy(alpha = 0.45f),
                            modifier = Modifier.padding(3.dp)
                        )
                    }
                }
            }
            if (tag != null) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = BallWhite,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Ink, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (price != null) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
        // The set name gives way to the number, which identifies the card
        Row {
            Text(
                text = setName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                text = "  #$number",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
