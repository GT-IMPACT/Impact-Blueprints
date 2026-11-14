package space.impact.iblueprints

import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase
import ic2.core.block.BlockIC2Door
import ic2.core.init.InternalName
import ic2.core.item.block.ItemIC2Door
import java.util.function.BooleanSupplier
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import net.minecraftforge.common.util.ForgeDirection
import space.impact.iblueprints.integration.gregtech.BlueprintHatch
import space.impact.iblueprints.integration.gregtech.BuiltStructureCapture
import space.impact.iblueprints.integration.gregtech.CopyBuiltStructure
import space.impact.iblueprints.integration.gregtech.GregTechScenes
import space.impact.iblueprints.integration.structures.BlueprintPlacement
import space.impact.iblueprints.integration.structures.BlueprintPlacementResult
import space.impact.iblueprints.integration.structures.BlueprintWorldUse
import space.impact.iblueprints.integration.structures.PreviewPlayer
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.item.BlankBlueprintItem
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.SceneView
import space.impact.structures.ImpactStructures
import space.impact.structures.StructureEvent
import space.impact.structures.alignment.enumerable.ExtendedFacing
import space.impact.structures.structure.IItemSource
import space.impact.structures.structure.StructureUtility

internal object BuiltStructureCaptureChecks {

    internal fun checkDoorLayout() {
        val block = BlockIC2Door(InternalName.blockDoorAlloy)
        val item = ItemIC2Door(
            InternalName.itemDoorAlloy,
            block,
        )
        val origin = ScenePosition(
            x = 0,
            y = 64,
            z = 0,
        )
        val lower = origin.copy(x = 1)
        val upper = lower.copy(y = lower.y + 1)
        val facing = ExtendedFacing.of(ForgeDirection.SOUTH)
        val placements = listOf(
            BlueprintPlacement(
                position = upper,
                stack = ItemStack(block),
                hatchFacing = null,
                doorMetadata = 9,
            ),
            BlueprintPlacement(
                position = lower,
                stack = ItemStack(block),
                hatchFacing = null,
                doorMetadata = 5,
            ),
        )
        val plan = requireNotNull(SceneBlueprint.capture(
            controller = ItemStack(Blocks.stone),
            origin = origin,
            facing = facing,
            blocks = placements,
            channels = emptyMap(),
        ))
        val material = requireNotNull(SceneBlueprint.materials(plan)).single {
            it.item === item
        }
        assertEquals(
            1,
            material.stackSize,
        )
        val restored = SceneBlueprint.placements(
            plan = plan,
            origin = origin,
            facing = facing,
        )
        assertEquals(
            listOf(9, 5),
            restored.map(BlueprintPlacement::doorMetadata),
        )
        val rotated = SceneBlueprint.placements(
            plan = plan,
            origin = origin,
            facing = ExtendedFacing.of(ForgeDirection.WEST),
        )
        assertEquals(
            listOf(9, 6),
            rotated.map(BlueprintPlacement::doorMetadata),
        )
        BlockScene("Door").use { scene ->
            for (placement in restored) {
                scene.block(
                    position = placement.position,
                    block = block,
                    metadata = requireNotNull(placement.doorMetadata),
                )
            }
            assertSame(
                item,
                scene.item(upper)?.item,
            )
            assertTrue(scene.isUpperDoorHalf(upper))
            assertFalse(scene.isUpperDoorHalf(lower))
            assertEquals(
                setOf(lower, upper),
                SceneView(scene).materials.single().positions.toSet(),
            )
        }
        BlockScene("Door placement").use { scene ->
            val world = scene.world
            world.isRemote = false
            scene.block(
                position = lower.copy(y = lower.y - 1),
                block = Blocks.stone,
            )
            val player = PreviewPlayer(world)
            var remaining = 1
            var acceptExtraction = false
            val source = IItemSource { predicate, simulate, _ ->
                assertTrue(predicate.test(material))
                if (simulate) {
                    mapOf(material to remaining)
                } else if (acceptExtraction) {
                    remaining--
                    mapOf(material to 1)
                } else {
                    emptyMap()
                }
            }
            assertEquals(
                BlueprintPlacementResult.EXTRACTION_FAILED,
                BlueprintWorldUse.placeDoor(
                    lower = restored[1],
                    upper = restored[0],
                    world = world,
                    player = player,
                    source = source,
                ),
            )
            assertEquals(
                listOf(lower.copy(y = lower.y - 1)),
                scene.positions(),
            )
            acceptExtraction = true
            assertEquals(
                BlueprintPlacementResult.PLACED,
                BlueprintWorldUse.placeDoor(
                    lower = restored[1],
                    upper = restored[0],
                    world = world,
                    player = player,
                    source = source,
                ),
            )
            assertEquals(
                0,
                remaining,
            )
            assertEquals(
                5,
                world.blockState(lower)?.metadata,
            )
            assertEquals(
                9,
                world.blockState(upper)?.metadata,
            )
        }
        val legacy = requireNotNull(SceneBlueprint.capture(
            controller = ItemStack(Blocks.stone),
            origin = origin,
            facing = facing,
            blocks = placements.map {
                it.copy(doorMetadata = null)
            },
            channels = emptyMap(),
        ))
        assertEquals(
            listOf(8, 0),
            SceneBlueprint.placements(
                plan = legacy,
                origin = origin,
                facing = facing,
            ).map(BlueprintPlacement::doorMetadata),
        )
    }

