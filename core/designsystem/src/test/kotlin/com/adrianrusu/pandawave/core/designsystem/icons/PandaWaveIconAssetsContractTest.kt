package com.adrianrusu.pandawave.core.designsystem.icons

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PandaWaveIconAssetsContractTest {
    private val drawableDir =
        File(
            requireNotNull(System.getProperty("pandawave.rootDir")),
            "core/designsystem/src/main/res/drawable"
        )

    private val migrationIcons =
        listOf(
            "account_circle", "album", "bolt", "eco", "favorite_border", "graphic_eq",
            "home", "library_music", "mic", "pause", "play_arrow", "play_circle_outline",
            "queue_music", "search", "settings", "shuffle", "skip_next", "skip_previous",
            "spa", "volume_down", "volume_up"
        )

    @Test
    fun `migration assets preserve dimensions tint ownership and RTL behavior`() {
        val androidNamespace = "http://schemas.android.com/apk/res/android"
        val mirroredIcons = setOf("queue_music", "volume_down", "volume_up")
        val parser = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()

        migrationIcons.forEach { name ->
            val file = File(drawableDir, "pandawave_ic_$name.xml")
            assertTrue(file.isFile, "Missing icon: $name")
            val vector = parser.parse(file).documentElement
            assertEquals("vector", vector.tagName, name)
            assertEquals("24dp", vector.getAttributeNS(androidNamespace, "width"), name)
            assertEquals("24dp", vector.getAttributeNS(androidNamespace, "height"), name)
            assertEquals("24", vector.getAttributeNS(androidNamespace, "viewportWidth"), name)
            assertEquals("24", vector.getAttributeNS(androidNamespace, "viewportHeight"), name)
            assertFalse(vector.hasAttributeNS(androidNamespace, "tint"), name)
            assertEquals(
                name in mirroredIcons,
                vector.getAttributeNS(androidNamespace, "autoMirrored") == "true",
                name
            )
            assertTrue(vector.getElementsByTagName("path").length > 0, "Empty icon: $name")
        }
    }
}
