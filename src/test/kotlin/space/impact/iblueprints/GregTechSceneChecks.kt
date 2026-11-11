package space.impact.iblueprints

import appeng.api.implementations.ICraftingPatternItem
import appeng.api.networking.GridFlags
import com.cleanroommc.modularui.screen.UISettings
import com.cleanroommc.modularui.utils.item.ItemStackHandler
import com.cleanroommc.modularui.value.sync.ModularSyncManager
import com.cleanroommc.modularui.value.sync.PanelSyncManager
import com.cleanroommc.modularui.widget.WidgetTree
import com.cleanroommc.modularui.widgets.slot.ItemSlot
import cpw.mods.fml.common.DummyModContainer
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.ModContainer
import cpw.mods.fml.common.ModMetadata
import cpw.mods.fml.common.event.FMLLoadCompleteEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.registry.GameRegistry
import io.netty.buffer.Unpooled
import space.impact.iblueprints.proxy.ClientProxy
import ic2.core.Ic2Items
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AssemblyLine
import gregtech.api.GregTech_API
import gregtech.api.enums.Casings
import gregtech.api.enums.GT_Values
import gregtech.api.enums.ItemList
import gregtech.api.interfaces.internal.IGT_Mod
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeChemicalReactor
import gregtech.api.metatileentity.BaseMetaTileEntity
import gregtech.api.metatileentity.MetaTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Energy
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Maintenance
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Input
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output_ME
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus_ME
import gregtech.common.blocks.GT_Item_Machines
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DistillationTower
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AirFilter
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_ElectricBlastFurnace
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_HeatExchanger
import gregtech.api.util.GT_Config
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.config.Configuration
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.oredict.OreDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import space.impact.iblueprints.integration.gregtech.GregTechScenes
import space.impact.iblueprints.integration.ae2.MeBlueprintPlannerItem
import space.impact.iblueprints.integration.ae2.MeBlueprintPlannerContainer
import space.impact.iblueprints.integration.ae2.MeBlueprintPlannerPart
import space.impact.iblueprints.integration.ae2.MePlannerInventory
import space.impact.iblueprints.integration.ae2.BlueprintProviderTile
import space.impact.iblueprints.integration.ae2.PlannerPatterns
import space.impact.iblueprints.integration.ae2.StructurePatternItem
import space.impact.iblueprints.integration.gregtech.BlueprintHatch
import space.impact.iblueprints.integration.nei.SceneCatalysts
import space.impact.iblueprints.integration.nei.SceneRecipeHandler
import space.impact.iblueprints.integration.modularui.SceneAlternativeList
import space.impact.iblueprints.integration.modularui.SceneMaterialSlot
import space.impact.iblueprints.integration.modularui.SceneViewer
import space.impact.iblueprints.integration.modularui.SceneWidget
import space.impact.iblueprints.integration.structures.SaveProjectorChannels
import space.impact.iblueprints.integration.structures.SaveStructureBlueprint
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.integration.structures.BlueprintPlacement
import space.impact.iblueprints.integration.structures.BlueprintPlacementResult
import space.impact.iblueprints.integration.structures.BlueprintWorldUse
import space.impact.iblueprints.integration.structures.PreviewPlayer
import space.impact.iblueprints.integration.structures.StructurePreviewCollector
import space.impact.iblueprints.item.BlankBlueprintItem
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.planner.BlueprintPlannerTile
import space.impact.iblueprints.planner.BlueprintPlannerGui
import space.impact.iblueprints.planner.PlannerState
import space.impact.iblueprints.planner.loadPlannerTraceConfig
import space.impact.iblueprints.planner.plannerTraceEnabled
import space.impact.iblueprints.planner.tracePlannerStep
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneAlternativeGroups
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.SceneBlockHit
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.SceneView
import space.impact.iblueprints.scene.world.SceneWorld
import space.impact.structures.Main
import space.impact.structures.ImpactStructures
import space.impact.structures.StructureEvent
import space.impact.structures.alignment.enumerable.ExtendedFacing
import space.impact.structures.CommonProxy
import space.impact.structures.alignment.constructable.ChannelDataAccessor
import space.impact.structures.item.ItemConstructableTrigger
import space.impact.structures.structure.IItemSource
import space.impact.structures.structure.AutoPlaceEnvironment
import space.impact.structures.structure.IStructureElement
import space.impact.structures.structure.IStructureElement.BlocksToPlace
import space.impact.structures.structure.StructureUtility
import space.impact.structures.util.InventoryUtility
import java.io.File
import java.nio.file.Files
import java.lang.reflect.Proxy
import java.util.function.Supplier

internal object GregTechSceneChecks {

