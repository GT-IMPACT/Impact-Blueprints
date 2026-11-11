package space.impact.iblueprints

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLClassLoader

public class OptionalIntegrationTest {

    @Test
    public fun engineAndClientProxyLoadWithoutIntegrationMods() {
        val urls = (javaClass.classLoader as URLClassLoader).getURLs()
        val absent = listOf(
            "appeng.",
            "mcp.mobius.waila.",
            "gregtech.",
            "space.impact.structures.",
            "codechicken.",
            "com.cleanroommc.modularui.",
            "com.gtnewhorizons.angelica.",
        )
        val loader = object : URLClassLoader(
            urls,
            null,
        ) {

            override fun loadClass(
                name: String,
                resolve: Boolean,
            ): Class<*> {
                if (absent.any(name::startsWith)) {
                    throw ClassNotFoundException("Optional integration is absent: $name")
                }
                return super.loadClass(
                    name,
                    resolve,
                )
            }
        }
        loader.use {
            for (name in listOf(
                "api.SceneEntry",
                "api.SceneRegistry",
                "scene.BlockScene",
                "scene.SceneView",
                "proxy.CommonProxy",
                "proxy.ClientProxy",
                "item.StructureBlueprintItem",
            )) {
                val type = Class.forName(
                    "space.impact.iblueprints.$name",
                    false,
                    loader,
                )
                assertTrue(type.declaredMethods.isNotEmpty())
            }
        }
    }
}
