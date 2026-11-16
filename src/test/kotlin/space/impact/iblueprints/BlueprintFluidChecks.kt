package space.impact.iblueprints

import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase
import java.util.function.Predicate
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.BlockFluidClassic
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.ItemFluidContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import space.impact.iblueprints.integration.gregtech.BuiltStructureCapture
import space.impact.iblueprints.integration.gregtech.GregTechScenes
import space.impact.iblueprints.integration.structures.BlueprintPlacement
import space.impact.iblueprints.integration.structures.BlueprintPlacementResult
import space.impact.iblueprints.integration.structures.BlueprintWorldUse
import space.impact.iblueprints.integration.structures.PreviewItemSource
import space.impact.iblueprints.integration.structures.PreviewPlayer
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.SceneView
import space.impact.iblueprints.scene.world.SceneWorld
import space.impact.structures.StructureEvent
import space.impact.structures.alignment.enumerable.ExtendedFacing
import space.impact.structures.structure.IItemSource
import space.impact.structures.structure.StructureUtility
import space.impact.structures.util.InventoryUtility

internal object BlueprintFluidChecks {

    private val origin = ScenePosition(
        x = 0,
        y = 64,
        z = 0,
    )
    private val water = BlueprintPlacement(
        position = origin.copy(x = 1),
        stack = null,
        hatchFacing = null,
        fluidBlock = Blocks.water,
    )

    internal fun run(serverPlayer: EntityPlayerMP) {
        // Initialize Forge's vanilla container registrations before testing custom containers.
        FluidContainerRegistry.getFluidForFilledItem(ItemStack(Items.water_bucket))
        checkPlan()
        checkContainers()
        checkBulkFluid(serverPlayer)
    }

    internal fun checkCapture(scene: BlockScene) {
        val origin = requireNotNull(scene.simulation).origin
        val world = scene.world
        val tile = world.getTileEntity(
            x = origin.x,
            y = origin.y,
            z = origin.z,
        ) as IGregTechTileEntity
        val machine = tile.metaTileEntity as GT_MetaTileEntity_EnhancedMultiBlockBase<*>
        val fluid = Fluid("blueprint_test_fluid")
        assertTrue(FluidRegistry.registerFluid(fluid))
        val customBlock = BlockFluidClassic(
            fluid,
            Material.water,
        )
        Block.blockRegistry.addObject(
            3008,
            "scene_test:blueprint_fluid",
            customBlock,
        )
        fluid.setBlock(customBlock)
        val waterPosition = origin.copy(x = origin.x + 100)
        val customPosition = waterPosition.copy(x = waterPosition.x + 1)
        scene.block(
            position = waterPosition,
            block = Blocks.water,
        )
        scene.block(
            position = customPosition,
            block = customBlock,
            metadata = 2,
        )
        val wasRemote = world.isRemote
        val wasFormed = machine.mMachine
        try {
            val plan = requireNotNull(SceneBlueprint.capture(
                scene = scene,
                channels = emptyMap(),
            ))
            world.isRemote = false
            machine.mMachine = true
            val copied = requireNotNull(BuiltStructureCapture.capture(machine = machine) {
                for (position in scene.positions()) {
                    StructureEvent.StructureElementVisitedEvent.fireEvent(
                        world,
                        position.x,
                        position.y,
                        position.z,
                        0,
                        0,
                        0,
                        StructureUtility.ofBlock<Any>(
                            world.getBlock(
                                position.x,
                                position.y,
                                position.z,
                            ),
                            world.getBlockMetadata(
                                position.x,
                                position.y,
                                position.z,
                            ),
                        ),
                    )
                }
            })
            for (saved in listOf(
                plan,
                copied,
            )) {
                val fluids = SceneBlueprint.placements(
                    plan = saved,
                    origin = origin,
                    facing = machine.extendedFacing,
                ).filter {
                    it.fluidBlock != null
                }
                assertEquals(
                    2,
                    fluids.size,
                )
                assertSame(
                    customBlock,
                    fluids.single {
                        it.position == customPosition
                    }.fluidBlock,
                )
                GregTechScenes.create(
                    controller = requireNotNull(SceneBlueprint.controller(saved)),
                    values = emptyMap(),
                    savedPlan = saved,
                ).use { restored ->
                    assertSame(
                        Blocks.water,
                        restored.world.blockState(waterPosition)?.block,
                    )
                    assertSame(
                        customBlock,
                        restored.world.blockState(customPosition)?.block,
                    )
                    assertEquals(
                        2,
                        restored.world.getBlockMetadata(
                            customPosition.x,
                            customPosition.y,
                            customPosition.z,
                        ),
                    )
                    assertSame(
                        Items.water_bucket,
                        restored.item(waterPosition)?.item,
                    )
                    restored.block(
                        position = waterPosition,
                        block = Blocks.stone,
                    )
                    val view = SceneView(restored)
                    SceneBlueprint.restore(
                        view = view,
                        plan = saved,
                    )
                    assertSame(
                        Blocks.water,
                        restored.world.blockState(waterPosition)?.block,
                    )
                }
            }
        } finally {
            machine.mMachine = wasFormed
            world.isRemote = wasRemote
            for (position in listOf(
                waterPosition,
                customPosition,
            )) {
                scene.block(
                    position = position,
                    block = Blocks.air,
                )
            }
        }
    }

