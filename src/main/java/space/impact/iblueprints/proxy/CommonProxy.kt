package space.impact.iblueprints.proxy

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.event.FMLLoadCompleteEvent
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent
import cpw.mods.fml.common.event.FMLServerStartedEvent
import cpw.mods.fml.common.event.FMLServerStartingEvent
import cpw.mods.fml.common.event.FMLServerStoppedEvent
import cpw.mods.fml.common.event.FMLServerStoppingEvent
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraftforge.common.MinecraftForge
import space.impact.iblueprints.MODID
import space.impact.iblueprints.integration.structures.BlueprintWorldUse
import space.impact.iblueprints.integration.structures.ProjectorChannelNetwork
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.planner.BlueprintPlannerBlock
import space.impact.iblueprints.planner.BlueprintPlannerTile

open class CommonProxy {

    // common

    open fun init(event: FMLInitializationEvent) = Unit

    public open fun preInit(event: FMLPreInitializationEvent) {
        StructureBlueprintItem.register()
        if (Loader.isModLoaded("modularui2")) {
            GameRegistry.registerBlock(
                BlueprintPlannerBlock,
                BlueprintPlannerBlock.REGISTRY_NAME,
            )
            GameRegistry.registerTileEntity(
                BlueprintPlannerTile::class.java,
                "$MODID:blueprint_planner",
            )
        }
        if (Loader.isModLoaded("impactapi")) {
            ProjectorChannelNetwork.register()
            MinecraftForge.EVENT_BUS.register(BlueprintWorldUse)
        }
    }


    open fun postInit(event: FMLPostInitializationEvent) = Unit

    public open fun loadComplete(event: FMLLoadCompleteEvent) = Unit

    // server only

    fun serverAboutToStart(event: FMLServerAboutToStartEvent) = Unit

    fun serverStarting(event: FMLServerStartingEvent) = Unit

    fun serverStarted(event: FMLServerStartedEvent) = Unit

    fun serverStopping(event: FMLServerStoppingEvent) = Unit

    fun serverStopped(event: FMLServerStoppedEvent) = Unit
}
