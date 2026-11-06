@file:Suppress("unused")

package space.impact.iblueprints.proxy

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.event.FMLLoadCompleteEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.MinecraftForge
import space.impact.iblueprints.integration.ae2.MeBlueprintPlannerItem
import space.impact.iblueprints.integration.ae2.PlannerPatterns
import space.impact.iblueprints.integration.ae2.StructurePatternItem
import space.impact.iblueprints.integration.gregtech.GregTechScenes
import space.impact.iblueprints.integration.nei.SceneHandlerInfo
import space.impact.iblueprints.integration.nei.SceneCatalysts
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.item.StructureBlueprintRenderer

public class ClientProxy : CommonProxy() {

    override fun preInit(event: FMLPreInitializationEvent) {
        super.preInit(event)
        MinecraftForgeClient.registerItemRenderer(
            StructureBlueprintItem,
            StructureBlueprintRenderer(),
        )
        if (Loader.isModLoaded("modularui2") && Loader.isModLoaded("appliedenergistics2")) {
            MeBlueprintPlannerItem.registerRenderer()
            MinecraftForgeClient.registerItemRenderer(
                StructurePatternItem,
                StructureBlueprintRenderer(PlannerPatterns::isOrder),
            )
        }
        if (Loader.isModLoaded("NotEnoughItems")) {
            // NEI registers page dimensions at load-complete, before it loads IConfigureNEI plugins.
            MinecraftForge.EVENT_BUS.register(SceneHandlerInfo)
            FMLCommonHandler.instance().bus().register(SceneCatalysts)
        }
    }

    override fun loadComplete(event: FMLLoadCompleteEvent) {
        // Addons such as Impact Core create their metatiles during their own postInit.
        if (Loader.isModLoaded("gregtech") && Loader.isModLoaded("impactapi")) {
            GregTechScenes.register()
        }
        if (Loader.isModLoaded("NotEnoughItems")) {
            SceneCatalysts.index()
        }
    }
}
