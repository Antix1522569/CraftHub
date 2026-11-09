package com.example

import com.example.data.model.MinecraftServer
import com.example.data.model.PlayerProfile
import com.example.data.model.ServerEdition
import com.example.ui.components.MinecraftColorParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun minecraftColorParser_parsesColorsAndStyles() {
    val raw = "§aZielony §cCzerwony §lPogrubiony §rNormalny"
    val result = MinecraftColorParser.parse(raw)
    assertNotNull(result)
    assertTrue(result.text.contains("Zielony"))
    assertTrue(result.text.contains("Czerwony"))
    assertTrue(result.text.contains("Pogrubiony"))
    assertTrue(result.text.contains("Normalny"))
  }

  @Test
  fun minecraftServer_computesFullAddress() {
    val javaStandard = MinecraftServer(
      id = "1",
      name = "Java Std",
      address = "mc.example.com",
      port = 25565,
      edition = ServerEdition.JAVA
    )
    assertEquals("mc.example.com", javaStandard.fullAddress)

    val javaCustomPort = MinecraftServer(
      id = "2",
      name = "Java Custom",
      address = "mc.example.com",
      port = 25567,
      edition = ServerEdition.JAVA
    )
    assertEquals("mc.example.com:25567", javaCustomPort.fullAddress)

    val bedrockStandard = MinecraftServer(
      id = "3",
      name = "Bedrock Std",
      address = "play.example.com",
      port = 19132,
      edition = ServerEdition.BEDROCK
    )
    assertEquals("play.example.com", bedrockStandard.fullAddress)
  }

  @Test
  fun playerProfile_generatesAvatarAndRenderUrls() {
    val profile = PlayerProfile(
      id = "test_1",
      username = "StevePro",
      isOfflineAccount = true
    )
    assertTrue(profile.avatarUrl.contains("StevePro"))
    assertTrue(profile.bodyRenderUrl.contains("StevePro"))
    assertTrue(profile.isOfflineAccount)
  }
}
