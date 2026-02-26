package com.mindaigle.ui.screens.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindaigle.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityResourcesScreen(
    userName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf<ResourceType?>(null) }
    val resources = remember(selectedType) {
        if (selectedType == null) CommunityResourcesData.getAllResources()
        else CommunityResourcesData.getResourcesByType(selectedType!!)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community resources") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceLight,
                    titleContentColor = TextPrimary
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundLight)
        ) {
            Text(
                text = "Find food, shelter, health care, and crisis support near you.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedType == ResourceType.FOOD_PANTRY,
                    onClick = { selectedType = ResourceType.FOOD_PANTRY },
                    label = { Text("Food") }
                )
                FilterChip(
                    selected = selectedType == ResourceType.SHELTER,
                    onClick = { selectedType = ResourceType.SHELTER },
                    label = { Text("Shelter") }
                )
                FilterChip(
                    selected = selectedType == ResourceType.HOSPITAL,
                    onClick = { selectedType = ResourceType.HOSPITAL },
                    label = { Text("Health") }
                )
                FilterChip(
                    selected = selectedType == ResourceType.MENTAL_HEALTH,
                    onClick = { selectedType = ResourceType.MENTAL_HEALTH },
                    label = { Text("Mental health") }
                )
                FilterChip(
                    selected = selectedType == ResourceType.CRISIS,
                    onClick = { selectedType = ResourceType.CRISIS },
                    label = { Text("Crisis") }
                )
            }
            HorizontalDivider()
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(resources) { resource ->
                    ResourceCard(
                        resource = resource,
                        onDirections = {
                            if (resource.latitude != 0.0 || resource.longitude != 0.0) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.geoUri))
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            } else {
                                val mapsUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(resource.fullAddress)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, mapsUri))
                            }
                        },
                        onCall = {
                            resource.phone?.let { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { c -> c.isDigit() || c == '+' }}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceCard(
    resource: CommunityResource,
    onDirections: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = resource.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = CalmBlue,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (resource.address.isNotBlank() && resource.type != ResourceType.CRISIS) {
                Text(
                    text = resource.fullAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            resource.hours?.let { hours ->
                Text(
                    text = "Hours: $hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            resource.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (resource.latitude != 0.0 || resource.longitude != 0.0 || resource.address.isNotBlank()) {
                    Button(
                        onClick = onDirections,
                        colors = ButtonDefaults.buttonColors(containerColor = CalmBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Directions", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (resource.phone?.isNotBlank() == true) {
                    OutlinedButton(
                        onClick = onCall,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Call", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
