package space.impact.iblueprints

import cpw.mods.fml.common.DummyModContainer
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.ModContainer
import cpw.mods.fml.common.ModMetadata
import cpw.mods.fml.common.event.FMLLoadCompleteEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import space.impact.iblueprints.proxy.ClientProxy
import ic2.core.Ic2Items
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_AssemblyLine
import gregtech.api.GregTech_API
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
import gregtech.common.blocks.GT_Item_Machines
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_VacuumFreezer
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_DistillationTower
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_ElectricBlastFurnace
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_HeatExchanger
import gregtech.api.util.GT_Config
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.config.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import space.impact.iblueprints.integration.gregtech.GregTechScenes
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.SceneView
import space.impact.structures.Main
import space.impact.structures.CommonProxy
import java.io.File

internal object GregTechSceneChecks {

    internal fun run() {
        GregTech_API.sMachineFile = GT_Config(Configuration(File("build/test-config/scene-machines.cfg")))
        GregTech_API.sBlockCasings2 = Blocks.wool
        GregTech_API.sBlockCasings4 = Blocks.stained_hardened_clay
        GregTech_API.sBlockCasings1 = Blocks.planks
        GregTech_API.sBlockCasings3 = Blocks.stained_glass_pane
        GregTech_API.sBlockCasings5 = Blocks.stained_glass
        GregTech_API.sBlockCasings8 = Blocks.stonebrick
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
        Ic2Items.reinforcedGlass = ItemStack(Blocks.glass)
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
        }
        checkReactorCasings(ItemStack(
            block,
            1,
            32014,
        ))
        checkAssemblyLine()
        val muffler = GregTech_API.METATILEENTITIES[32006]
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
            }
        }
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
