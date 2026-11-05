package space.impact.gui3dengine

import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLClassLoader

public class OptionalIntegrationTest {

    @Test
    public fun engineAndClientProxyLoadWithoutIntegrationMods() {
        val urls = (javaClass.classLoader as URLClassLoader).getURLs()
        val absent = listOf(
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
                "proxy.ClientProxy",
            )) {
                val type = Class.forName(
                    "space.impact.gui3dengine.$name",
                    false,
                    loader,
                )
                assertTrue(type.declaredMethods.isNotEmpty())
            }
        }
    }
}
