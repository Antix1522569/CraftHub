package com.example.data.model

enum class ServerEdition(val displayName: String) {
    JAVA("Java Edition"),
    BEDROCK("Bedrock Edition"),
    HYBRID_GEYSER("Java + Bedrock (Geyser)")
}

data class MinecraftServer(
    val id: String,
    val name: String,
    val address: String,
    val port: Int = 25565,
    val edition: ServerEdition = ServerEdition.JAVA,
    val isNonPremium: Boolean = true, // Offline mode friendly
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val isOnline: Boolean = false,
    val latencyMs: Long = -1,
    val onlinePlayers: Int = 0,
    val maxPlayers: Int = 0,
    val version: String = "1.21.x",
    val software: String = "PaperMC",
    val motdRaw: String = "",
    val motdClean: String = "",
    val iconUrl: String? = null,
    val lastCheckedTime: Long = 0,
    val isChecking: Boolean = false
) {
    val fullAddress: String
        get() = if ((edition == ServerEdition.JAVA && port == 25565) || 
                   (edition == ServerEdition.BEDROCK && port == 19132)) {
            address
        } else {
            "$address:$port"
        }
}

data class EngineRelease(
    val id: String,
    val name: String,
    val category: String, // Server / Client / Proxy
    val latestVersion: String,
    val buildNumber: String? = null,
    val channel: String = "Stable Release",
    val releaseDate: String,
    val downloadUrl: String,
    val features: List<String>,
    val isRecommendedForOffline: Boolean = false,
    val isCheckingForUpdate: Boolean = false
)

data class PlayerProfile(
    val id: String,
    val username: String,
    val isOfflineAccount: Boolean = true,
    val isDefault: Boolean = false,
    val skinType: String = "classic",
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    val avatarUrl: String
        get() = "https://crafatar.com/avatars/$username?size=128&overlay=true"
    
    val bodyRenderUrl: String
        get() = "https://crafatar.com/renders/body/$username?scale=5&overlay=true"
}

data class ModItem(
    val id: String,
    val title: String,
    val description: String,
    val projectType: String, // Mod, Modpack, Shader, Resource Pack
    val downloads: Long,
    val iconUrl: String?,
    val author: String,
    val categories: List<String>,
    val webUrl: String
)
