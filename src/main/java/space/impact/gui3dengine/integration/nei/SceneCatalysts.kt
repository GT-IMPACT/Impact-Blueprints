package space.impact.gui3dengine.integration.nei

import codechicken.nei.PositionedStack
import codechicken.nei.recipe.GuiRecipe
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import net.minecraft.client.Minecraft

/** NEI's global catalyst table is per category. This changes only the current screen's native panel. */
internal object SceneCatalysts {

    @SubscribeEvent
    public fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) {
            return
        }
        val gui = Minecraft.getMinecraft().currentScreen as? GuiRecipe<*> ?: return
        if (gui.handler !is SceneRecipeHandler) {
            return
        }
        // GuiRecipe.updateScreen first resets this panel from its category-wide table.
        // Restore the visible recipe after that reset, before the next input/render cycle.
        update(
            gui = gui,
            recipe = gui.recipeIndices.firstOrNull(),
        )
    }

    internal fun update(
        gui: GuiRecipe<*>,
        recipe: Int?,
    ) {
        val handler = gui.handler as? SceneRecipeHandler ?: return
        val controller = recipe?.let {
            handler.controller(it)
        }
        val catalysts = if (controller == null) {
            emptyList()
        } else {
            listOf(PositionedStack(
                controller,
                0,
                0,
                false,
            ))
        }
        val panel = gui.recipeCatalystWidget
        val right = panel.x + panel.w
        // NEI keeps the old child widgets when setCatalysts receives an empty list.
        if (catalysts.isEmpty()) {
            panel.clear()
        }
        panel.setCatalysts(catalysts)
        panel.x = right - panel.w
        panel.update()
    }
}
