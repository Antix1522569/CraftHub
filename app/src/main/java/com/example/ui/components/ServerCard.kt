package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MinecraftServer
import com.example.data.model.ServerEdition
import com.example.ui.theme.BedrockBadgeColor
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JavaBadgeColor
import com.example.ui.theme.NonPremiumBadgeColor
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.PingGood
import com.example.ui.theme.PingHigh
import com.example.ui.theme.PingMedium
import com.example.ui.theme.ServerOfflineRed
import com.example.ui.theme.ServerOnlineGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServerCard(
    server: MinecraftServer,
    onCopyIp: () -> Unit,
    onQuickLaunch: () -> Unit,
    onShareInvite: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val pingColor = when {
        !server.isOnline -> ServerOfflineRed
        server.latencyMs < 75 -> PingGood
        server.latencyMs < 160 -> PingMedium
        else -> PingHigh
    }

    val editionBg = when (server.edition) {
        ServerEdition.JAVA -> JavaBadgeColor.copy(alpha = 0.2f)
        ServerEdition.BEDROCK -> BedrockBadgeColor.copy(alpha = 0.2f)
        ServerEdition.HYBRID_GEYSER -> EmeraldContainer
    }

    val editionText = when (server.edition) {
        ServerEdition.JAVA -> JavaBadgeColor
        ServerEdition.BEDROCK -> BedrockBadgeColor
        ServerEdition.HYBRID_GEYSER -> EmeraldOnContainer
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .testTag("server_card_${server.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            // Header Row: Icon, Title, Status & Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Server Icon or Placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurfaceVariant)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!server.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(server.iconUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Server Icon",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Text(
                            text = server.name.take(2).uppercase(),
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Address
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = server.fullAddress,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite and Refresh buttons
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("favorite_btn_${server.id}")
                ) {
                    Icon(
                        imageVector = if (server.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Ulubiony",
                        tint = if (server.isFavorite) Color(0xFFFBBF24) else TextMuted
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("refresh_btn_${server.id}")
                ) {
                    if (server.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = EmeraldPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Odśwież",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row: Status dot + Ping, Edition, Non-Premium, Player Count
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Online/Offline Pill with Ping
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = pingColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, pingColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(pingColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (server.isOnline) "${server.latencyMs} ms" else "Offline",
                            color = pingColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Edition Tag
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = editionBg
                ) {
                    Text(
                        text = server.edition.displayName,
                        color = editionText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Non-Premium Tag
                if (server.isNonPremium) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = NonPremiumBadgeColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NonPremiumBadgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Non-Premium / Offline",
                            color = Color(0xFFC084FC),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Player count tag
                if (server.isOnline) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianSurfaceVariant
                    ) {
                        Text(
                            text = "Gracze: ${server.onlinePlayers}/${server.maxPlayers}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Version tag
                if (server.version.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianSurfaceVariant
                    ) {
                        Text(
                            text = server.version,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MOTD Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBackground)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(10.dp)
            ) {
                if (server.motdRaw.isNotBlank()) {
                    MinecraftFormattedText(
                        rawText = server.motdRaw,
                        fontSize = 13.sp,
                        maxLines = if (isExpanded) 10 else 2
                    )
                } else if (server.motdClean.isNotBlank()) {
                    Text(
                        text = server.motdClean,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = if (isExpanded) 10 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = if (server.isOnline) "Serwer aktywny" else "Brak odpowiedzi od serwera",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Launch (Bedrock) or Direct Join
                FilledTonalButton(
                    onClick = onQuickLaunch,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_launch_btn_${server.id}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = ObsidianBackground
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (server.edition == ServerEdition.BEDROCK || server.edition == ServerEdition.HYBRID_GEYSER)
                            "Graj Bedrock" else "Uruchom",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Copy IP Button
                OutlinedButton(
                    onClick = onCopyIp,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("copy_ip_btn_${server.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Kopiuj",
                        modifier = Modifier.size(16.dp),
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kopiuj IP", color = TextPrimary, fontSize = 12.sp)
                }

                // Share / Invite Button
                IconButton(
                    onClick = onShareInvite,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("share_invite_btn_${server.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Zaproś",
                        tint = EmeraldPrimary
                    )
                }

                // Delete custom server if applicable
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("delete_server_btn_${server.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Usuń",
                            tint = ServerOfflineRed
                        )
                    }
                }
            }
        }
    }
}
