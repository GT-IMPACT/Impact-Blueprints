package space.impact.iblueprints.proxy

import appeng.api.AEApi
import cpw.mods.fml.common.network.NetworkRegistry
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
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.oredict.ShapedOreRecipe
import space.impact.iblueprints.ImpactBlueprintsMod
import space.impact.iblueprints.MODID
import space.impact.iblueprints.integration.ae2.BlueprintProviderBlock
import space.impact.iblueprints.integration.ae2.BlueprintProviderTile
import space.impact.iblueprints.integration.ae2.MeBlueprintPlannerItem
import space.impact.iblueprints.integration.ae2.MePlannerGuiFactory
import space.impact.iblueprints.integration.ae2.StructurePatternItem
import space.impact.iblueprints.integration.structures.BlueprintWorldUse
import space.impact.iblueprints.integration.structures.ProjectorChannelNetwork
import space.impact.iblueprints.item.BlankBlueprintItem
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.planner.BlueprintPlannerBlock
import space.impact.iblueprints.planner.BlueprintPlannerTile

open class CommonProxy {

    // common

    open fun init(event: FMLInitializationEvent) {
        GameRegistry.addRecipe(
            ShapedOreRecipe(
                ItemStack(BlankBlueprintItem),
                " P ",
                "PDP",
                " P ",
                'P',
                Items.paper,
                'D',
                "dyeBlue",
            ),
        )
        if (Loader.isModLoaded("modularui2")) {
            GameRegistry.addShapelessRecipe(
                ItemStack(BlueprintPlannerBlock),
                BlankBlueprintItem,
                Blocks.crafting_table,
            )
            if (Loader.isModLoaded("appliedenergistics2")) {
                registerMePlannerRecipes()
            }
        }
    }

    private fun registerMePlannerRecipes() {
        val definitions = AEApi.instance().definitions()
        val interfaceBlock = definitions.blocks().iface().maybeStack(1).orNull()
        if (interfaceBlock != null) {
            GameRegistry.addShapelessRecipe(
                ItemStack(BlueprintProviderBlock),
                BlankBlueprintItem,
                interfaceBlock,
            )
        }
        val patternTerminal = definitions.parts().patternTerminal().maybeStack(1).orNull()
        if (patternTerminal != null) {
            GameRegistry.addShapelessRecipe(
                ItemStack(MeBlueprintPlannerItem),
                BlankBlueprintItem,
                patternTerminal,
            )
        }
    }

    public open fun preInit(event: FMLPreInitializationEvent) {
        GameRegistry.registerItem(
            BlankBlueprintItem,
            BlankBlueprintItem.REGISTRY_NAME,
        )
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
            if (Loader.isModLoaded("appliedenergistics2")) {
                MeBlueprintPlannerItem.register()
                GameRegistry.registerItem(
                    StructurePatternItem,
                    StructurePatternItem.REGISTRY_NAME,
                )
                GameRegistry.registerBlock(
                    BlueprintProviderBlock,
                    BlueprintProviderBlock.REGISTRY_NAME,
                )
                GameRegistry.registerTileEntity(
                    BlueprintProviderTile::class.java,
                    "$MODID:blueprint_provider",
                )
                NetworkRegistry.INSTANCE.registerGuiHandler(
                    ImpactBlueprintsMod,
                    MePlannerGuiFactory,
                )
            }
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