    internal fun checkLayout(scene: BlockScene) {
        val plan = requireNotNull(SceneBlueprint.capture(
            scene = scene,
            channels = emptyMap(),
        ))
        GregTechScenes.create(
            controller = requireNotNull(SceneBlueprint.controller(plan)),
            values = emptyMap(),
            savedPlan = plan,
        ).use { restored ->
            assertEquals(
                scene.positions().toSet(),
                restored.positions().toSet(),
            )
            for (position in scene.positions()) {
                assertTrue(scene.item(position)?.isItemEqual(restored.item(position)) == true)
                assertEquals(
                    scene.simulation?.hatchFacing(position),
                    restored.simulation?.hatchFacing(position),
                )
            }
        }
    }

    internal fun run(scene: BlockScene) {
        val origin = requireNotNull(scene.simulation).origin
        val world = scene.world
        val tile = world.getTileEntity(
            x = origin.x,
            y = origin.y,
            z = origin.z,
        ) as IGregTechTileEntity
        val machine = tile.metaTileEntity as GT_MetaTileEntity_EnhancedMultiBlockBase<*>
        val wasRemote = world.isRemote
        val wasFormed = machine.mMachine
        val previousUpdate = machine.mUpdate
        val previousStartUpCheck = machine.mStartUpCheck
        val structurePositions = scene.positions().toSet()
        val neighbor = ScenePosition(
            x = origin.x + 10,
            y = origin.y,
            z = origin.z + 10,
        )
        scene.block(
            position = neighbor,
            block = Blocks.stone,
        )
        world.isRemote = false
        try {
            var checked = false
            assertEquals(
                null,
                BuiltStructureCapture.capture(machine = machine) {
                    checked = true
                },
            )
            assertFalse(checked)

            machine.mMachine = true
            val plan = requireNotNull(BuiltStructureCapture.capture(machine = machine) {
                // A failed optional piece must not add unrelated neighboring blocks.
                BuiltStructureCapture.checkPiece(
                    machine,
                    BooleanSupplier {
                        StructureEvent.StructureElementVisitedEvent.fireEvent(
                            world,
                            neighbor.x,
                            neighbor.y,
                            neighbor.z,
                            0,
                            0,
                            0,
                            StructureUtility.ofBlock<Any>(
                                Blocks.stone,
                                0,
                            ),
                        )
                        false
                    },
                )
                BuiltStructureCapture.checkPiece(
                    machine,
                    BooleanSupplier {
                        machine.mUpdate = 1
                        machine.mStartUpCheck = 100
                        machine.onPostTick(
                            tile,
                            1,
                        )
                        machine.mMachine
                    },
                )
            })
            assertFalse(ImpactStructures.isInstrumentEnabled())
            val placements = SceneBlueprint.placements(
                plan = plan,
                origin = origin,
                facing = machine.extendedFacing,
            )
            assertEquals(
                structurePositions.filterNot(origin::equals).toSet(),
                placements.map {
                    it.position
                }.toSet(),
            )
            for ((position, stack, direction) in placements) {
                assertTrue(stack.isItemEqual(scene.item(position)))
                assertEquals(
                    BlueprintHatch.facing(world.getTileEntity(
                        x = position.x,
                        y = position.y,
                        z = position.z,
                    )),
                    direction,
                )
            }
            GregTechScenes.create(
                controller = requireNotNull(SceneBlueprint.controller(plan)),
                values = emptyMap(),
                savedPlan = plan,
            ).use { restored ->
                assertEquals(
                    structurePositions,
                    restored.positions().toSet(),
                )
                for (position in structurePositions) {
                    assertTrue(scene.item(position)?.isItemEqual(restored.item(position)) == true)
                    assertEquals(
                        scene.simulation?.hatchFacing(position),
                        restored.simulation?.hatchFacing(position),
                    )
                }
            }
            checkInventory(
                scene = scene,
                plan = plan,
            )
            val brokenPosition = placements.first {
                it.hatchFacing == null
            }.position
            val previousBlock = requireNotNull(world.blockState(brokenPosition))
            world.setBlock(
                x = brokenPosition.x,
                y = brokenPosition.y,
                z = brokenPosition.z,
                block = Blocks.air,
                meta = 0,
                flags = 0,
            )
            try {
                assertEquals(
                    null,
                    BuiltStructureCapture.capture(machine = machine) {
                        machine.mUpdate = 1
                        machine.mStartUpCheck = 100
                        machine.onPostTick(
                            tile,
                            1,
                        )
                    },
                )
            } finally {
                world.setBlockState(
                    position = brokenPosition,
                    state = previousBlock,
                )
            }
            assertFalse(ImpactStructures.isInstrumentEnabled())
            machine.mMachine = true
            var failed = false
            try {
                BuiltStructureCapture.capture(machine = machine) {
                    throw IllegalStateException("Failed structure check")
                }
            } catch (_: IllegalStateException) {
                failed = true
            }
            assertTrue(failed)
            assertFalse(ImpactStructures.isInstrumentEnabled())
        } finally {
            world.setBlock(
                x = neighbor.x,
                y = neighbor.y,
                z = neighbor.z,
                block = Blocks.air,
                meta = 0,
                flags = 0,
            )
            world.isRemote = wasRemote
            machine.mMachine = wasFormed
            machine.mUpdate = previousUpdate
            machine.mStartUpCheck = previousStartUpCheck
        }
    }

