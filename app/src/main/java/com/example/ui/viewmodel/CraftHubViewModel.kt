package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.EngineRelease
import com.example.data.model.MinecraftServer
import com.example.data.model.ModItem
import com.example.data.model.PlayerProfile
import com.example.data.model.ServerEdition
import com.example.data.network.MinecraftNetworkServices
import com.example.data.repository.EngineRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ServerFilter {
    ALL,
    NON_PREMIUM,
    BEDROCK,
    JAVA,
    FAVORITES
}

enum class NavigationTab(val title: String) {
    SERVERS("Serwery"),
    ENGINES("Silniki"),
    PROFILES("Profile"),
    MODS("Mody")
}

data class DirectPingState(
    val queryAddress: String = "",
    val edition: ServerEdition = ServerEdition.JAVA,
    val isTesting: Boolean = false,
    val result: MinecraftServer? = null,
    val errorMessage: String? = null
)

class CraftHubViewModel(application: Application) : AndroidViewModel(application) {

    private val networkServices = MinecraftNetworkServices()
    private val serverRepo = ServerRepository(application, networkServices)
    private val engineRepo = EngineRepository(networkServices)
    private val profileRepo = ProfileRepository(application)

    // Active Navigation
    private val _currentTab = MutableStateFlow(NavigationTab.SERVERS)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Server Filtering & Search
    private val _selectedFilter = MutableStateFlow(ServerFilter.ALL)
    val selectedFilter: StateFlow<ServerFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isRefreshingAll: StateFlow<Boolean> = serverRepo.isRefreshingAll

