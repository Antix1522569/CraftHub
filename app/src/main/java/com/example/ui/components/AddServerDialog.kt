package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerEdition
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String, port: Int, edition: ServerEdition, isNonPremium: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var edition by remember { mutableStateOf(ServerEdition.JAVA) }
    var portText by remember { mutableStateOf("25565") }
    var isNonPremium by remember { mutableStateOf(true) }

    fun updateEdition(newEdition: ServerEdition) {
        edition = newEdition
        if (newEdition == ServerEdition.BEDROCK && portText == "25565") {
            portText = "19132"
        } else if (newEdition == ServerEdition.JAVA && portText == "19132") {
            portText = "25565"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("add_server_dialog"),
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "Dodaj serwer Minecraft",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Server Edition Chips
                Text("Edycja gry:", color = TextSecondary, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServerEdition.values().forEach { ed ->
                        FilterChip(
                            selected = edition == ed,
                            onClick = { updateEdition(ed) },
                            label = { Text(ed.name.replace("_", " "), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = ObsidianBackground
                            )
                        )
                    }
                }

                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa serwera (opcjonalnie)") },
                    placeholder = { Text("np. Mój Prywatny Serwer") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_server_name")
                )

                // Address input
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adres IP lub domena *") },
                    placeholder = { Text("np. play.myserver.net") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_server_address")
                )

                // Port input
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_server_port")
                )

                // Non-Premium checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isNonPremium,
                        onCheckedChange = { isNonPremium = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary),
                        modifier = Modifier.testTag("checkbox_non_premium")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Wspiera darmowe konta offline (Non-Premium)",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (address.isNotBlank()) {
                        val parsedPort = portText.toIntOrNull() ?: if (edition == ServerEdition.BEDROCK) 19132 else 25565
                        onConfirm(name, address, parsedPort, edition, isNonPremium)
                    }
                },
                enabled = address.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = ObsidianBackground),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_add_server")
            ) {
                Text("Dodaj", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = TextSecondary)
            }
        }
    )
}