    internal fun run(serverPlayer: EntityPlayerMP) {
        GameRegistry.registerTileEntity(
            BaseMetaTileEntity::class.java,
            "iblueprints:test_machine",
        )
        GameRegistry.registerTileEntity(
            BlueprintPlannerTile::class.java,
            "iblueprints:test_planner",
        )
        GameRegistry.registerTileEntity(
            BlueprintProviderTile::class.java,
            "iblueprints:test_provider",
        )
        Item.itemRegistry.addObject(
            3001,
            "iblueprints:structure_pattern",
            StructurePatternItem,
        )
        Item.itemRegistry.addObject(
            3002,
            "iblueprints:blank_blueprint",
            BlankBlueprintItem,
        )
        checkWorldPlacement()
        checkRegisteredInventoryPlacement(serverPlayer)
        checkSceneTileCleanup()
        checkSceneTileCleanupFailures()
        checkPlannerTraceConfig()
        GregTech_API.sMachineFile = GT_Config(Configuration(File("build/test-config/scene-machines.cfg")))
        GregTech_API.sBlockCasings2 = Blocks.wool
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay
        GregTech_API.sBlockCasings1 = Blocks.planks
        GregTech_API.sBlockCasings3 = Blocks.stained_glass_pane
        GregTech_API.sBlockCasings5 = Blocks.stained_glass
        GregTech_API.sBlockCasings8 = Blocks.stonebrick
        GregTech_API.sBlockCasingsNH = Blocks.iron_block
        val proxy = Main::class.java.getDeclaredField("proxy")
        proxy.isAccessible = true
        proxy.set(
            null,
            CommonProxy(),
        )
        val block = object : Block(Material.iron) {

            override fun hasTileEntity(metadata: Int): Boolean {
                return true
            }

            override fun createTileEntity(
                world: World,
                metadata: Int,
            ): TileEntity {
                return BaseMetaTileEntity()
            }

            override fun getDamageValue(
                world: World,
                x: Int,
                y: Int,
                z: Int,
            ): Int {
                return (world.getTileEntity(
                    x,
                    y,
                    z,
                ) as BaseMetaTileEntity).metaTileID
            }
        }
        Block.blockRegistry.addObject(
            3000,
            "scene_test:machine",
            block,
        )
        Item.itemRegistry.addObject(
            3000,
            "scene_test:machine",
            GT_Item_Machines(block),
        )
        GregTech_API.sBlockMachines = block
        register(
            id = 32000,
            prototype = GT_MetaTileEntity_Hatch_Maintenance(
                "test",
                1,
                "test",
                null,
                false,
            ),
        )
        register(
            id = 32001,
            prototype = GT_MetaTileEntity_Hatch_Energy(
                "test",
                1,
                "test",
                null,
            ),
        )
        register(
            id = 32002,
            prototype = GT_MetaTileEntity_Hatch_InputBus(
                "test",
                1,
                "test",
                null,
            ),
        )
        register(
            id = 32003,
            prototype = GT_MetaTileEntity_Hatch_OutputBus(
                "test",
                1,
                "test",
                null,
            ),
        )
        register(
            id = 32004,
            prototype = GT_MetaTileEntity_Hatch_Input(
                "test",
                1,
                "test",
                null,
            ),
        )
        register(
            id = 32005,
            prototype = GT_MetaTileEntity_Hatch_Output(
                "test",
                1,
                "test",
                null,
            ),
        )
        register(
            id = 32006,
            prototype = GT_MetaTileEntity_Hatch_Muffler(
                "test",
                1,
                "test",
                null,
            ),
        )
        register(
            id = 32010,
            prototype = GT_MetaTileEntity_VacuumFreezer("test_freezer"),
        )
        register(
            id = 32011,
            prototype = GT_MetaTileEntity_DistillationTower("test_tower"),
        )
        register(
            id = 32012,
            prototype = GT_MetaTileEntity_ElectricBlastFurnace("test_blast"),
        )
        register(
            id = 32013,
            prototype = GT_MetaTileEntity_HeatExchanger("test_exchanger"),
        )
        register(
            id = 32014,
            prototype = GT_MetaTileEntity_LargeChemicalReactor("test_chemical_reactor"),
        )
        ItemList.AdsorptionFilter.set(ItemStack(Items.paper))
        ItemList.AdsorptionFilterDirty.set(ItemStack(Items.paper))
        register(
            id = 32016,
            prototype = GT_MetaTileEntity_AirFilter("test_air_filter"),
        )
        val namedMods = Loader::class.java.getDeclaredField("namedMods")
        namedMods.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val mods = namedMods.get(Loader.instance()) as MutableMap<String, ModContainer>
        for (id in listOf(
            "gregtech",
            "impactapi",
        )) {
            val metadata = ModMetadata()
            metadata.modId = id
            mods[id] = DummyModContainer(metadata)
        }
        val clientProxy = ClientProxy()
        clientProxy.postInit(FMLPostInitializationEvent())
        assertTrue(
            "Scene registration must wait for other mods to finish postInit",
            SceneRegistry.all().none {
                it.id == "iblueprints:metatile_32010"
            },
        )
        // Impact Core registers its machines after our postInit in the affected modpack.
        Ic2Items.reinforcedGlass = ItemStack(Blocks.stained_glass)
        register(
            id = 32015,
            prototype = GT_MetaTileEntity_AssemblyLine("test_assembly_line"),
        )
        clientProxy.loadComplete(FMLLoadCompleteEvent())
        assertTrue(SceneRegistry.all().any {
            it.id == "iblueprints:metatile_32015"
        })
        val towerEntry = SceneRegistry.all().single {
            it.id == "iblueprints:metatile_32011"
        }
        checkProjectorSave()
        assertTrue(towerEntry.channels.any {
                    it.id == "height"
                })
        val controller = ItemStack(
            block,
            1,
            32010,
        )
        GregTechScenes.create(
            controller = controller,
            values = emptyMap(),
        ).use { scene ->
            assertTrue(
                scene.diagnostics.toString(),
                scene.diagnostics.isEmpty(),
            )
            assertEquals(
                26,
                scene.positions().size,
            )
            assertEquals(
                1,
                scene.elements.values.count {
                        it.requiredAir
                    },
            )
            assertEquals(
                0,
                scene.elements.values.count {
                        it.unresolved
                    },
            )
            assertEquals(
                0,
                hatchCount(scene),
            )
            assertEquals(
                25,
                scene.positions().count {
                    scene.world.getBlock(
                        x = it.x,
                        y = it.y,
                        z = it.z,
                    ) === GregTech_API.sBlockCasings2
                },
            )
            assertFalse(block.shouldSideBeRendered(
                scene.world,
                0,
                65,
                0,
                1,
            ))
            scene.world.withVisiblePositions(positions = scene.positions().filter {
                it.y <= 64
            }.toSet()) {
                assertTrue(block.shouldSideBeRendered(
                    scene.world,
                    0,
                    65,
                    0,
                    1,
                ))
            }
            assertFalse(block.shouldSideBeRendered(
                scene.world,
                0,
                65,
                0,
                1,
            ))
            val view = SceneView(scene)
            assertTrue(!view.slice.enabled)
            assertTrue(view.highlightedPositions().isEmpty())
            val casing = view.materials.single {
                it.positions.size == 25
            }
            view.toggleMaterial(casing)
            assertEquals(
                casing.positions.toSet(),
                view.highlightedPositions().toSet(),
            )
            assertTrue(view.highlightedPositions().none {
                scene.elements[it]?.requiredAir == true
            })
            view.slice.select(
                axis = 1,
                level = scene.bounds().min.y,
                cutaway = false,
            )
            assertTrue(view.highlightedPositions().all {
                it.y == scene.bounds().min.y
            })
            view.toggleMaterial(casing)
            assertTrue(view.highlightedPositions().isEmpty())
        }
        towerEntry.build(mapOf("height" to 1)).use { small ->
            towerEntry.build(mapOf("height" to 3)).use { large ->
                assertTrue(
                    small.diagnostics.toString(),
                    small.diagnostics.isEmpty(),
                )
                assertTrue(
                    large.diagnostics.toString(),
                    large.diagnostics.isEmpty(),
                )
                assertTrue(large.positions().size > small.positions().size)
                BuiltStructureCaptureChecks.checkLayout(large)
            }
        }
        val blast = ItemStack(
            block,
            1,
            32012,
        )
        GregTechScenes.create(
            controller = blast,
            values = mapOf("coil" to 1),
        ).use { low ->
            GregTechScenes.create(
                controller = blast,
                values = mapOf("coil" to 2),
            ).use { high ->
                assertTrue(
                    low.diagnostics.toString(),
                    low.diagnostics.isEmpty(),
                )
                assertTrue(
                    high.diagnostics.toString(),
                    high.diagnostics.isEmpty(),
                )
                assertEquals(
                    1,
                    hatchCount(low),
                )
                assertEquals(
                    1,
                    hatchCount(high),
                )
                val coils = low.positions().filter {
                    low.world.getBlock(
                        x = it.x,
                        y = it.y,
                        z = it.z,
                    ) === Blocks.stained_glass
                }
                assertTrue(coils.isNotEmpty())
                assertTrue(coils.any {
                    low.world.getBlockMetadata(
                        x = it.x,
                        y = it.y,
                        z = it.z,
                    ) != high.world.getBlockMetadata(
                        x = it.x,
                        y = it.y,
                        z = it.z,
                    )
                })
            }
        }
        GregTechScenes.create(
            controller = ItemStack(
                block,
                1,
                32013,
            ),
            values = emptyMap(),
        ).use { exchanger ->
            assertTrue(
                exchanger.diagnostics.toString(),
                exchanger.diagnostics.isEmpty(),
            )
            assertEquals(
                0,
                exchanger.elements.values.count {
                        it.unresolved
                    },
            )
            assertEquals(
                2,
                hatchCount(exchanger),
            )
            for (position in exchanger.positions()) {
                val tile = exchanger.world.getTileEntity(
                    x = position.x,
                    y = position.y,
                    z = position.z,
                ) as? BaseMetaTileEntity ?: continue
                val hatch = tile.metaTileEntity as? GT_MetaTileEntity_Hatch ?: continue
                assertEquals(
                    "Automatically placed hatches must receive their structure casing texture",
                    Casings.STABLE_TITANIUM.textureId,
                    (hatch.texturePage.toInt() shl 7) or hatch.updateData.toInt(),
                )
            }
            checkDefaultHatchFacing(exchanger)
        }
        checkReactorCasings(ItemStack(
            block,
            1,
            32014,
        ))
        checkFreezerSimulation(controller)
        val gt = GT_Values.GT
        GT_Values.GT = Proxy.newProxyInstance(
            IGT_Mod::class.java.classLoader,
            arrayOf(IGT_Mod::class.java),
        ) { _, method, _ ->
            // GT's client proxy also reports that the server classes are present.
            check(method.name == "isServerSide")
            true
        } as IGT_Mod
        try {
            checkAirFilterMuffler(GregTech_API.METATILEENTITIES[32016].getStackForm(1))
        } finally {
            GT_Values.GT = gt
        }
        checkAssemblyLine()
        SceneCatalysts.index()
        while (SceneCatalysts.advanceIndex()) {
            // Drain the client-tick work without starting Minecraft's main loop.
        }
        val reactor = SceneRegistry.all().single {
            it.id == "iblueprints:metatile_32014"
        }
        val hatch = GregTech_API.METATILEENTITIES[32001].getStackForm(1)
        val hatchUsage = SceneRecipeHandler()
        hatchUsage.loadUsageRecipes(hatch)
        val reactorRecipe = (0 until hatchUsage.numRecipes()).single {
            hatchUsage.controller(it)?.isItemEqual(reactor.item()) == true
        }
        val reactorCatalysts = hatchUsage.catalysts(reactorRecipe)
        assertTrue(reactorCatalysts.first().item.isItemEqual(reactor.item()))
        assertFalse(reactorCatalysts.any {
            it.item.isItemEqual(hatch)
        })
        assertTrue(reactorCatalysts.drop(1).isNotEmpty())
        val blastEntry = SceneRegistry.all().single {
            it.id == "iblueprints:metatile_32012"
        }
        blastEntry.build(mapOf("coil" to 2)).use { scene ->
            val position = scene.positions().first {
                scene.item(it)?.item === Item.getItemFromBlock(Blocks.stained_glass)
            }
            val coil = requireNotNull(scene.item(position))
            val usage = requireNotNull(SceneCatalysts.usage(
                entry = blastEntry,
                stack = coil,
            ))
            assertEquals(
                2,
                usage.values["coil"],
            )
            assertTrue(position in usage.positions)
        }
        val muffler = GregTech_API.METATILEENTITIES[32006]
        val mufflerUsage = SceneRecipeHandler()
        mufflerUsage.loadUsageRecipes(muffler.getStackForm(1))
        val blastRecipe = (0 until mufflerUsage.numRecipes()).single {
            mufflerUsage.controller(it)?.isItemEqual(blast) == true
        }
        val blastCatalysts = mufflerUsage.catalysts(blastRecipe)
        assertTrue(blastCatalysts.first().item.isItemEqual(blast))
        assertTrue(blastCatalysts.size > 1)
        assertFalse(blastCatalysts.any {
            GregTechScenes.isHatch(it.item)
        })
        GregTech_API.METATILEENTITIES[32006] = null
        try {
            GregTechScenes.create(
                controller = blast,
                values = emptyMap(),
            ).use { incomplete ->
                assertTrue(incomplete.diagnostics.isNotEmpty())
                assertTrue(incomplete.elements.values.any {
                    it.unresolved
                })
                assertTrue(incomplete.elements.values.any {
                    it.requiredAir
                })
            }
        } finally {
            GregTech_API.METATILEENTITIES[32006] = muffler
        }
        checkAlternativeGroups()
        checkPickedFaces()
        checkWildcardAlternatives()
    }