    // Filtered Servers
    val servers: StateFlow<List<MinecraftServer>> = combine(
        serverRepo.servers,
        _selectedFilter,
        _searchQuery
    ) { list, filter, query ->
        list.filter { server ->
            val matchesFilter = when (filter) {
                ServerFilter.ALL -> true
                ServerFilter.NON_PREMIUM -> server.isNonPremium
                ServerFilter.BEDROCK -> server.edition == ServerEdition.BEDROCK || server.edition == ServerEdition.HYBRID_GEYSER
                ServerFilter.JAVA -> server.edition == ServerEdition.JAVA || server.edition == ServerEdition.HYBRID_GEYSER
                ServerFilter.FAVORITES -> server.isFavorite
            }

            val matchesQuery = query.isBlank() ||
                server.name.contains(query, ignoreCase = true) ||
                server.address.contains(query, ignoreCase = true) ||
                server.software.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct Ping Tester
    private val _directPingState = MutableStateFlow(DirectPingState())
    val directPingState: StateFlow<DirectPingState> = _directPingState.asStateFlow()

    // Engines
    val engines: StateFlow<List<EngineRelease>> = engineRepo.engines
    val isEnginesLoading: StateFlow<Boolean> = engineRepo.isLoading

    // Profiles
    val profiles: StateFlow<List<PlayerProfile>> = profileRepo.profiles
    val activeProfile: StateFlow<PlayerProfile?> = profileRepo.activeProfile

    // Mods
    private val _mods = MutableStateFlow<List<ModItem>>(emptyList())
    val mods: StateFlow<List<ModItem>> = _mods.asStateFlow()

    private val _isModsLoading = MutableStateFlow(false)
    val isModsLoading: StateFlow<Boolean> = _isModsLoading.asStateFlow()

    private val _modSearchQuery = MutableStateFlow("")
    val modSearchQuery: StateFlow<String> = _modSearchQuery.asStateFlow()

    init {
        // Trigger initial server refresh and load mods
        serverRepo.refreshAll()
        searchMods("")
    }

    fun setTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun setFilter(filter: ServerFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshAllServers() {
        serverRepo.refreshAll()
    }

    fun refreshSingleServer(id: String) {
        serverRepo.refreshServer(id)
    }

    fun toggleFavorite(id: String) {
        serverRepo.toggleFavorite(id)
    }

    fun addCustomServer(
        name: String,
        address: String,
        port: Int,
        edition: ServerEdition,
        isNonPremium: Boolean
    ) {
        serverRepo.addServer(name, address, port, edition, isNonPremium)
    }

    fun deleteServer(id: String) {
        serverRepo.deleteServer(id)
    }

    // Direct Ping Tester methods
    fun updateDirectPingQuery(query: String) {
        _directPingState.update { it.copy(queryAddress = query) }
    }

    fun updateDirectPingEdition(edition: ServerEdition) {
        _directPingState.update { it.copy(edition = edition) }
    }

    fun runDirectPing() {
        val query = _directPingState.value.queryAddress.trim()
        if (query.isBlank()) return

        val parts = query.split(":")
        val host = parts[0]
        val port = if (parts.size > 1) {
            parts[1].toIntOrNull() ?: if (_directPingState.value.edition == ServerEdition.BEDROCK) 19132 else 25565
        } else {
            if (_directPingState.value.edition == ServerEdition.BEDROCK) 19132 else 25565
        }

        _directPingState.update { it.copy(isTesting = true, errorMessage = null, result = null) }

        viewModelScope.launch {
            try {
                val dummy = MinecraftServer(
                    id = "test_ping",
                    name = host,
                    address = host,
                    port = port,
                    edition = _directPingState.value.edition,
                    isNonPremium = true
                )
                val result = networkServices.pingServer(dummy)
                _directPingState.update { it.copy(isTesting = false, result = result) }
            } catch (e: Exception) {
                _directPingState.update {
                    it.copy(isTesting = false, errorMessage = "Nie udało się połączyć: ${e.localizedMessage}")
                }
            }
        }
    }

    // Engine updates
    fun refreshEngines() {
        engineRepo.refreshEngines()
    }

    // Profile actions
    fun addProfile(username: String, isOffline: Boolean) {
        profileRepo.addProfile(username, isOffline)
    }

    fun selectActiveProfile(id: String) {
        profileRepo.selectActiveProfile(id)
    }

    fun deleteProfile(id: String) {
        profileRepo.deleteProfile(id)
    }

    // Mods
    fun setModSearchQuery(query: String) {
        _modSearchQuery.value = query
        searchMods(query)
    }

    fun searchMods(query: String) {
        _isModsLoading.value = true
        viewModelScope.launch {
            try {
                _mods.value = networkServices.searchModrinth(query)
            } finally {
                _isModsLoading.value = false
            }
        }
    }

    // Helper Actions
    fun copyServerAddress(context: Context, server: MinecraftServer) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Server IP", server.fullAddress)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Skopiowano IP: ${server.fullAddress}", Toast.LENGTH_SHORT).show()
    }

    /**
     * Launch or configure in Minecraft Bedrock directly if installed on the device.
     */
    fun launchBedrockServer(context: Context, server: MinecraftServer) {
        val port = if (server.edition == ServerEdition.BEDROCK) server.port else 19132
        val uriString = "minecraft://?addExternalServer=${Uri.encode(server.name)}:${server.address}:$port"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // If Minecraft is not installed, copy address and open store/browser
            copyServerAddress(context, server)
            Toast.makeText(
                context,
                "Nie znaleziono Minecraft Bedrock. Skopiowano IP serwera!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Share multiplayer invitation with server details, friend gamertag, and connect guide.
     */
    fun shareServerInvite(context: Context, server: MinecraftServer) {
        val activeNick = activeProfile.value?.username ?: "Gracz"
        val accountType = if (activeProfile.value?.isOfflineAccount == false) "Konto Microsoft / Xbox" else "Nick offline"
        
        val inviteText = """
            🎮 Dołącz do mnie w Minecraft!
            
            📍 Serwer: ${server.name}
            🌐 Adres IP: ${server.fullAddress}
            ⚙️ Wersja: ${server.version} (${server.edition.displayName})
            🛡️ Tryb: ${if (server.isNonPremium) "Obsługuje darmowe konta offline & Non-Premium" else "Wymagane konto"}
            👤 Mój nick: $activeNick ($accountType)
            
            Dołącz przez CraftHub lub wpisz IP w grze!
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Zaproszenie na serwer Minecraft")
            putExtra(Intent.EXTRA_TEXT, inviteText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Zaproś znajomych do gry"))
    }
}
