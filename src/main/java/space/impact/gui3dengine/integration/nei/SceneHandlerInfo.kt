package space.impact.gui3dengine.integration.nei

import codechicken.nei.event.NEIRegisterHandlerInfosEvent
import codechicken.nei.recipe.HandlerInfo
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import space.impact.gui3dengine.MODID
import space.impact.gui3dengine.MODNAME

/** Registered during client pre-init, including the first NEI handler-info event and later reloads. */
internal object SceneHandlerInfo {

    @SubscribeEvent
    public fun register(event: NEIRegisterHandlerInfosEvent) {
        event.registerHandlerInfo(
            HandlerInfo.Builder(
                SceneRecipeHandler::class.java,
                MODNAME,
                MODID,
            )
                .setDisplayStack(ItemStack(Blocks.glass))
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
