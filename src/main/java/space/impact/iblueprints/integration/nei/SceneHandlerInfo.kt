package space.impact.iblueprints.integration.nei

import codechicken.nei.event.NEIRegisterHandlerInfosEvent
import codechicken.nei.recipe.GuiOverlayButton
import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.recipe.GuiRecipeButton
import codechicken.nei.recipe.HandlerInfo
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import space.impact.iblueprints.MODID
import space.impact.iblueprints.MODNAME
import space.impact.iblueprints.item.StructureBlueprintItem

/** Registered during client pre-init, including the first NEI handler-info event and later reloads. */
internal object SceneHandlerInfo {

    @SubscribeEvent
    public fun addPlannerTransfer(event: GuiRecipeButton.UpdateRecipeButtonsEvent.Post) {
        if (event.handlerRef.handler.handlerId != SceneRecipeHandler::class.java.name) {
            return
        }
        val gui = event.gui as? GuiRecipe<*> ?: return
        val planner = gui.firstScreen ?: return
        if (event.handlerRef.getOverlayHandler(planner) !is PlannerRecipeSelection) {
            return
        }
        val button = object : GuiOverlayButton(
            planner,
            event.handlerRef,
            event.width - GuiRecipeButton.BUTTON_WIDTH - 3,
            event.height - GuiRecipeButton.BUTTON_HEIGHT - 2,
        ) {
            init {
                displayString = "?"
            }

            override fun drawContent(
                minecraft: Minecraft,
                mouseX: Int,
                mouseY: Int,
                mouseOver: Boolean,
            ) {
                drawCenteredString(
                    minecraft.fontRenderer,
                    displayString,
                    xPosition + width / 2,
                    yPosition + (height - 8) / 2,
                    getTextColour(mouseOver),
                )
            }
        }
        event.buttonList.add(button)
    }

    @SubscribeEvent
    public fun register(event: NEIRegisterHandlerInfosEvent) {
        event.registerHandlerInfo(
            HandlerInfo.Builder(
                SceneRecipeHandler::class.java,
                MODNAME,
                MODID,
            )
                .setDisplayStack(ItemStack(StructureBlueprintItem))
                .setWidth(ScenePreviewLayout.WIDTH)
                .setHeight(ScenePreviewLayout.HEIGHT)
                .setShiftY(0)
                .setMultipleWidgetsAllowed(false)
                .setShowOverlayButton(false)
                .setShowFavoritesButton(false)
                .build(),
        )
    }
}
