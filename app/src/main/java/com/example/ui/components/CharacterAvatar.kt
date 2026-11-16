package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.mira.memory.model.CharacterProfileEntity

@Composable
fun CharacterAvatar(
    character: CharacterProfileEntity,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 2.dp
) {
    val fallbackRes = when (character.presetDrawableKey) {
        "ananya" -> R.drawable.img_char_ananya
        "kavya" -> R.drawable.img_char_kavya
        else -> R.drawable.img_mira_portrait
    }

    val ringBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    val clippedModifier = modifier
        .clip(CircleShape)
        .border(borderWidth, ringBrush, CircleShape)

    if (!character.referencePhotoUri.isNullOrBlank()) {
        AsyncImage(
            model = character.referencePhotoUri,
            contentDescription = "${character.name} portrait",
            placeholder = painterResource(fallbackRes),
            error = painterResource(fallbackRes),
            contentScale = ContentScale.Crop,
            modifier = clippedModifier
        )
    } else {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = "${character.name} portrait",
            contentScale = ContentScale.Crop,
            modifier = clippedModifier
        )
    }
}
