package com.example.dazzlelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawerContent(
    apps: List<AppInfo>,
    homeApps: List<AppInfo>,
    dockApps: List<AppInfo>,
    isExpanded: Boolean,
    onAppClick: (String) -> Unit,
    onToggleHome: (AppInfo) -> Unit,
    shouldUseDarkText: Boolean,
    blurDrawer: Boolean,
    iconShape: IconShape
) {
    var searchQuery by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            gridState.scrollToItem(0)
            searchQuery = ""
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) apps else {
            apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    val labelColor = if (blurDrawer) {
        if (shouldUseDarkText) Color.Black else Color.White
    } else {
        if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onSurface
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp)
                    .height(56.dp),
                placeholder = { Text("Search apps...", color = labelColor.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = labelColor.copy(alpha = 0.5f)) },
                shape = CircleShape,
                singleLine = true,
                textStyle = TextStyle(color = labelColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = labelColor.copy(alpha = 0.08f),
                    unfocusedContainerColor = labelColor.copy(alpha = 0.08f),
                    focusedBorderColor = labelColor.copy(alpha = 0.15f),
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = labelColor,
                    focusedTextColor = labelColor,
                    unfocusedTextColor = labelColor
                )
            )
            
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = filteredApps,
                    key = { it.key }
                ) { app ->
                    val isOnHome = homeApps.any { it.packageName == app.packageName } || 
                                   dockApps.any { it.packageName == app.packageName }
                    
                    Box(contentAlignment = Alignment.TopEnd) {
                        AppItem(
                            app = app, 
                            onClick = onAppClick, 
                            onLongClick = { onToggleHome(app) },
                            labelColor = labelColor, 
                            iconShape = iconShape
                        )
                        
                        if (isOnHome) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp, end = 10.dp)
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(9.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
