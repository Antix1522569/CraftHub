package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerEdition
import com.example.ui.components.AddServerDialog
import com.example.ui.components.ServerCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CraftHubViewModel
import com.example.ui.viewmodel.ServerFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    viewModel: CraftHubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val servers by viewModel.servers.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshingAll by viewModel.isRefreshingAll.collectAsState()
    val directPingState by viewModel.directPingState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showPingTester by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBackground)
                .testTag("servers_list"),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Screen Header & Search Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Przeglądarka Serwerów",
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                            Text(
                                text = "Serwery Non-Premium, Java & Bedrock",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Direct Ping Tool Toggle
                            IconButton(
                                onClick = { showPingTester = !showPingTester },
                                modifier = Modifier.testTag("toggle_ping_tester")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sensors,
                                    contentDescription = "Tester Ping",
                                    tint = if (showPingTester) EmeraldPrimary else TextSecondary
                                )
                            }

                            // Refresh All button
                            IconButton(
                                onClick = { viewModel.refreshAllServers() },
                                modifier = Modifier.testTag("refresh_all_servers_btn")
                            ) {
                                if (isRefreshingAll) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = EmeraldPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Odśwież wszystko",
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search TextField
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Szukaj serwera, adresu IP lub silnika...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Wyczyść", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_servers_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == ServerFilter.ALL,
                                onClick = { viewModel.setFilter(ServerFilter.ALL) },
                                label = { Text("Wszystkie (${servers.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = ObsidianBackground
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == ServerFilter.NON_PREMIUM,
                                onClick = { viewModel.setFilter(ServerFilter.NON_PREMIUM) },
                                label = { Text("⚡ Non-Premium / Offline") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = ObsidianBackground
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == ServerFilter.BEDROCK,
                                onClick = { viewModel.setFilter(ServerFilter.BEDROCK) },
                                label = { Text("📱 Bedrock") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = ObsidianBackground
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == ServerFilter.JAVA,
                                onClick = { viewModel.setFilter(ServerFilter.JAVA) },
                                label = { Text("☕ Java") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = ObsidianBackground
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == ServerFilter.FAVORITES,
                                onClick = { viewModel.setFilter(ServerFilter.FAVORITES) },
                                label = { Text("⭐ Ulubione") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = ObsidianBackground
                                )
                            )
                        }
                    }
                }
            }

            // Direct Ping Tester Panel (Expandable)
            item {
                AnimatedVisibility(visible = showPingTester) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .background(ObsidianSurfaceVariant)
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.NetworkCheck, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Szybki Test Pingu Serwera", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = directPingState.queryAddress,
                                    onValueChange = { viewModel.updateDirectPingQuery(it) },
                                    placeholder = { Text("np. hypixel.net lub ip:port", fontSize = 13.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("direct_ping_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = ObsidianBorder
                                    )
                                )

                                Button(
                                    onClick = { viewModel.runDirectPing() },
                                    enabled = directPingState.queryAddress.isNotBlank() && !directPingState.isTesting,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = ObsidianBackground
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("run_direct_ping_btn")
                                ) {
                                    if (directPingState.isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = ObsidianBackground,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Pinguj", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Show Ping Result
                            if (directPingState.result != null) {
                                val res = directPingState.result!!
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ObsidianBackground)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = if (res.isOnline) "🟢 ONLINE - Ping: ${res.latencyMs} ms" else "🔴 OFFLINE",
                                            fontWeight = FontWeight.Bold,
                                            color = if (res.isOnline) EmeraldPrimary else MaterialTheme.colorScheme.error
                                        )
                                        if (res.isOnline) {
                                            Text("Gracze: ${res.onlinePlayers}/${res.maxPlayers} | Wersja: ${res.version}", color = TextSecondary, fontSize = 12.sp)
                                            if (res.motdClean.isNotBlank()) {
                                                Text(res.motdClean, color = TextPrimary, fontSize = 12.sp, maxLines = 2)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Server List Items
            if (servers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Brak serwerów spełniających kryteria",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("Dodaj własny serwer", color = EmeraldPrimary)
                        }
                    }
                }
            } else {
                items(servers, key = { it.id }) { server ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ServerCard(
                            server = server,
                            onCopyIp = { viewModel.copyServerAddress(context, server) },
                            onQuickLaunch = { viewModel.launchBedrockServer(context, server) },
                            onShareInvite = { viewModel.shareServerInvite(context, server) },
                            onToggleFavorite = { viewModel.toggleFavorite(server.id) },
                            onRefresh = { viewModel.refreshSingleServer(server.id) },
                            onDelete = if (server.isCustom) {
                                { viewModel.deleteServer(server.id) }
                            } else null
                        )
                    }
                }
            }
        }

        // Add Server Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldPrimary,
            contentColor = ObsidianBackground,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 20.dp)
                .testTag("fab_add_server")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Dodaj serwer")
        }

        // Dialog
        if (showAddDialog) {
            AddServerDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, address, port, edition, isNonPremium ->
                    viewModel.addCustomServer(name, address, port, edition, isNonPremium)
                    showAddDialog = false
                }
            )
        }
    }
}
