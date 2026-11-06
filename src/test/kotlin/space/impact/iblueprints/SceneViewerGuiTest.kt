package space.impact.iblueprints

import codechicken.nei.util.EmptyContainer
import com.cleanroommc.modularui.screen.ModularContainer
import java.lang.reflect.Constructor
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.client.gui.GuiScreen
import org.junit.Assert.assertSame
import org.junit.Test
import space.impact.iblueprints.integration.modularui.SceneViewerGui

public class SceneViewerGuiTest {

    @Test
    public fun initializationRestoresThePlannerAfterRecipeScreens() {
        // Exercise vanilla initGui without a display, network connection or ModularUI event callbacks.
        val client = allocate(Minecraft::class.java)
        val player = allocate(EntityClientPlayerMP::class.java)
        client.thePlayer = player
        val viewer = allocate(SceneViewerGui::class.java)
        val clientField = GuiScreen::class.java.getDeclaredField("mc")
        clientField.isAccessible = true
        clientField.set(
            viewer,
            client,
        )
        viewer.inventorySlots = ModularContainer()
        val planner = ModularContainer()
        val parentField = SceneViewerGui::class.java.getDeclaredField("parentContainer")
        parentField.isAccessible = true
        parentField.set(
            viewer,
            planner,
        )
        for (activeContainer in listOf(
            planner,
            planner,
            EmptyContainer(),
            EmptyContainer(),
            planner,
        )) {
            player.openContainer = activeContainer
            viewer.width += 100
            viewer.height += 50
            viewer.initGui()
            assertSame(
                planner,
                player.openContainer,
            )
        }
    }

    private fun <T> allocate(type: Class<T>): T {
        val factoryClass = Class.forName("sun.reflect.ReflectionFactory")
        val factory = factoryClass.getMethod("getReflectionFactory").invoke(null)
        val constructor = factoryClass.getMethod(
            "newConstructorForSerialization",
            Class::class.java,
            Constructor::class.java,
        ).invoke(
            factory,
            type,
            Any::class.java.getDeclaredConstructor(),
        ) as Constructor<*>
        constructor.isAccessible = true
        return type.cast(constructor.newInstance())
    }
}