    private fun checkInventory(
        scene: BlockScene,
        plan: NBTTagCompound,
    ) {
        val player = PreviewPlayer(scene.world)
        player.inventory.mainInventory[0] = ItemStack(
            BlankBlueprintItem,
            3,
        )
        assertTrue(CopyBuiltStructure.save(
            player = player,
            plan = plan,
        ))
        assertEquals(
            2,
            player.heldItem.stackSize,
        )
        val blueprint = player.inventory.mainInventory.filterNotNull().single {
            it.item === StructureBlueprintItem
        }
        assertEquals(
            plan,
            SceneBlueprint.fromItem(blueprint),
        )
        val before = player.heldItem.copy()
        assertFalse(CopyBuiltStructure.save(
            player = player,
            plan = NBTTagCompound(),
        ))
        assertTrue(ItemStack.areItemStacksEqual(
            before,
            player.heldItem,
        ))
        player.inventory.mainInventory[0] = ItemStack(BlankBlueprintItem)
        assertTrue(CopyBuiltStructure.save(
            player = player,
            plan = plan,
        ))
        assertTrue(player.heldItem.item === StructureBlueprintItem)

        for (slot in player.inventory.mainInventory.indices) {
            player.inventory.mainInventory[slot] = ItemStack(
                Items.paper,
                64,
            )
        }
        player.inventory.mainInventory[0] = ItemStack(
            BlankBlueprintItem,
            2,
        )
        assertFalse(CopyBuiltStructure.save(
            player = player,
            plan = plan,
        ))
        assertEquals(
            2,
            player.heldItem.stackSize,
        )
        player.heldItem.stackSize = 1
        assertTrue(CopyBuiltStructure.save(
            player = player,
            plan = plan,
        ))
        assertTrue(player.heldItem.item === StructureBlueprintItem)
    }
}