    private fun checkPlan() {
        val facing = ExtendedFacing.of(ForgeDirection.SOUTH)
        val plan = requireNotNull(SceneBlueprint.capture(
            controller = ItemStack(Blocks.stone),
            origin = origin,
            facing = facing,
            blocks = listOf(
                water,
                water.copy(
                    position = origin.copy(z = 2),
                    fluidBlock = Blocks.flowing_lava,
                    fluidMetadata = 3,
                ),
            ),
            channels = emptyMap(),
        ))
        assertTrue(SceneBlueprint.canStore(plan))
        val restored = SceneBlueprint.placements(
            plan = plan,
            origin = origin,
            facing = facing,
        )
        assertEquals(
            water,
            restored.first(),
        )
        assertSame(
            Blocks.flowing_lava,
            restored.last().fluidBlock,
        )
        assertEquals(
            3,
            restored.last().fluidMetadata,
        )
        val rotated = SceneBlueprint.placements(
            plan = plan,
            origin = origin,
            facing = ExtendedFacing.of(ForgeDirection.NORTH),
        )
        assertEquals(
            origin.copy(x = -1),
            rotated.first().position,
        )
        val materials = requireNotNull(SceneBlueprint.materials(plan))
        assertTrue(materials.any {
            it.item === Items.water_bucket && it.stackSize == 1
        })
        assertTrue(materials.any {
            it.item === Items.lava_bucket && it.stackSize == 1
        })
        val palette = plan.getTagList(
            "palette",
            10,
        )
        palette.getCompoundTagAt(0).setString(
            "fluidBlock",
            "minecraft:stone",
        )
        assertFalse(SceneBlueprint.isValid(plan))
        palette.getCompoundTagAt(0).setString(
            "fluidBlock",
            "minecraft:water",
        )
        palette.getCompoundTagAt(0).setInteger(
            "fluidMeta",
            16,
        )
        assertFalse(SceneBlueprint.isValid(plan))
    }

    private fun checkContainers() {
        val inventory = InventoryBasic(
            "Blueprint fluids",
            true,
            1,
        )
        val world = SceneWorld()
        world.isRemote = false
        val player = PreviewPlayer(world)
        val source = IItemSource.fromInventory(inventory)
        inventory.setInventorySlotContents(
            0,
            ItemStack(Items.water_bucket),
        )
        assertEquals(
            BlueprintPlacementResult.PLACED,
            BlueprintWorldUse.place(
                placement = water,
                world = world,
                player = player,
                source = source,
            ),
        )
        assertNull(inventory.getStackInSlot(0))
        assertSame(
            Items.bucket,
            player.inventory.getStackInSlot(0).item,
        )
        assertSame(
            Blocks.water,
            world.getBlock(
                1,
                64,
                0,
            ),
        )

        val tank = ItemFluidContainer(
            0,
            4000,
        )
        val filled = ItemStack(tank)
        tank.fill(
            filled,
            FluidStack(
                FluidRegistry.WATER,
                2500,
            ),
            true,
        )
        inventory.setInventorySlotContents(
            0,
            filled,
        )
        val next = water.copy(position = origin.copy(x = 2))
        assertEquals(
            BlueprintPlacementResult.PLACED,
            BlueprintWorldUse.place(
                placement = next,
                world = world,
                player = player,
                source = source,
            ),
        )
        assertNull(inventory.getStackInSlot(0))
        assertEquals(
            2500,
            tank.getFluid(filled).amount,
        )
        assertEquals(
            1500,
            tank.getFluid(player.inventory.getStackInSlot(1)).amount,
        )

        inventory.setInventorySlotContents(
            0,
            ItemStack(Items.lava_bucket),
        )
        assertEquals(
            BlueprintPlacementResult.MISSING_ITEM,
            BlueprintWorldUse.place(
                placement = water.copy(position = origin.copy(x = 3)),
                world = world,
                player = player,
                source = source,
            ),
        )
        assertSame(
            Items.lava_bucket,
            inventory.getStackInSlot(0).item,
        )
        assertSame(
            Blocks.air,
            world.getBlock(
                3,
                64,
                0,
            ),
        )
        world.clear()
    }

