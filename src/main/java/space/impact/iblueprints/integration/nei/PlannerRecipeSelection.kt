package space.impact.iblueprints.integration.nei

import codechicken.nei.api.IOverlayHandler
import codechicken.nei.recipe.GuiCraftingRecipe
import codechicken.nei.recipe.IRecipeHandler
import cpw.mods.fml.common.Loader
import net.minecraft.inventory.Container
import space.impact.iblueprints.integration.ae2.MeBlueprintPlannerContainer
import space.impact.iblueprints.planner.PlannerSession
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.util.ChatComponentTranslation
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.planner.PlannerContainer

internal class PlannerRecipeSelection(private val entry: SceneEntry) : IOverlayHandler {

    override fun requireShiftForOverlayRecipe(): Boolean {
        return false
    }

    override fun overlayRecipe(
        firstGui: GuiContainer,
        recipe: IRecipeHandler,
        recipeIndex: Int,
        maxTransfer: Boolean,
    ) {
        select(firstGui)
    }

    override fun transferRecipe(
        firstGui: GuiContainer,
        recipe: IRecipeHandler,
        recipeIndex: Int,
        multiplier: Int,
    ): Int {
        return if (select(firstGui)) {
            1
        } else {
            0
        }
    }

    private fun select(gui: GuiContainer): Boolean {
        val planner = session(gui.inventorySlots) ?: return false
        val defaults = entry.channels.associate {
            it.id to it.minimum
        }
        val plan = try {
            entry.build(defaults).use {
                SceneBlueprint.capture(
                    scene = it,
                    channels = defaults,
                )
            }
        } catch (failure: Exception) {
            LogManager.getLogger("ImpactBlueprints").warn(
                "Cannot prepare planner structure ${entry.id}",
                failure,
            )
            null
        }
        if (plan == null) {
            gui.mc.thePlayer.addChatMessage(ChatComponentTranslation("iblueprints.planner.save_failed"))
            return false
        }
        return planner.select(
            id = entry.id,
            plan = plan,
        )
    }

    internal companion object {

        internal const val QUERY = "iblueprints.planner"

        internal fun session(container: Container): PlannerSession? {
            if (container is PlannerContainer) {
                return container.planner
            }
            if (Loader.isModLoaded("appliedenergistics2") && container is MeBlueprintPlannerContainer) {
                return container.planner
            }
            return null
        }

        internal fun open() {
            GuiCraftingRecipe.openRecipeGui(QUERY)
        }
    }
}