    private fun checkWildcardAlternatives() {
        val block = Blocks.stained_glass
        val position = ScenePosition(
            x = 0,
            y = 64,
            z = 0,
        )
        val element = object : IStructureElement<Any> by StructureUtility.ofBlockAnyMeta<Any>(block) {
            override fun getBlocksToPlace(
                context: Any,
                world: World,
                x: Int,
                y: Int,
                z: Int,
                trigger: ItemStack,
                environment: AutoPlaceEnvironment,
            ): BlocksToPlace {
                return BlocksToPlace.create(
                    Item.getItemFromBlock(block),
                    OreDictionary.WILDCARD_VALUE,
                )
            }
        }
        BlockScene("Wildcard glass").use { scene ->
            scene.block(
                position = position,
                block = block,
            )
            val collector = StructurePreviewCollector(
                scene = scene,
                context = Any(),
                trigger = ItemStack(block),
                actor = PreviewPlayer(scene.world),
                candidates = listOf(
                    ItemStack(block),
                    ItemStack(
                        block,
                        1,
                        14,
                    ),
                    ItemStack(Blocks.glass),
                ),
            )
            MinecraftForge.EVENT_BUS.register(collector)
            ImpactStructures.enableInstrument(collector)
            try {
                StructureEvent.StructureElementVisitedEvent.fireEvent(
                    scene.world,
                    position.x,
                    position.y,
                    position.z,
                    0,
                    0,
                    0,
                    element,
                )
            } finally {
                ImpactStructures.disableInstrument()
                MinecraftForge.EVENT_BUS.unregister(collector)
            }
            collector.finish { _, _, _ ->
                {
                    emptyList()
                }
            }
            val choices = scene.elements.getValue(position).alternatives()
            assertTrue(choices.all {
                it.item === Item.getItemFromBlock(block)
            })
            assertEquals(
                listOf(0, 14),
                choices.map(ItemStack::getItemDamage),
            )
        }
    }

    private fun checkPickedFaces() {
        val position = ScenePosition(
            x = 0,
            y = 64,
            z = 0,
        )
        BlockScene("Pick faces").use { scene ->
            scene.block(
                position = position,
                block = Blocks.stone,
            )
            val view = SceneView(scene)
            val angles = mapOf(
                ForgeDirection.SOUTH to (0.0 to 0.0),
                ForgeDirection.NORTH to (180.0 to 0.0),
                ForgeDirection.EAST to (-90.0 to 0.0),
                ForgeDirection.WEST to (90.0 to 0.0),
                ForgeDirection.UP to (0.0 to 89.0),
                ForgeDirection.DOWN to (0.0 to -89.0),
            )
            for ((face, angle) in angles) {
                view.camera.fit(
                    bounds = scene.bounds(),
                    width = 200,
                    height = 200,
                )
                view.camera.rotate(
                    dx = angle.first - view.camera.yaw,
                    dy = angle.second - view.camera.pitch,
                )
                val hit = requireNotNull(view.pick(
                    visible = scene.positions(),
                    mouseX = 100,
                    mouseY = 100,
                ))
                assertEquals(
                    position,
                    hit.position,
                )
                assertEquals(
                    face,
                    hit.face,
                )
            }
        }
    }

    private fun checkAlternativeGroups() {
        val meFluidOutput = GT_MetaTileEntity_Hatch_Output_ME(
            "test_me_fluid_output",
            4,
            emptyArray(),
            null,
        )
        val meItemOutput = GT_MetaTileEntity_Hatch_OutputBus_ME(
            "test_me_item_output",
            4,
            emptyArray(),
            null,
        )
        register(
            id = 32017,
            prototype = meFluidOutput,
        )
        register(
            id = 32018,
            prototype = meItemOutput,
        )
        val fluidOutput = GregTech_API.METATILEENTITIES[32005].getStackForm(1)
        val itemOutput = GregTech_API.METATILEENTITIES[32003].getStackForm(1)
        assertEquals(
            "iblueprints.variants.group.fluid_output",
            SceneAlternativeGroups.group(fluidOutput),
        )
        assertEquals(
            SceneAlternativeGroups.group(fluidOutput),
            SceneAlternativeGroups.group(meFluidOutput.getStackForm(1)),
        )
        assertEquals(
            "iblueprints.variants.group.item_output",
            SceneAlternativeGroups.group(itemOutput),
        )
        assertEquals(
            SceneAlternativeGroups.group(itemOutput),
            SceneAlternativeGroups.group(meItemOutput.getStackForm(1)),
        )
        val casing = ItemStack(
            Casings.FROST_PROOF.block,
            1,
            Casings.FROST_PROOF.metadata,
        )
        assertEquals(
            "iblueprints.variants.group.casings",
            SceneAlternativeGroups.group(casing),
        )
        val unmatched = ItemStack(Blocks.stone)
        assertEquals(
            SceneAlternativeGroups.OTHER,
            SceneAlternativeGroups.group(unmatched),
        )
        SceneAlternativeGroups.register(
            translationKey = "test.alternatives.custom",
            matches = fluidOutput::isItemEqual,
        )
        assertEquals(
            "test.alternatives.custom",
            SceneAlternativeGroups.group(fluidOutput),
        )
        SceneAlternativeGroupPriorityChecks.run(fluidOutput)
    }