    private fun checkBulkFluid(serverPlayer: EntityPlayerMP) {
        var stock = 2000
        var allowCommit = false
        val source = object : IItemSource {

            override fun take(
                predicate: Predicate<ItemStack>,
                simulate: Boolean,
                count: Int,
            ): Map<ItemStack, Int> {
                return emptyMap()
            }

            override fun takeFluid(
                fluid: FluidStack,
                simulate: Boolean,
            ): Boolean {
                assertSame(
                    FluidRegistry.WATER,
                    fluid.getFluid(),
                )
                assertEquals(
                    1000,
                    fluid.amount,
                )
                if (stock < fluid.amount || !simulate && !allowCommit) {
                    return false
                }
                if (!simulate) {
                    stock -= fluid.amount
                }
                return true
            }
        }
        BlockScene("Fluid rollback").use { scene ->
            val world = scene.world
            world.isRemote = false
            val player = PreviewPlayer(world)
            scene.block(
                position = water.position,
                block = Blocks.tallgrass,
                metadata = 1,
            )
            assertEquals(
                BlueprintPlacementResult.EXTRACTION_FAILED,
                BlueprintWorldUse.place(
                    placement = water,
                    world = world,
                    player = player,
                    source = source,
                ),
            )
            assertSame(
                Blocks.tallgrass,
                world.getBlock(
                    1,
                    64,
                    0,
                ),
            )
            assertEquals(
                1,
                world.getBlockMetadata(
                    1,
                    64,
                    0,
                ),
            )
            assertEquals(
                2000,
                stock,
            )
            allowCommit = true
            assertEquals(
                BlueprintPlacementResult.PLACED,
                BlueprintWorldUse.place(
                    placement = water,
                    world = world,
                    player = player,
                    source = source,
                ),
            )
            assertEquals(
                1000,
                stock,
            )
            assertSame(
                Blocks.water,
                world.getBlock(
                    1,
                    64,
                    0,
                ),
            )
            assertNull(player.inventory.getStackInSlot(0))
            assertEquals(
                BlueprintPlacementResult.PLACED,
                BlueprintWorldUse.place(
                    placement = water.copy(position = origin.copy(x = 2)),
                    world = world,
                    player = player,
                    source = null,
                ),
            )
            assertEquals(
                1000,
                stock,
            )
        }
        assertTrue(PreviewItemSource(emptyList()).takeFluid(
            fluid = FluidStack(
                FluidRegistry.WATER,
                1000,
            ),
            simulate = true,
        ))
        val terminal = Items.stick
        val extractor = object : InventoryUtility.ItemStackExtractor {

            override fun takeFromStack(
                predicate: Predicate<ItemStack>,
                simulate: Boolean,
                count: Int,
                counter: InventoryUtility.ItemStackCounter,
                filter: ItemStack?,
                stack: ItemStack,
                player: EntityPlayerMP,
            ): Int {
                return 0
            }

            override fun takeFluid(
                stack: ItemStack,
                fluid: FluidStack,
                simulate: Boolean,
                player: EntityPlayerMP,
            ): Boolean {
                return player === serverPlayer && source.takeFluid(
                    fluid,
                    simulate,
                )
            }

            override fun isValidSource(
                stack: ItemStack,
                player: EntityPlayerMP,
            ): Boolean {
                return player === serverPlayer && stack.item === terminal
            }

            override fun isAPIImplemented(type: InventoryUtility.ItemStackExtractor.APIType): Boolean {
                return type == InventoryUtility.ItemStackExtractor.APIType.IS_VALID_SOURCE ||
                    type == InventoryUtility.ItemStackExtractor.APIType.MAIN
            }
        }
        InventoryUtility.registerStackExtractor(
            "blueprint-fluid-test",
            extractor,
        )
        val slot = serverPlayer.inventory.firstEmptyStack
        serverPlayer.inventory.setInventorySlotContents(
            slot,
            ItemStack(terminal),
        )
        try {
            val playerSource = IItemSource.fromPlayer(serverPlayer)
            assertTrue(playerSource.takeFluid(
                FluidStack(
                    FluidRegistry.WATER,
                    1000,
                ),
                true,
            ))
            assertEquals(
                1000,
                stock,
            )
            assertTrue(playerSource.takeFluid(
                FluidStack(
                    FluidRegistry.WATER,
                    1000,
                ),
                false,
            ))
            assertEquals(
                0,
                stock,
            )
            assertFalse(playerSource.takeFluid(
                FluidStack(
                    FluidRegistry.WATER,
                    1000,
                ),
                false,
            ))
            assertSame(
                terminal,
                serverPlayer.inventory.getStackInSlot(slot).item,
            )
        } finally {
            serverPlayer.inventory.setInventorySlotContents(
                slot,
                null,
            )
        }
    }
}
