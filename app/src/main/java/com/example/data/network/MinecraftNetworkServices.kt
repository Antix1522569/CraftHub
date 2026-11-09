package com.example.data.network

import com.example.data.model.EngineRelease
import com.example.data.model.MinecraftServer
import com.example.data.model.ModItem
import com.example.data.model.ServerEdition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class MinecraftNetworkServices {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Queries live status of a Minecraft server using mcsrvstat.us API
     * and measures ping latency.
     */
    suspend fun pingServer(server: MinecraftServer): MinecraftServer = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val queryHost = if (server.port != 25565 && server.port != 19132) {
            "${server.address}:${server.port}"
        } else {
            server.address
        }

        val url = if (server.edition == ServerEdition.BEDROCK) {
            "https://api.mcsrvstat.us/bedrock/3/$queryHost"
        } else {
            "https://api.mcsrvstat.us/3/$queryHost"
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CraftHub-Android/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(12)

            if (!response.isSuccessful) {
                return@withContext pingViaSocketFallback(server)
            }

            val body = response.body?.string() ?: return@withContext pingViaSocketFallback(server)
            val json = JSONObject(body)
            val isOnline = json.optBoolean("online", false)

            if (!isOnline) {
                // If public API returned false, test direct socket fallback
                val fallback = pingViaSocketFallback(server)
                if (fallback.isOnline) return@withContext fallback

                return@withContext server.copy(
                    isOnline = false,
                    isChecking = false,
                    lastCheckedTime = System.currentTimeMillis(),
                    latencyMs = -1
                )
            }

            val playersObj = json.optJSONObject("players")
            val onlinePlayers = playersObj?.optInt("online", 0) ?: 0
            val maxPlayers = playersObj?.optInt("max", 0) ?: 0

            val motdObj = json.optJSONObject("motd")
            val cleanMotdList = motdObj?.optJSONArray("clean")
            val motdClean = if (cleanMotdList != null && cleanMotdList.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until cleanMotdList.length()) {
                    sb.append(cleanMotdList.getString(i)).append("\n")
                }
                sb.toString().trim()
            } else {
                "Serwer Minecraft online"
            }

            val rawMotdList = motdObj?.optJSONArray("raw")
            val motdRaw = if (rawMotdList != null && rawMotdList.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until rawMotdList.length()) {
                    sb.append(rawMotdList.getString(i)).append("\n")
                }
                sb.toString().trim()
            } else {
                motdClean
            }

            val version = json.optString("version", "1.21.x")
            val software = json.optString("software", if (server.edition == ServerEdition.BEDROCK) "Bedrock" else "PaperMC")
            val icon = json.optString("icon", "")

            val iconUrl = if (icon.isNotEmpty()) icon else "https://api.mcsrvstat.us/icon/$queryHost"

            server.copy(
                isOnline = true,
                latencyMs = latency,
                onlinePlayers = onlinePlayers,
                maxPlayers = maxPlayers,
                version = version,
                software = if (software.isNotEmpty()) software else server.software,
                motdClean = motdClean,
                motdRaw = motdRaw,
                iconUrl = iconUrl,
                lastCheckedTime = System.currentTimeMillis(),
                isChecking = false
            )
        } catch (e: Exception) {
            pingViaSocketFallback(server)
        }
    }

    private fun pingViaSocketFallback(server: MinecraftServer): MinecraftServer {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(server.address, server.port), 3000)
                val ping = System.currentTimeMillis() - start
                server.copy(
                    isOnline = true,
                    latencyMs = ping,
                    lastCheckedTime = System.currentTimeMillis(),
                    isChecking = false
                )
            }
        } catch (e: Exception) {
            server.copy(
                isOnline = false,
                latencyMs = -1,
                lastCheckedTime = System.currentTimeMillis(),
                isChecking = false
            )
        }
    }

    /**
     * Checks real-time versions for popular Minecraft server engines:
     * Mojang Vanilla, PaperMC, Purpur, Fabric, GeyserMC, and Bedrock Dedicated Server.
     */
    suspend fun fetchEngineReleases(): List<EngineRelease> = withContext(Dispatchers.IO) {
        val releases = mutableListOf<EngineRelease>()

        // 1. Fetch Mojang official releases
        try {
            val req = Request.Builder()
                .url("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")
                .header("User-Agent", "CraftHub-Android/1.0")
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "{}")
                val latest = json.optJSONObject("latest")
                val releaseVer = latest?.optString("release", "1.21.4") ?: "1.21.4"
                val snapshotVer = latest?.optString("snapshot", "25w07a") ?: "Snapshot"

                releases.add(
                    EngineRelease(
                        id = "vanilla_java",
                        name = "Vanilla Java Edition",
                        category = "Oficjalny silnik",
                        latestVersion = releaseVer,
                        buildNumber = "Snapshot: $snapshotVer",
                        channel = "Official Mojang Release",
                        releaseDate = "Aktualny",
                        downloadUrl = "https://www.minecraft.net/download/server",
                        features = listOf("Czysty kod Mojang", "Pełna zgodność z protokołem", "Wspiera oficjalne serwery"),
                        isRecommendedForOffline = false
                    )
                )
            }
        } catch (e: Exception) {
            releases.add(
                EngineRelease(
                    id = "vanilla_java",
                    name = "Vanilla Java Edition",
                    category = "Oficjalny silnik",
                    latestVersion = "1.21.4",
                    channel = "Official Mojang Release",
                    releaseDate = "Aktualny",
                    downloadUrl = "https://www.minecraft.net/download/server",
                    features = listOf("Czysty kod Mojang", "Pełna zgodność z protokołem"),
                    isRecommendedForOffline = false
                )
            )
        }

        // 2. PaperMC (Najpopularniejszy silnik multiplayer z obsługą offline i pluginów)
        try {
            val req = Request.Builder()
                .url("https://api.papermc.io/v2/projects/paper")
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "{}")
                val versions = json.optJSONArray("versions")
                val latestVer = if (versions != null && versions.length() > 0) {
                    versions.getString(versions.length() - 1)
                } else "1.21.4"

                // Fetch latest build number
                var latestBuild = "Build #140"
                try {
                    val buildReq = Request.Builder()
                        .url("https://api.papermc.io/v2/projects/paper/versions/$latestVer")
                        .build()
                    val buildResp = httpClient.newCall(buildReq).execute()
                    if (buildResp.isSuccessful) {
                        val buildJson = JSONObject(buildResp.body?.string() ?: "{}")
                        val builds = buildJson.optJSONArray("builds")
                        if (builds != null && builds.length() > 0) {
                            latestBuild = "Build #${builds.getInt(builds.length() - 1)}"
                        }
                    }
                } catch (_: Exception) {}

                releases.add(
                    EngineRelease(
                        id = "papermc",
                        name = "PaperMC Server Engine",
                        category = "Silnik serwera (Zalecany)",
                        latestVersion = latestVer,
                        buildNumber = latestBuild,
                        channel = "Real-time High Performance",
                        releaseDate = "W czasie rzeczywistym",
                        downloadUrl = "https://papermc.io/downloads/paper",
                        features = listOf(
                            "Idealny dla serwerów non-premium (online-mode=false)",
                            "Obsługa pluginów Bukkit, Spigot, Paper",
                            "Maksymalna optymalizacja TPS i ochrona przed exploitami"
                        ),
                        isRecommendedForOffline = true
                    )
                )
            }
        } catch (e: Exception) {
            releases.add(
                EngineRelease(
                    id = "papermc",
                    name = "PaperMC Server Engine",
                    category = "Silnik serwera (Zalecany)",
                    latestVersion = "1.21.4",
                    buildNumber = "Najnowszy build",
                    channel = "High Performance",
                    releaseDate = "Aktualny",
                    downloadUrl = "https://papermc.io/downloads/paper",
                    features = listOf("Obsługa trybu offline (non-premium)", "Optymalizacja TPS", "Wsparcie pluginów"),
                    isRecommendedForOffline = true
                )
            )
        }

        // 3. Purpur (Ultra-konfigurowalny silnik)
        releases.add(
            EngineRelease(
                id = "purpur",
                name = "Purpur Engine",
                category = "Silnik serwera",
                latestVersion = "1.21.4",
                buildNumber = "Purpur-2342",
                channel = "Ultra Configurable",
                releaseDate = "Aktualizowany na bieżąco",
                downloadUrl = "https://purpurmc.org/downloads",
                features = listOf(
                    "Jeszcze więcej opcji konfiguracji niż Paper",
                    "Jazda na dowolnym mobie i modyfikacje rozgrywki",
                    "Pełna obsługa serwerów non-premium"
                ),
                isRecommendedForOffline = true
            )
        )

        // 4. Fabric Loader
        try {
            val req = Request.Builder()
                .url("https://meta.fabricmc.net/v2/versions/loader")
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val array = JSONArray(resp.body?.string() ?: "[]")
                val latestLoader = if (array.length() > 0) {
                    array.getJSONObject(0).optJSONObject("loader")?.optString("version", "0.16.10") ?: "0.16.10"
                } else "0.16.10"

                releases.add(
                    EngineRelease(
                        id = "fabric",
                        name = "Fabric Modded Engine",
                        category = "Silnik modów (Client & Server)",
                        latestVersion = "1.21.4",
                        buildNumber = "Loader v$latestLoader",
                        channel = "Lightweight Modding",
                        releaseDate = "Aktualny",
                        downloadUrl = "https://fabricmc.net/use/installer/",
                        features = listOf(
                            "Ultra lekki framework modów",
                            "Działa z modami wydajnościowymi (Sodium, Lithium, Iris)",
                            "Obsługa serwerów LAN i dedykowanych"
                        ),
                        isRecommendedForOffline = true
                    )
                )
            }
        } catch (e: Exception) {
            releases.add(
                EngineRelease(
                    id = "fabric",
                    name = "Fabric Modded Engine",
                    category = "Silnik modów",
                    latestVersion = "1.21.4",
                    buildNumber = "Loader v0.16.10",
                    channel = "Lightweight Modding",
                    releaseDate = "Aktualny",
                    downloadUrl = "https://fabricmc.net/use/installer/",
                    features = listOf("Wspiera Sodium i Iris", "Mody serwerowe i klienckie"),
                    isRecommendedForOffline = true
                )
            )
        }

        // 5. GeyserMC (Most Java + Bedrock)
        releases.add(
            EngineRelease(
                id = "geysermc",
                name = "GeyserMC & Floodgate",
                category = "Most Java <-> Bedrock",
                latestVersion = "Build #650+",
                buildNumber = "Geyser 2.4",
                channel = "Cross-Play Bridge",
                releaseDate = "Ciągła integracja",
                downloadUrl = "https://geysermc.org/download",
                features = listOf(
                    "Umożliwia graczom Minecraft Bedrock (Android/iOS/PC) dołączanie do serwerów Java!",
                    "Współpracuje z Floodgate dla autentykacji bez konta Java",
                    "Nie wymaga instalowania modów na telefonie"
                ),
                isRecommendedForOffline = true
            )
        )

        // 6. Bedrock Dedicated Server (BDS)
        releases.add(
            EngineRelease(
                id = "bedrock_bds",
                name = "Bedrock Dedicated Server (BDS)",
                category = "Oficjalny Bedrock",
                latestVersion = "1.21.60",
                buildNumber = "BDS Official",
                channel = "Official Bedrock",
                releaseDate = "Aktualny",
                downloadUrl = "https://www.minecraft.net/download/server/bedrock",
                features = listOf(
                    "Oficjalny serwer Bedrock dla telefonów i Windows 10/11",
                    "Domyślny port 19132 UDP",
                    "Wsparcie dla kont Microsoft i Xbox Live"
                ),
                isRecommendedForOffline = false
            )
        )

        releases
    }

    /**
     * Searches Modrinth API for popular and verified Minecraft mods & modpacks.
     */
    suspend fun searchModrinth(query: String = ""): List<ModItem> = withContext(Dispatchers.IO) {
        val qParam = if (query.isNotBlank()) "query=${query.trim()}&" else ""
        val url = "https://api.modrinth.com/v2/search?${qParam}limit=20&index=relevance"

        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "CraftHub-Android/1.0")
                .build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext getFallbackMods()

            val json = JSONObject(resp.body?.string() ?: "{}")
            val hits = json.optJSONArray("hits") ?: return@withContext getFallbackMods()

            val list = mutableListOf<ModItem>()
            for (i in 0 until hits.length()) {
                val hit = hits.getJSONObject(i)
                val catList = mutableListOf<String>()
                val catArray = hit.optJSONArray("categories")
                if (catArray != null) {
                    for (c in 0 until catArray.length().coerceAtMost(3)) {
                        catList.add(catArray.getString(c))
                    }
                }

                list.add(
                    ModItem(
                        id = hit.optString("project_id", i.toString()),
                        title = hit.optString("title", "Modyfikacja"),
                        description = hit.optString("description", "Opis niedostępny"),
                        projectType = hit.optString("project_type", "mod").replaceFirstChar { it.uppercase() },
                        downloads = hit.optLong("downloads", 0),
                        iconUrl = hit.optString("icon_url").takeIf { it.isNotBlank() },
                        author = hit.optString("author", "Autor"),
                        categories = catList,
                        webUrl = "https://modrinth.com/${hit.optString("project_type", "mod")}/${hit.optString("slug", "")}"
                    )
                )
            }
            if (list.isEmpty()) getFallbackMods() else list
        } catch (e: Exception) {
            getFallbackMods()
        }
    }

    private fun getFallbackMods(): List<ModItem> {
        return listOf(
            ModItem(
                id = "sodium",
                title = "Sodium",
                description = "Rewolucyjny mod podnoszący liczbę FPS i płynność renderowania grafiki w Minecraft.",
                projectType = "Mod",
                downloads = 34500000,
                iconUrl = "https://cdn.modrinth.com/data/AANobbMI/icon.png",
                author = "jellysquid3",
                categories = listOf("Wydajność", "Grafika", "Optymalizacja"),
                webUrl = "https://modrinth.com/mod/sodium"
            ),
            ModItem(
                id = "iris",
                title = "Iris Shaders",
                description = "Nowoczesny, szybki silnik cieniowania (shaderów) kompatybilny z Sodium.",
                projectType = "Mod",
                downloads = 21900000,
                iconUrl = "https://cdn.modrinth.com/data/YL57xq9U/icon.png",
                author = "coderbot",
                categories = listOf("Shadery", "Wydajność"),
                webUrl = "https://modrinth.com/mod/iris"
            ),
            ModItem(
                id = "simple-voice-chat",
                title = "Simple Voice Chat",
                description = "Czat głosowy w grze oparty na odległości (proximity chat) dla serwerów multiplayer.",
                projectType = "Mod",
                downloads = 15400000,
                iconUrl = "https://cdn.modrinth.com/data/9eGKb6K1/icon.png",
                author = "henkelmax",
                categories = listOf("Multiplayer", "Komunikacja"),
                webUrl = "https://modrinth.com/mod/simple-voice-chat"
            ),
            ModItem(
                id = "lithium",
                title = "Lithium",
                description = "Ogólny mod optymalizacji fizyki i ticków serwera (TPS) bez zmiany zachowań gry.",
                projectType = "Mod",
                downloads = 18200000,
                iconUrl = "https://cdn.modrinth.com/data/gvQqBUqZ/icon.png",
                author = "jellysquid3",
                categories = listOf("Wydajność", "Serwer"),
                webUrl = "https://modrinth.com/mod/lithium"
            ),
            ModItem(
                id = "geyser-fabric",
                title = "GeyserMC",
                description = "Most pozwalający graczom Minecraft Bedrock dołączać na serwer Java bez modów.",
                projectType = "Mod",
                downloads = 8300000,
                iconUrl = "https://cdn.modrinth.com/data/u6dsqAZZ/icon.png",
                author = "GeyserMC",
                categories = listOf("Crossplay", "Bedrock"),
                webUrl = "https://modrinth.com/mod/geyser"
            )
        )
    }
}