    private fun checkPlannerTraceConfig() {
        val defaults = Files.createTempFile(
            "iblueprints-trace-defaults",
            ".cfg",
        )
        val disabled = Files.createTempFile(
            "iblueprints-trace-disabled",
            ".cfg",
        )
        try {
            loadPlannerTraceConfig(defaults.toFile())
            assertTrue(plannerTraceEnabled)
            Files.write(
                disabled,
                "diagnostics {\n    B:plannerTrace=false\n}\n".toByteArray(),
            )
            loadPlannerTraceConfig(disabled.toFile())
            assertFalse(plannerTraceEnabled)
            val result = tracePlannerStep(
                step = "test.disabled",
                details = "must be silent",
            ) {
                42
            }
            assertEquals(
                42,
                result,
            )
        } finally {
            loadPlannerTraceConfig(defaults.toFile())
            Files.deleteIfExists(defaults)
            Files.deleteIfExists(disabled)
        }
    }

    private fun checkSceneTileCleanup() {
        val world = SceneWorld()
        world.isRemote = false
        val tile = object : TileEntity() {

            override fun invalidate() {
                assertTrue(
                    "Preview tile cleanup must run on the client side",
                    worldObj.isRemote,
                )
                super.invalidate()
            }
        }

        world.setTileEntity(
            0,
            64,
            0,
            tile,
        )
        world.removeTileEntity(
            0,
            64,
            0,
        )
        assertTrue(tile.isInvalid)
        assertFalse(world.isRemote)

        world.setTileEntity(
            0,
            64,
            0,
            tile,
        )
        world.clear()
        assertTrue(tile.isInvalid)
        assertFalse(world.isRemote)
    }

    private fun checkSceneTileCleanupFailures() {
        val scene = BlockScene("Failing removal hook")
        val world = scene.world
        world.isRemote = false
        var removalCalls = 0
        val failingTile = object : TileEntity() {

            override fun invalidate() {
                assertTrue(worldObj.isRemote)
                removalCalls++
                // TecTech removal hooks can fail before TileEntity.invalidate() is reached.
                throw NullPointerException("Uninitialized preview machine")
            }
        }
        world.setTileEntity(
            x = 0,
            y = 64,
            z = 0,
            tile = failingTile,
        )
        world.removeTileEntity(
            x = 0,
            y = 64,
            z = 0,
        )
        assertEquals(
            null,
            world.getTileEntity(
                x = 0,
                y = 64,
                z = 0,
            ),
        )
        assertFalse(world.isRemote)

        world.setTileEntity(
            x = 0,
            y = 64,
            z = 0,
            tile = failingTile,
        )
        val replacement = TileEntity()
        world.setTileEntity(
            x = 0,
            y = 64,
            z = 0,
            tile = replacement,
        )
        scene.block(
            position = ScenePosition(
                x = 1,
                y = 64,
                z = 0,
            ),
            block = Blocks.stone,
        )
        world.setTileEntity(
            x = 1,
            y = 64,
            z = 0,
            tile = failingTile,
        )
        scene.close()
        assertTrue(replacement.isInvalid)
        assertTrue(scene.positions().isEmpty())
        assertEquals(
            null,
            world.getTileEntity(
                x = 1,
                y = 64,
                z = 0,
            ),
        )
        assertFalse(world.isRemote)
        scene.close()
        assertEquals(
            3,
            removalCalls,
        )
    }

    private fun checkWorldPlacement() {
        val world = SceneWorld()
        world.isRemote = false
        val player = PreviewPlayer(world)
        val stack = ItemStack(Blocks.stone)
        val placement = BlueprintPlacement(
            position = ScenePosition(
                x = 7,
                y = 64,
                z = 7,
            ),
            stack = stack,
            hatchFacing = null,
        )
        var available = 1
        val source = IItemSource { predicate, simulate, _ ->
            if (available == 0 || !predicate.test(stack)) {
                emptyMap()
            } else {
                if (!simulate) {
                    available--
                }
                mapOf(stack to 1)
            }
        }
        assertEquals(
            BlueprintPlacementResult.PLACED,
            BlueprintWorldUse.place(
                placement = placement,
                world = world,
                player = player,
                source = source,
            ),
        )
        assertEquals(
            Blocks.stone,
            world.getBlock(
                7,
                64,
                7,
            ),
        )
        assertEquals(0, available)
        available = 1
        assertEquals(
            BlueprintPlacementResult.OCCUPIED,
            BlueprintWorldUse.place(
                placement = placement,
                world = world,
                player = player,
                source = source,
            ),
        )
        assertEquals(1, available)
        assertEquals(
            Blocks.air,
            world.getBlock(
                7,
                65,
                7,
            ),
        )
        val uncommitted = placement.copy(
            position = ScenePosition(
                x = 8,
                y = 64,
                z = 7,
            ),
        )
        val unavailableOnCommit = IItemSource { _, simulate, _ ->
            if (simulate) {
                mapOf(stack to 1)
            } else {
                emptyMap()
            }
        }
        assertEquals(
            BlueprintPlacementResult.EXTRACTION_FAILED,
            BlueprintWorldUse.place(
                placement = uncommitted,
                world = world,
                player = player,
                source = unavailableOnCommit,
            ),
        )
        assertEquals(
            Blocks.air,
            world.getBlock(
                8,
                64,
                7,
            ),
        )
    }

    private fun checkRegisteredInventoryPlacement(serverPlayer: EntityPlayerMP) {
        val inventory = InventoryBasic(
            "Blueprint source test",
            true,
            1,
        )
        inventory.setInventorySlotContents(
            0,
            ItemStack(
                Blocks.wool,
                1,
                4,
            ),
        )
        InventoryUtility.registerInventoryProvider("8000-blueprint-test") { player ->
            inventory.takeIf { player === serverPlayer }
        }
        val world = SceneWorld()
        world.isRemote = false
        val player = PreviewPlayer(world)
        val source = IItemSource.fromPlayer(serverPlayer)
        val placement = BlueprintPlacement(
            position = ScenePosition(
                x = 7,
                y = 64,
                z = 7,
            ),
            stack = ItemStack(
                Blocks.wool,
                1,
                4,
            ),
            hatchFacing = null,
        )
        assertEquals(
            BlueprintPlacementResult.PLACED,
            BlueprintWorldUse.place(
                placement = placement,
                world = world,
                player = player,
                source = source,
            ),
        )
        assertEquals(null, inventory.getStackInSlot(0))
        val next = placement.copy(position = placement.position.copy(x = 8))
        assertEquals(
            BlueprintPlacementResult.MISSING_ITEM,
            BlueprintWorldUse.place(
                placement = next,
                world = world,
                player = player,
                source = source,
            ),
        )
    }

