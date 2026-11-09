package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PlayerProfile
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NonPremiumBadgeColor
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.ServerOfflineRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CraftHubViewModel

@Composable
fun ProfilesScreen(
    viewModel: CraftHubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val servers by viewModel.servers.collectAsState()

    var showAddProfileDialog by remember { mutableStateOf(false) }
    var friendGamertag by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("profiles_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Konta & Profile Graczy",
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "Konta offline i zaproszenia Microsoft / Xbox",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = { showAddProfileDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = ObsidianBackground
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_profile_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nowy profil", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Profile Hero Showcase with 3D Skin
            if (activeProfile != null) {
                ActiveProfileHeroCard(
                    profile = activeProfile!!,
                    onShareInvite = {
                        val server = servers.firstOrNull()
                        if (server != null) {
                            viewModel.shareServerInvite(context, server)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // List of saved profiles
            Text(
                text = "Zapisane profile graczy (${profiles.size})",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Profile Cards
        items(profiles, key = { it.id }) { profile ->
            ProfileItemRow(
                profile = profile,
                isActive = profile.id == activeProfile?.id,
                onSelect = { viewModel.selectActiveProfile(profile.id) },
                onDelete = if (profiles.size > 1) {
                    { viewModel.deleteProfile(profile.id) }
                } else null
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Microsoft / Xbox Gamertag Invite Generator
        item {
            Spacer(modifier = Modifier.height(20.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Zaproś przez darmowe konto Microsoft / Xbox",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Gracze Minecraft Bedrock używają darmowych kont Microsoft do gry wieloosobowej. Wpisz gamertag znajomego, aby wygenerować kartę zaproszenia:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = friendGamertag,
                        onValueChange = { friendGamertag = it },
                        label = { Text("Gamertag Microsoft / Xbox znajomego") },
                        placeholder = { Text("np. CraftMaster99") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("friend_gamertag_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val activeNick = activeProfile?.username ?: "Gracz"
                            val server = servers.firstOrNull()
                            val sName = server?.name ?: "Nasz serwer"
                            val sAddress = server?.fullAddress ?: "mc.craftpoland.pl"

                            val text = """
                                🎮 Hej ${friendGamertag.ifBlank { "znajomy" }}!
                                Zapraszam Cię do wspólnej gry w Minecraft przez CraftHub!
                                
                                📍 Serwer: $sName
                                🌐 Adres IP: $sAddress
                                👤 Mój profil: $activeNick
                                
                                Jeśli grasz na telefonie lub Bedrock, wystarczy darmowe konto Microsoft!
                            """.trimIndent()

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Wyślij zaproszenie"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = ObsidianBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_gamertag_invite_btn")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wyślij zaproszenie dla znajomego", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Non-Premium Authentication Tutorial
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(ObsidianSurfaceVariant)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Jak działa autentykacja Non-Premium?",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Serwery z obsługą darmowych kont offline (np. PaperMC z online-mode=false) chronią graczy za pomocą pluginów bezpieczeństwa (np. AuthMe):\n\n" +
                                "1. Po wejściu wpisz na czacie: /register <hasło> <hasło>\n" +
                                "2. Przy kolejnym logowaniu wpisz: /login <hasło>\n" +
                                "3. Nikt inny nie wejdzie na Twój nick bez Twojego hasła!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }

    // Add Profile Dialog
    if (showAddProfileDialog) {
        var newNick by remember { mutableStateOf("") }
        var isOffline by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Dodaj profil gracza", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newNick,
                        onValueChange = { newNick = it },
                        label = { Text("Nick gracza / Gamertag") },
                        placeholder = { Text("np. StevePro_PL") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_profile_nick")
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isOffline,
                            onCheckedChange = { isOffline = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Konto Offline (Non-Premium)",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNick.isNotBlank()) {
                            viewModel.addProfile(newNick, isOffline)
                            showAddProfileDialog = false
                        }
                    },
                    enabled = newNick.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = ObsidianBackground
                    ),
                    modifier = Modifier.testTag("confirm_add_profile_btn")
                ) {
                    Text("Zapisz profil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfileDialog = false }) {
                    Text("Anuluj", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ActiveProfileHeroCard(
    profile: PlayerProfile,
    onShareInvite: () -> Unit
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("active_profile_hero"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3D Render of Skin or Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianBackground)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(profile.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Skin Avatara",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldContainer
                ) {
                    Text(
                        text = "AKTYWNY PROFIL",
                        color = EmeraldOnContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = profile.username,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )

                Text(
                    text = if (profile.isOfflineAccount) "Konto Offline (Non-Premium)" else "Konto Microsoft / Xbox",
                    color = if (profile.isOfflineAccount) Color(0xFFC084FC) else EmeraldPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onShareInvite,
                modifier = Modifier.testTag("active_profile_share_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Udostępnij profil",
                    tint = EmeraldPrimary
                )
            }
        }
    }
}

@Composable
fun ProfileItemRow(
    profile: PlayerProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .border(
                1.dp,
                if (isActive) EmeraldPrimary else ObsidianBorder,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) ObsidianSurfaceVariant else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ObsidianBackground),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(profile.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.username,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (profile.isOfflineAccount) "Offline" else "Microsoft / Xbox",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (isActive) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Aktywny",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Usuń profil",
                        tint = ServerOfflineRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
