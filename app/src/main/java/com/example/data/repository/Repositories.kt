package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.EngineRelease
import com.example.data.model.MinecraftServer
import com.example.data.model.PlayerProfile
import com.example.data.model.ServerEdition
import com.example.data.network.MinecraftNetworkServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ServerRepository(
    private val context: Context,
    private val networkServices: MinecraftNetworkServices = MinecraftNetworkServices()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("crafthub_servers", Context.MODE_PRIVATE)
    private val _servers = MutableStateFlow<List<MinecraftServer>>(emptyList())
    val servers: StateFlow<List<MinecraftServer>> = _servers.asStateFlow()

    private val _isRefreshingAll = MutableStateFlow(false)
    val isRefreshingAll: StateFlow<Boolean> = _isRefreshingAll.asStateFlow()

    init {
        loadServers()
    }

    private fun loadServers() {
        val savedJson = prefs.getString("custom_servers", null)
        val customList = mutableListOf<MinecraftServer>()

        if (savedJson != null) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    customList.add(
                        MinecraftServer(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            address = obj.getString("address"),
                            port = obj.getInt("port"),
                            edition = ServerEdition.valueOf(obj.optString("edition", "JAVA")),
                            isNonPremium = obj.optBoolean("isNonPremium", true),
                            isCustom = true,
                            isFavorite = obj.optBoolean("isFavorite", false),
                            software = obj.optString("software", "PaperMC")
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // Combine curated non-premium and hybrid servers with user servers
        val initialList = getCuratedServers() + customList
        _servers.value = initialList
    }

    private fun saveCustomServers() {
        val customOnly = _servers.value.filter { it.isCustom }
        val array = JSONArray()
        for (server in customOnly) {
            val obj = JSONObject()
            obj.put("id", server.id)
            obj.put("name", server.name)
            obj.put("address", server.address)
            obj.put("port", server.port)
            obj.put("edition", server.edition.name)
            obj.put("isNonPremium", server.isNonPremium)
            obj.put("isFavorite", server.isFavorite)
            obj.put("software", server.software)
            array.put(obj)
        }
        prefs.edit().putString("custom_servers", array.toString()).apply()
    }

    private fun getCuratedServers(): List<MinecraftServer> {
        return listOf(
            MinecraftServer(
                id = "curated_craftpolska",
                name = "MC-Polska Survival & PvP",
                address = "mc.craftpoland.pl",
                port = 25565,
                edition = ServerEdition.HYBRID_GEYSER,
                isNonPremium = true,
                isCustom = false,
                isFavorite = true,
                software = "PaperMC + Geyser",
                motdClean = "Serwer Survival + Działki | Non-Premium & Premium | Java + Bedrock (Port 19132)"
            ),
            MinecraftServer(
                id = "curated_mineglobe",
                name = "AuraMC Non-Premium Network",
                address = "play.auramc.net",
                port = 25565,
                edition = ServerEdition.JAVA,
                isNonPremium = true,
                isCustom = false,
                isFavorite = false,
                software = "Purpur",
                motdClean = "BedWars, SkyBlock & Survival | Darmowe konta offline mile widziane!"
            ),
            MinecraftServer(
                id = "curated_bedrock_hub",
                name = "Bedrock & Mobile CrossHub",
                address = "bedrock.geysermc.org",
                port = 19132,
                edition = ServerEdition.BEDROCK,
                isNonPremium = true,
                isCustom = false,
                isFavorite = true,
                software = "GeyserMC Standalone",
                motdClean = "Oficjalny hub testowy GeyserMC dla graczy mobilnych i konsolowych."
            ),
            MinecraftServer(
                id = "curated_anarchy",
                name = "WildCraft Anarchia & Gildie",
                address = "wildcraft.pl",
                port = 25565,
                edition = ServerEdition.JAVA,
                isNonPremium = true,
                isCustom = false,
                isFavorite = false,
                software = "PaperMC",
                motdClean = "Tryb offline włączony | Autoryzacja /register i /login | Brak lagów"
            )
        )
    }

    fun addServer(
        name: String,
        address: String,
        port: Int,
        edition: ServerEdition,
        isNonPremium: Boolean
    ) {
        val newServer = MinecraftServer(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { address },
            address = address.trim(),
            port = port,
            edition = edition,
            isNonPremium = isNonPremium,
            isCustom = true,
            isFavorite = false
        )
        _servers.update { it + newServer }
        saveCustomServers()
        refreshServer(newServer.id)
    }

    fun deleteServer(id: String) {
        _servers.update { list -> list.filterNot { it.id == id && it.isCustom } }
        saveCustomServers()
    }

    fun toggleFavorite(id: String) {
        _servers.update { list ->
            list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it }
        }
        saveCustomServers()
    }

    fun refreshServer(id: String) {
        val target = _servers.value.find { it.id == id } ?: return
        _servers.update { list ->
            list.map { if (it.id == id) it.copy(isChecking = true) else it }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val updated = networkServices.pingServer(target)
            _servers.update { list ->
                list.map { if (it.id == id) updated else it }
            }
        }
    }

    fun refreshAll() {
        if (_isRefreshingAll.value) return
        _isRefreshingAll.value = true

        _servers.update { list -> list.map { it.copy(isChecking = true) } }

        CoroutineScope(Dispatchers.IO).launch {
            val currentList = _servers.value
            currentList.forEach { server ->
                val updated = networkServices.pingServer(server)
                _servers.update { list ->
                    list.map { if (it.id == server.id) updated else it }
                }
            }
            _isRefreshingAll.value = false
        }
    }
}

class EngineRepository(
    private val networkServices: MinecraftNetworkServices = MinecraftNetworkServices()
) {
    private val _engines = MutableStateFlow<List<EngineRelease>>(emptyList())
    val engines: StateFlow<List<EngineRelease>> = _engines.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshEngines()
    }

    fun refreshEngines() {
        if (_isLoading.value) return
        _isLoading.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = networkServices.fetchEngineReleases()
                _engines.value = list
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class ProfileRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("crafthub_profiles", Context.MODE_PRIVATE)
    private val _profiles = MutableStateFlow<List<PlayerProfile>>(emptyList())
    val profiles: StateFlow<List<PlayerProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<PlayerProfile?>(null)
    val activeProfile: StateFlow<PlayerProfile?> = _activeProfile.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val saved = prefs.getString("profiles_list", null)
        val list = mutableListOf<PlayerProfile>()

        if (saved != null) {
            try {
                val array = JSONArray(saved)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PlayerProfile(
                            id = obj.getString("id"),
                            username = obj.getString("username"),
                            isOfflineAccount = obj.optBoolean("isOfflineAccount", true),
                            isDefault = obj.optBoolean("isDefault", false),
                            skinType = obj.optString("skinType", "classic"),
                            createdTimestamp = obj.optLong("createdTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        if (list.isEmpty()) {
            val defaultGuest = PlayerProfile(
                id = "default_guest",
                username = "Steve_Player",
                isOfflineAccount = true,
                isDefault = true
            )
            val gamerPro = PlayerProfile(
                id = "gamer_pro",
                username = "AlexCraft",
                isOfflineAccount = false, // Microsoft / Xbox Gamertag
                isDefault = false
            )
            list.add(defaultGuest)
            list.add(gamerPro)
        }

        _profiles.value = list
        _activeProfile.value = list.firstOrNull { it.isDefault } ?: list.firstOrNull()
        saveProfiles()
    }

    private fun saveProfiles() {
        val array = JSONArray()
        for (p in _profiles.value) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("username", p.username)
            obj.put("isOfflineAccount", p.isOfflineAccount)
            obj.put("isDefault", p.isDefault)
            obj.put("skinType", p.skinType)
            obj.put("createdTimestamp", p.createdTimestamp)
            array.put(obj)
        }
        prefs.edit().putString("profiles_list", array.toString()).apply()
    }

    fun addProfile(username: String, isOffline: Boolean) {
        val trimmed = username.trim().replace(" ", "_")
        if (trimmed.isBlank()) return

        val newProfile = PlayerProfile(
            id = UUID.randomUUID().toString(),
            username = trimmed,
            isOfflineAccount = isOffline,
            isDefault = _profiles.value.isEmpty()
        )

        _profiles.update { it + newProfile }
        if (newProfile.isDefault || _activeProfile.value == null) {
            _activeProfile.value = newProfile
        }
        saveProfiles()
    }

    fun selectActiveProfile(id: String) {
        _profiles.update { list ->
            list.map { it.copy(isDefault = it.id == id) }
        }
        _activeProfile.value = _profiles.value.find { it.id == id }
        saveProfiles()
    }

    fun deleteProfile(id: String) {
        if (_profiles.value.size <= 1) return // Keep at least one
        _profiles.update { list -> list.filterNot { it.id == id } }
        if (_activeProfile.value?.id == id) {
            _activeProfile.value = _profiles.value.firstOrNull()
            _activeProfile.value?.let { active ->
                selectActiveProfile(active.id)
            }
        }
        saveProfiles()
    }
}