    private fun checkProjectorSave() {
        val projector = ItemStack(ItemConstructableTrigger())
        projector.setStackDisplayName("Build planner")
        ChannelDataAccessor.setChannelData(
            projector,
            "length",
            1,
        )
        ChannelDataAccessor.setChannelData(
            projector,
            "other",
            2,
        )
        val packet = SaveProjectorChannels(
            slot = 25,
            stack = projector,
            channels = mapOf("length" to 3),
        )
        val received = SaveProjectorChannels()
        val buffer = Unpooled.buffer()
        try {
            packet.toBytes(buffer)
            received.fromBytes(buffer)
            assertFalse(buffer.isReadable)
        } finally {
            buffer.release()
        }
        val updated = requireNotNull(received.updatedStack(projector.copy()))
        assertEquals(
            3,
            ChannelDataAccessor.getChannelData(
                updated,
                "length",
            ),
        )
        assertEquals(
            2,
            ChannelDataAccessor.getChannelData(
                updated,
                "other",
            ),
        )
        assertEquals(
            projector.displayName,
            updated.displayName,
        )
        assertEquals(
            1,
            ChannelDataAccessor.getChannelData(
                projector,
                "length",
            ),
        )
        assertTrue(received.updatedStack(updated) == null)
        assertTrue(received.updatedStack(ItemStack(Blocks.stone)) == null)
    }

    private fun checkAssemblyLine() {
        val entry = SceneRegistry.all().single {
            it.id == "iblueprints:metatile_32015"
        }
        for (length in listOf(
            1,
            12,
        )) {
            entry.build(mapOf("length" to length)).use { scene ->
                if (length == 12) {
                    BuiltStructureCaptureChecks.checkLayout(scene)
                }
                assertEquals(
                    10 * (length + 4),
                    scene.positions().size,
                )
                assertEquals(
                    length + 4,
                    hatchCount(scene),
                )
                assertTrue(scene.positions().none {
                    val tile = scene.world.getTileEntity(
                        x = it.x,
                        y = it.y,
                        z = it.z,
                    ) as? BaseMetaTileEntity
                    tile?.metaTileEntity is GT_MetaTileEntity_Hatch_Energy
                })
                assertTrue(!SceneView(scene).slice.enabled)
                if (length == 1) {
                    val glass = scene.positions().first {
                        scene.item(it)?.item === Item.getItemFromBlock(Blocks.stained_glass)
                    }
                    val glassAlternatives = scene.elements.getValue(glass).alternatives()
                    assertEquals(
                        (0..15).toList(),
                        glassAlternatives.map(ItemStack::getItemDamage).sorted(),
                    )
                    val color = glassAlternatives.single {
                        it.itemDamage == 14
                    }
                    val view = SceneView(scene)
                    assertTrue(view.replaceBlock(
                        position = glass,
                        stack = color,
                    ))
                    assertTrue(color.isItemEqual(scene.item(glass)))
                    assertTrue(view.materials.any {
                        color.isItemEqual(it.stack) && glass in it.positions
                    })
                    checkGlassBatch(
                        view = view,
                        glass = glass,
                        color = color,
                    )
                    val top = scene.positions().maxBy {
                        it.y
                    }
                    val element = scene.elements.getValue(top)
                    val alternatives = element.inspectionAlternatives()
                    assertTrue(alternatives.first().isItemEqual(scene.item(top)))
                    assertTrue(element.hatchChoicesIncomplete)
                }
            }
        }
    }

    private fun checkGlassBatch(
        view: SceneView,
        glass: ScenePosition,
        color: ItemStack,
    ) {
        val original = view.materials.single {
            it.stack.item === color.item && it.stack.itemDamage == 0
        }
        val second = original.positions.first()
        val origin = requireNotNull(view.scene.simulation).origin
        view.selectBlock(
            hit = SceneBlockHit(
                position = second,
                face = ForgeDirection.NORTH,
            ),
            additive = false,
        )
        view.selectBlock(
            hit = SceneBlockHit(
                position = glass,
                face = ForgeDirection.SOUTH,
            ),
            additive = true,
        )
        val both = view.inspectedPositions()
        assertEquals(
            setOf(
                glass,
                second,
            ),
            both,
        )
        assertTrue(view.commonAlternatives(both).any(color::isItemEqual))
        val controller = SceneBlockHit(
            position = origin,
            face = null,
        )
        view.selectBlock(
            hit = controller,
            additive = true,
        )
        val mixed = view.inspectedPositions()
        assertTrue(view.commonAlternatives(mixed).isEmpty())
        assertFalse(view.replaceSelection(
            positions = mixed,
            stack = color,
        ))
        assertTrue(original.stack.isItemEqual(view.scene.item(second)))
        view.selectBlock(
            hit = controller,
            additive = true,
        )
        assertTrue(view.replaceSelection(
            positions = both,
            stack = color,
        ))
        val red = view.materials.single {
            color.isItemEqual(it.stack)
        }
        assertEquals(
            both,
            red.positions.toSet(),
        )
        view.toggleMaterial(red)
        assertTrue(view.inspectedPositions().isEmpty())
        view.toggleMaterial(red)
        val clear = view.materials.single {
            original.stack.isItemEqual(it.stack)
        }
        view.toggleMaterial(
            material = clear,
            additive = true,
        )
        assertEquals(
            both + clear.positions,
            view.inspectedPositions(),
        )
        assertTrue(view.isMaterialSelected(red))
        assertTrue(view.isMaterialSelected(clear))
        view.toggleMaterial(
            material = clear,
            additive = true,
        )
        assertFalse(view.isMaterialSelected(clear))
        assertEquals(
            both,
            view.inspectedPositions(),
        )
        assertTrue(view.replaceSelection(
            positions = both,
            stack = original.stack,
        ))
        assertTrue(both.all {
            original.stack.isItemEqual(view.scene.item(it))
        })
    }

    @Suppress("DEPRECATION")
    private fun checkAirFilterMuffler(controller: ItemStack) {
        GregTechScenes.create(
            controller = controller,
            values = emptyMap(),
        ).use { scene ->
            val view = SceneView(scene)
            val muffler = GregTech_API.METATILEENTITIES[32006].getStackForm(1)
            val position = scene.elements.entries.first { (position, element) ->
                position.y == 65 && element.inspectionAlternatives().any(muffler::isItemEqual)
            }.key
            assertTrue(view.replaceBlock(
                position = position,
                stack = muffler,
                preferredFacing = ForgeDirection.DOWN,
            ))
            val facing = requireNotNull(scene.simulation?.hatchFacing(position))
            assertTrue(facing != ForgeDirection.DOWN)
            assertEquals(
                false,
                view.structureValid,
            )
            val tile = scene.world.getTileEntity(
                x = position.x,
                y = position.y,
                z = position.z,
            ) as BaseMetaTileEntity
            val hatch = tile.metaTileEntity as GT_MetaTileEntity_Hatch
            assertEquals(
                Casings.AIR_FILTER_TURBINE.textureId,
                (hatch.texturePage.toInt() shl 7) or hatch.updateData.toInt(),
            )
            assertEquals(
                Casings.AIR_FILTER_TURBINE.textureId and 127,
                hatch.mMachineBlock.toInt(),
            )
        }
    }

    private fun checkDefaultHatchFacing(scene: BlockScene) {
        val alternativeId = 32007
        val previous = GregTech_API.METATILEENTITIES[alternativeId]
        register(
            id = alternativeId,
            prototype = GT_MetaTileEntity_Hatch_Output(
                "test_alternative_output",
                2,
                "test",
                null,
            ),
        )
        try {
            val original = GregTech_API.METATILEENTITIES[32005].getStackForm(1)
            val alternative = GregTech_API.METATILEENTITIES[alternativeId].getStackForm(1)
            val position = scene.positions().first {
                original.isItemEqual(scene.item(it))
            }
            val simulation = requireNotNull(scene.simulation)
            val originalFacing = requireNotNull(simulation.hatchFacing(position))
            val selectedFacing = requireNotNull(simulation.nextHatchFacing(position))
            val view = SceneView(scene)
            view.selectBlock(
                hit = SceneBlockHit(
                    position = position,
                    face = selectedFacing,
                ),
                additive = false,
            )
            assertTrue(view.replaceSelection(
                positions = setOf(position),
                stack = alternative,
            ))
            assertEquals(
                selectedFacing,
                simulation.hatchFacing(position),
            )
            assertTrue(view.replaceSelection(
                positions = setOf(position),
                stack = original,
            ))
            assertTrue(original.isItemEqual(scene.item(position)))
            assertTrue(originalFacing != selectedFacing)
            assertEquals(
                selectedFacing,
                simulation.hatchFacing(position),
            )
        } finally {
            GregTech_API.METATILEENTITIES[alternativeId] = previous
        }
    }

