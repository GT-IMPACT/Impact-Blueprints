@file:Suppress("unused")

package space.impact.iblueprints.proxy

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.event.FMLLoadCompleteEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.common.MinecraftForge
import space.impact.iblueprints.integration.gregtech.GregTechScenes
import space.impact.iblueprints.integration.nei.SceneHandlerInfo
import space.impact.iblueprints.integration.nei.SceneCatalysts

public class ClientProxy : CommonProxy() {

    override fun preInit(event: FMLPreInitializationEvent) {
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
    }
}
