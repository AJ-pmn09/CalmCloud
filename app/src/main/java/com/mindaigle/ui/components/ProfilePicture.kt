package com.mindaigle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.mindaigle.data.remote.ServerConfig

/**
 * Reusable Profile Picture Component
 * 
 * @param imageUrl Profile picture URL (can be relative or absolute)
 * @param userName User's name for fallback initial
 * @param size Size of the profile picture
 * @param shape Shape of the profile picture (Circle or RoundedCorner)
 * @param cornerRadius Corner radius if using RoundedCornerShape (default: 16.dp)
 * @param backgroundColor Background color for fallback
 * @param textColor Text color for fallback initial
 */
@Composable
fun ProfilePicture(
    imageUrl: String?,
    userName: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    shape: ProfilePictureShape = ProfilePictureShape.Circle,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    textColor: Color = MaterialTheme.colorScheme.primary
) {
    val fullImageUrl = if (imageUrl != null && imageUrl.isNotBlank()) {
        // If URL is absolute, use as-is; otherwise prepend server URL
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            imageUrl
        } else {
            // Relative URL - prepend server base URL
            val baseUrl = ServerConfig.getBaseUrl()
            if (imageUrl.startsWith("/")) {
                "$baseUrl$imageUrl"
            } else {
                "$baseUrl/$imageUrl"
            }
        }
    } else {
        null
    }
    
    val initial = userName.take(1).uppercase()
    val shapeModifier = when (shape) {
        ProfilePictureShape.Circle -> Modifier.clip(CircleShape)
        ProfilePictureShape.Rounded -> Modifier.clip(RoundedCornerShape(cornerRadius))
    }
    
    Box(
        modifier = modifier
            .size(size)
            .then(shapeModifier),
        contentAlignment = Alignment.Center
    ) {
        if (fullImageUrl != null) {
            SubcomposeAsyncImage(
                model = fullImageUrl,
                contentDescription = "$userName's profile picture",
                modifier = Modifier
                    .fillMaxSize()
                    .then(shapeModifier),
                contentScale = ContentScale.Crop,
                loading = {
                    // Show loading placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(shapeModifier)
                            .background(backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(size * 0.4f),
                            color = textColor
                        )
                    }
                },
                error = {
                    // Fallback to initial if image fails to load
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(shapeModifier)
                            .background(backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineSmall,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            )
        } else {
            // No image URL - show initial
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(shapeModifier)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

enum class ProfilePictureShape {
    Circle,
    Rounded
}