    private fun checkFreezerSimulation(controller: ItemStack) {
        GregTechScenes.create(
            controller = controller,
            values = emptyMap(),
        ).use { scene ->
            val view = SceneView(scene)
            assertFalse(view.hasEditedStructure)
            view.validateStructure()
            assertEquals(
                false,
                view.structureValid,
            )
            val casing = view.materials.single { material ->
                material.positions.size == 25
            }
            val hatchIds = listOf(
                32000,
                32001,
                32002,
                32003,
            )
            for ((index, id) in hatchIds.withIndex()) {
                val position = casing.positions[index]
                val stack = scene.elements.getValue(position).inspectionAlternatives().single {
                    it.itemDamage == id
                }
                assertTrue(view.replaceBlock(
                    position = position,
                    stack = stack,
                ))
                assertTrue(stack.isItemEqual(scene.item(position)))
                assertTrue(view.hasEditedStructure)
            }
            assertEquals(
                true,
                view.structureValid,
            )
            val energy = GregTech_API.METATILEENTITIES[32001].getStackForm(1)
            val maintenance = GregTech_API.METATILEENTITIES[32000].getStackForm(1)
            assertTrue(view.replaceBlock(
                position = casing.positions[1],
                stack = maintenance,
            ))
            assertEquals(
                false,
                view.structureValid,
            )
            assertTrue(view.materials.none { material ->
                material.stack.isItemEqual(energy)
            })
            assertTrue(view.replaceBlock(
                position = casing.positions[1],
                stack = energy,
                preferredFacing = ForgeDirection.UP,
            ))
            for (index in 4 until 10) {
                assertTrue(view.replaceBlock(
                    position = casing.positions[index],
                    stack = energy,
                ))
            }
            assertEquals(
                false,
                view.structureValid,
            )
            assertEquals(
                15,
                view.materials.single { material ->
                    material.stack.isItemEqual(casing.stack)
                }.positions.size,
            )
            assertTrue(view.replaceBlock(
                position = casing.positions[9],
                stack = casing.stack,
            ))
            assertEquals(
                true,
                view.structureValid,
            )
            assertEquals(
                16,
                view.materials.single { material ->
                    material.stack.isItemEqual(casing.stack)
                }.positions.size,
            )
            assertEquals(
                6,
                view.materials.single { material ->
                    material.stack.isItemEqual(energy)
                }.positions.size,
            )
            val simulation = requireNotNull(scene.simulation)
            val energyPosition = casing.positions[1]
            val initialFacing = requireNotNull(simulation.hatchFacing(energyPosition))
            assertEquals(
                ForgeDirection.UP,
                initialFacing,
            )
            val nextFacing = requireNotNull(simulation.nextHatchFacing(energyPosition))
            assertTrue(view.rotateHatch(energyPosition))
            assertEquals(
                nextFacing,
                simulation.hatchFacing(energyPosition),
            )
            assertTrue(nextFacing != initialFacing)
            assertTrue(nextFacing != ForgeDirection.UNKNOWN)
            assertFalse(view.rotateHatch(casing.positions[9]))
            assertEquals(
                true,
                view.structureValid,
            )
            GregTechScenes.create(
                controller = controller,
                values = emptyMap(),
            ).use { rebuilt ->
                view.replaceConfiguredScene(rebuilt)
                assertTrue(view.hasEditedStructure)
                assertEquals(
                    true,
                    view.structureValid,
                )
                assertEquals(
                    9,
                    hatchCount(rebuilt),
                )
                assertEquals(
                    nextFacing,
                    rebuilt.simulation?.hatchFacing(energyPosition),
                )
                checkBlueprint(
                    scene = rebuilt,
                    hatchPosition = energyPosition,
                    hatchFacing = nextFacing,
                )
                BuiltStructureCaptureChecks.run(rebuilt)
            }
        }
    }

    private fun checkBlueprint(
        scene: BlockScene,
        hatchPosition: ScenePosition,
        hatchFacing: ForgeDirection,
    ) {
        val blueprint = ItemStack(StructureBlueprintItem)
        val plan = requireNotNull(SceneBlueprint.capture(
            scene = scene,
            channels = mapOf("length" to 2),
        ))
        val packet = requireNotNull(SaveStructureBlueprint.create(
            slot = 0,
            stack = blueprint,
            plan = plan,
        ))
        val buffer = Unpooled.buffer()
        val received = SaveStructureBlueprint()
        try {
            packet.toBytes(buffer)
            received.fromBytes(buffer)
        } finally {
            buffer.release()
        }
        val saved = requireNotNull(received.updatedStack(blueprint)).tagCompound
            .getCompoundTag(StructureBlueprintItem.PLAN_TAG)
        assertEquals(
            plan,
            saved,
        )
        assertFalse(blueprint.hasTagCompound())
        val origin = requireNotNull(scene.simulation).origin
        val originalFacing = ExtendedFacing.of(ForgeDirection.SOUTH)
        val placements = saved.getIntArray("placements").toList().chunked(5)
        assertEquals(
            scene.positions().size - 1,
            placements.size,
        )
        val hatch = placements.single { placement ->
            val world = IntArray(3)
            originalFacing.getWorldOffset(
                placement.take(3).toIntArray(),
                world,
            )
            world[0] + origin.x == hatchPosition.x && world[1] + origin.y == hatchPosition.y &&
                world[2] + origin.z == hatchPosition.z
        }
        val localSide = ForgeDirection.getOrientation(hatch[4])
        val originalSide = IntArray(3)
        originalFacing.getWorldOffset(
            intArrayOf(localSide.offsetX, localSide.offsetY, localSide.offsetZ),
            originalSide,
        )
        assertTrue(originalSide.contentEquals(intArrayOf(hatchFacing.offsetX, hatchFacing.offsetY, hatchFacing.offsetZ)))
        val rotatedFacing = ExtendedFacing.of(ForgeDirection.NORTH)
        val rotatedPosition = IntArray(3)
        rotatedFacing.getWorldOffset(
            hatch.take(3).toIntArray(),
            rotatedPosition,
        )
        assertEquals(
            listOf(origin.x - hatchPosition.x, hatchPosition.y - origin.y, origin.z - hatchPosition.z),
            rotatedPosition.toList(),
        )
        val rotatedSide = IntArray(3)
        rotatedFacing.getWorldOffset(
            intArrayOf(localSide.offsetX, localSide.offsetY, localSide.offsetZ),
            rotatedSide,
        )
        assertEquals(
            listOf(-hatchFacing.offsetX, hatchFacing.offsetY, -hatchFacing.offsetZ),
            rotatedSide.toList(),
        )
        val controllerPosition = ScenePosition(
            x = 20,
            y = 88,
            z = 40,
        )
        val plannedHatch = SceneBlueprint.placements(
            plan = saved,
            origin = controllerPosition,
            facing = rotatedFacing,
        ).single {
            it.position.x == controllerPosition.x + rotatedPosition[0] &&
                it.position.y == controllerPosition.y + rotatedPosition[1] &&
                it.position.z == controllerPosition.z + rotatedPosition[2]
        }
        assertTrue(plannedHatch.stack.isItemEqual(scene.item(hatchPosition)))
        val targetFacing = requireNotNull(plannedHatch.hatchFacing)
        assertEquals(
            rotatedSide.toList(),
            listOf(
                targetFacing.offsetX,
                targetFacing.offsetY,
                targetFacing.offsetZ,
            ),
        )
        checkPlanner(
            scene = scene,
            plan = plan,
            hatchPosition = hatchPosition,
            hatchFacing = hatchFacing,
        )
    }

