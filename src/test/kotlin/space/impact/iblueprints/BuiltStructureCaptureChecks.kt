package space.impact.iblueprints

import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase
import java.util.function.BooleanSupplier
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import space.impact.iblueprints.integration.gregtech.BlueprintHatch
import space.impact.iblueprints.integration.gregtech.BuiltStructureCapture
import space.impact.iblueprints.integration.gregtech.CopyBuiltStructure
import space.impact.iblueprints.integration.gregtech.GregTechScenes
import space.impact.iblueprints.integration.structures.PreviewPlayer
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.item.BlankBlueprintItem
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.ScenePosition
import space.impact.structures.ImpactStructures
import space.impact.structures.StructureEvent
import space.impact.structures.structure.StructureUtility

internal object BuiltStructureCaptureChecks {

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