    private fun checkViewerPanel(
        controller: ItemStack,
        plan: NBTTagCompound,
        hatchPosition: ScenePosition,
    ) {
        val channels = SceneBlueprint.channels(plan)
        val panel = SceneViewer.createPanel(
            entry = SceneEntry(
                id = "test:simulator",
                item = controller,
                factory = Supplier {
                    GregTechScenes.create(
                        controller = controller,
                        values = channels,
                    )
                },
            ),
            initialValues = channels,
            savedPlan = plan,
            onLayoutChanged = {},
        )
        val view = panel.children.filterIsInstance<SceneWidget>().single().view
        try {
            val materials = WidgetTree.flatListByType(
                panel,
                SceneMaterialSlot::class.java,
            )
            assertTrue(materials.isNotEmpty())
            // SecondaryPanel performs this same check before opening the simulator.
            assertFalse(WidgetTree.hasSyncedValues(panel))

            view.selectBlock(
                hit = SceneBlockHit(
                    position = hatchPosition,
                    face = ForgeDirection.UP,
                ),
                additive = false,
            )
            val alternatives = SceneAlternativeList(
                view = view,
                query = {
                    ""
                },
                onChanged = {},
            )
            panel.child(alternatives)
            assertTrue(WidgetTree.flatListByType(
                alternatives,
                SceneMaterialSlot::class.java,
            ).isNotEmpty())
            assertFalse(WidgetTree.hasSyncedValues(panel))
        } finally {
            view.scene.close()
        }
    }

    private fun checkPlanner(
        scene: BlockScene,
        plan: NBTTagCompound,
        hatchPosition: ScenePosition,
        hatchFacing: ForgeDirection,
    ) {
        val controller = requireNotNull(scene.item(requireNotNull(scene.simulation).origin))
        checkViewerPanel(
            controller = controller,
            plan = plan,
            hatchPosition = hatchPosition,
        )
        val tile = BlueprintPlannerTile()
        val planner = tile.planner
        assertEquals(
            3,
            planner.inventory.slots,
        )
        val panel = BlueprintPlannerGui.build(
            state = planner,
            manager = PanelSyncManager(
                ModularSyncManager(true),
                true,
            ),
            settings = UISettings(),
            accessible = {
                true
            },
        )
        assertEquals(
            3,
            panel.children.count {
                it is ItemSlot
            },
        )
        val id = "test:machine"
        val channels = mapOf("length" to 2)
        val original = GregTechScenes.create(
            controller = controller,
            values = channels,
        ).use {
            requireNotNull(SceneBlueprint.capture(
                scene = it,
                channels = channels,
            ))
        }
        assertTrue(planner.select(
            id = id,
            snapshot = original,
        ))
        assertTrue(planner.edit(
            expectedSelection = planner.selection,
            snapshot = plan,
        ))
        planner.inventory.setStackInSlot(
            PlannerState.BLUEPRINT_INPUT,
            ItemStack(Items.paper),
        )
        assertFalse(planner.saveBlueprint())
        planner.inventory.setStackInSlot(
            PlannerState.BLUEPRINT_INPUT,
            ItemStack(
                BlankBlueprintItem,
                2,
            ),
        )
        val saved = NBTTagCompound().also(tile::writeToNBT)
        val reopened = (TileEntity.createAndLoadEntity(saved) as BlueprintPlannerTile).planner
        assertEquals(id, reopened.recipe)
        val restoredPlan = requireNotNull(reopened.plan)
        checkMePlanner(restoredPlan)
        checkBlueprintProvider(restoredPlan)
        assertEquals(plan, restoredPlan)
        assertEquals(channels, SceneBlueprint.channels(restoredPlan))
        assertEquals(
            scene.positions().size,
            requireNotNull(SceneBlueprint.materials(restoredPlan)).sumOf {
                it.stackSize
            },
        )
        GregTechScenes.create(
            controller = controller,
            values = SceneBlueprint.channels(restoredPlan),
        ).use { restored ->
            val view = SceneView(restored)
            SceneBlueprint.restore(
                view = view,
                plan = restoredPlan,
            )
            assertEquals(
                hatchFacing,
                restored.simulation?.hatchFacing(hatchPosition),
            )
            assertEquals(
                plan,
                SceneBlueprint.capture(
                    scene = restored,
                    channels = channels,
                ),
            )
            val hatch = restored.world.getTileEntity(
                x = hatchPosition.x,
                y = hatchPosition.y,
                z = hatchPosition.z,
            )
            val next = requireNotNull(restored.simulation?.nextHatchFacing(hatchPosition))
            BlueprintHatch.orient(
                tile = hatch,
                direction = next,
            )
            assertEquals(
                next,
                BlueprintHatch.facing(hatch),
            )
        }
        reopened.saveBlueprint()
        reopened.saveBlueprint()
        assertEquals(
            1,
            reopened.inventory.getStackInSlot(PlannerState.BLUEPRINT_INPUT)?.stackSize,
        )
        val blueprint = requireNotNull(reopened.inventory.getStackInSlot(PlannerState.BLUEPRINT))
        assertEquals(
            plan,
            SceneBlueprint.fromItem(blueprint),
        )
        val imported = BlueprintPlannerTile().planner
        imported.inventory.setStackInSlot(
            PlannerState.BLUEPRINT_INPUT,
            blueprint,
        )
        assertTrue(imported.loadBlueprint())
        assertEquals(id, imported.recipe)
        assertEquals(plan, imported.plan)
        assertTrue(blueprint === imported.inventory.getStackInSlot(PlannerState.BLUEPRINT_INPUT))
        val projector = ItemStack(ItemConstructableTrigger())
        imported.inventory.setStackInSlot(
            PlannerState.PROJECTOR,
            projector,
        )
        assertTrue(imported.saveProjector())
        val savedProjector = requireNotNull(imported.inventory.getStackInSlot(PlannerState.PROJECTOR))
        assertEquals(
            plan,
            SceneBlueprint.fromItem(savedProjector),
        )
        assertEquals(
            2,
            ChannelDataAccessor.getChannelData(
                savedProjector,
                "length",
            ),
        )
        val legacy = blueprint.copy()
        legacy.tagCompound.removeTag(StructureBlueprintItem.RECIPE_TAG)
        imported.inventory.setStackInSlot(
            PlannerState.BLUEPRINT_INPUT,
            legacy,
        )
        assertTrue(imported.loadBlueprint())
        assertEquals("iblueprints:metatile_${controller.itemDamage}", imported.recipe)
        legacy.setStackDisplayName("Custom blueprint")
        assertTrue(imported.edit(
            expectedSelection = imported.selection,
            snapshot = original,
        ))
        assertTrue(imported.saveBlueprint())
        val rewritten = requireNotNull(imported.inventory.getStackInSlot(PlannerState.BLUEPRINT))
        assertEquals(
            "Custom blueprint",
            rewritten.displayName,
        )
        assertEquals(
            original,
            SceneBlueprint.fromItem(rewritten),
        )
        assertEquals(
            null,
            imported.inventory.getStackInSlot(PlannerState.BLUEPRINT_INPUT),
        )
        assertTrue(imported.saveBlueprint())
        val blanks = ItemStack(
            BlankBlueprintItem,
            16,
        )
        imported.inventory.setStackInSlot(
            PlannerState.BLUEPRINT_INPUT,
            blanks,
        )
        imported.reset(imported.selection)
        assertTrue(imported.hasBlueprintInput())
        assertTrue(imported.loadBlueprint())
        assertEquals(
            original,
            imported.plan,
        )
        assertTrue(imported.edit(
            expectedSelection = imported.selection,
            snapshot = plan,
        ))
        assertTrue(imported.saveBlueprint())
        assertEquals(
            16,
            blanks.stackSize,
        )
        assertTrue(blanks === imported.inventory.getStackInSlot(PlannerState.BLUEPRINT_INPUT))
        val updatedBlueprint = requireNotNull(imported.inventory.getStackInSlot(PlannerState.BLUEPRINT))
        assertEquals(
            plan,
            SceneBlueprint.fromItem(updatedBlueprint),
        )
        assertEquals(
            "Custom blueprint",
            updatedBlueprint.displayName,
        )
        val oldSelection = reopened.selection
        reopened.reset(oldSelection)
        assertFalse(reopened.edit(
            expectedSelection = oldSelection,
            snapshot = plan,
        ))
    }

    private fun checkMePlanner(plan: NBTTagCompound) {
        WirelessPlannerChecks.check(plan)
        val state = PlannerState(
            networked = true,
            world = {
                null
            },
            onChanged = {},
        )
        assertTrue(state.select(
            id = "test:machine",
            snapshot = plan,
        ))
        assertEquals(
            5,
            state.inventory.slots,
        )
        state.inventory.setStackInSlot(
            PlannerState.BLUEPRINT_INPUT,
            ItemStack(BlankBlueprintItem),
        )
        val item = ItemStack(MeBlueprintPlannerItem)
        val part = MeBlueprintPlannerItem.createPartFromItemStack(item) as MeBlueprintPlannerPart
        assertTrue(GridFlags.REQUIRE_CHANNEL in part.proxy.flags)
        assertFalse(part.proxy.isActive)
        part.readFromNBT(NBTTagCompound().also(state::writeToNBT))
        val inventory = MePlannerInventory(part)
        assertTrue(inventory.isItemValidForSlot(
            PlannerState.BLUEPRINT,
            ItemStack(StructureBlueprintItem),
        ))
        assertFalse(inventory.isItemValidForSlot(
            PlannerState.BLUEPRINT,
            ItemStack(BlankBlueprintItem),
        ))
        assertEquals(
            5,
            inventory.sizeInventory,
        )
        assertTrue(inventory.getStackInSlot(PlannerState.BLUEPRINT_INPUT)?.item === BlankBlueprintItem)
        assertTrue(inventory.isItemValidForSlot(
            PlannerState.BLUEPRINT_INPUT,
            ItemStack(BlankBlueprintItem),
        ))
        assertFalse(inventory.isItemValidForSlot(
            PlannerState.BLUEPRINT_INPUT,
            ItemStack(Items.paper),
        ))
        val codec = MeBlueprintPlannerContainer.PAYLOAD
        val frame = Unpooled.buffer()
        try {
            val payload = byteArrayOf(4, 9, 12)
            codec.write(
                frame,
                payload,
            )
            frame.writeInt(-1)
            assertTrue(codec.read(frame).contentEquals(payload))
            assertEquals(
                -1,
                frame.readInt(),
            )
        } finally {
            frame.release()
        }
        val saved = NBTTagCompound().also(part::writeToNBT)
        assertEquals(
            plan,
            saved.getCompoundTag("Plan"),
        )
        val drops = mutableListOf<ItemStack>()
        part.getDrops(
            drops,
            true,
        )
        assertTrue(drops.single().item === BlankBlueprintItem)
    }

    private fun checkBlueprintProvider(plan: NBTTagCompound) {
        val pattern = requireNotNull(PlannerPatterns.structureStack(
            item = StructurePatternItem,
            plan = plan,
            recipe = "test:machine",
        ))
        assertFalse(pattern.item is ICraftingPatternItem)
        val materials = NBTTagList().apply {
            appendTag(ItemStack(Items.paper).writeToNBT(NBTTagCompound()).apply {
                setInteger("Amount", 143)
            })
            appendTag(ItemStack(Blocks.iron_block).writeToNBT(NBTTagCompound()).apply {
                setInteger("Amount", 37)
            })
        }
        val inventory = ItemStackHandler(29)
        inventory.setStackInSlot(
            0,
            pattern,
        )
        inventory.setStackInSlot(
            27,
            ItemStack(Items.paper),
        )
        val snapshot = NBTTagCompound().apply {
            setTag(
                "Patterns",
                inventory.serializeNBT(),
            )
            setTag(
                "PendingMaterials",
                materials,
            )
        }
        val provider = BlueprintProviderTile()
        assertTrue(GridFlags.REQUIRE_CHANNEL in provider.proxy.flags)
        assertTrue(provider.canUpdate())
        provider.readFromNBT(snapshot)

        val saved = NBTTagCompound().also(provider::writeToNBT)
        assertEquals(
            39,
            saved.getCompoundTag("Patterns").getInteger("Size"),
        )
        assertTrue(ItemStack.areItemStacksEqual(pattern, provider.inventory.getStackInSlot(0)))
        assertEquals(null, provider.inventory.getStackInSlot(27))
        assertTrue(provider.inventory.getStackInSlot(36)?.item === Items.paper)
        assertEquals(18, provider.capacity())
        assertEquals(
            materials,
            saved.getTag("PendingMaterials"),
        )
        for (slot in 37..38) {
            provider.inventory.setStackInSlot(
                slot,
                ItemStack(Items.paper),
            )
        }
        assertEquals(36, provider.capacity())
        provider.inventory.setStackInSlot(
            35,
            pattern.copy(),
        )
        assertFalse(provider.canRemoveUpgrade())
    }

    private fun checkReactorCasings(controller: ItemStack) {
        GregTechScenes.create(
            controller = controller,
            values = emptyMap(),
        ).use { scene ->
            assertTrue(
                scene.diagnostics.toString(),
                scene.diagnostics.isEmpty(),
            )
            assertEquals(
                0,
                hatchCount(scene),
            )
            assertEquals(
                27,
                scene.positions().size,
            )
            assertTrue(!SceneView(scene).slice.enabled)
            val shell = ScenePosition(
                x = -1,
                y = 64,
                z = 0,
            )
            val shellOptions = scene.elements.getValue(shell).inspectionAlternatives()
            assertTrue(shellOptions.first().isItemEqual(scene.item(shell)))
            val hatches = shellOptions.drop(1).map {
                it.itemDamage
            }.toSet()
            assertEquals(
                (32000..32005).toSet(),
                hatches,
            )
            val pipe = ScenePosition(
                x = 0,
                y = 64,
                z = -1,
            )
            val pipeOptions = scene.elements.getValue(pipe).inspectionAlternatives()
            assertTrue(pipeOptions.single().isItemEqual(scene.item(pipe)))
            assertEquals(
                0,
                hatchCount(scene),
            )
        }
    }

    private fun hatchCount(scene: BlockScene): Int {
        return scene.positions().count {
            val tile = scene.world.getTileEntity(
                x = it.x,
                y = it.y,
                z = it.z,
            ) as? BaseMetaTileEntity
            tile?.metaTileEntity is GT_MetaTileEntity_Hatch
        }
    }

    private fun register(
        id: Int,
        prototype: MetaTileEntity,
    ) {
        GregTech_API.METATILEENTITIES[id] = prototype
        val base = BaseMetaTileEntity()
        prototype.baseMetaTileEntity = base
        base.setMetaTileID(id.toShort())
    }
}
