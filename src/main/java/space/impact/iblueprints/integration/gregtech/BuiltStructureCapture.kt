package space.impact.iblueprints.integration.gregtech

import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase
import java.util.function.BooleanSupplier
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.MinecraftForge
import space.impact.iblueprints.integration.structures.BlueprintDoor
import space.impact.iblueprints.integration.structures.BlueprintPlacement
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.mixins.gregtech.IMultiblockSetupInvoker
import space.impact.iblueprints.scene.ScenePosition
import space.impact.structures.ImpactStructures

internal object BuiltStructureCapture {

    private val current = ThreadLocal<BuiltStructureCollector>()

    internal fun capture(machine: GT_MetaTileEntity_EnhancedMultiBlockBase<*>): NBTTagCompound? {
        val invoker = machine as? IMultiblockSetupInvoker ?: return null
        val tile = machine.baseMetaTileEntity ?: return null
        return capture(machine = machine) {
            invoker.`iblueprints$setupMachine`(
                tile,
                tile.getStackInSlot(1),
            )
        }
    }

    internal fun capture(
        machine: GT_MetaTileEntity_EnhancedMultiBlockBase<*>,
        recheck: () -> Unit,
    ): NBTTagCompound? {
        if (!machine.mMachine || ImpactStructures.isInstrumentEnabled()) {
            return null
        }
        val tile = machine.baseMetaTileEntity ?: return null
        val world = tile.world ?: return null
        if (world.isRemote) {
            return null
        }
        val collector = BuiltStructureCollector(
            machine = machine,
            world = world,
        )
        MinecraftForge.EVENT_BUS.register(collector)
        try {
            ImpactStructures.enableInstrument(collector)
            current.set(collector)
            try {
                recheck()
            } finally {
                current.remove()
                ImpactStructures.disableInstrument()
            }
        } finally {
            MinecraftForge.EVENT_BUS.unregister(collector)
        }
        if (!machine.mMachine || tile.isInvalidTileEntity) {
            return null
        }
        val origin = ScenePosition(
            x = tile.xCoord,
            y = tile.yCoord.toInt(),
            z = tile.zCoord,
        )
        val blocks = mutableListOf<BlueprintPlacement>()
        for (position in collector.positions) {
            if (position == origin || world.isAirBlock(
                    position.x,
                    position.y,
                    position.z,
                )
            ) {
                continue
            }
            if (!world.blockExists(
                    position.x,
                    position.y,
                    position.z,
                )
            ) {
                return null
            }
            val block = world.getBlock(
                position.x,
                position.y,
                position.z,
            )
            val item = Item.getItemFromBlock(block) as? ItemBlock ?: return null
            val placedTile = world.getTileEntity(
                position.x,
                position.y,
                position.z,
            )
            blocks.add(BlueprintPlacement(
                position = position,
                stack = ItemStack(
                    item,
                    1,
                    block.getDamageValue(
                        world,
                        position.x,
                        position.y,
                        position.z,
                    ),
                ),
                hatchFacing = BlueprintHatch.facing(placedTile),
                doorMetadata = if (BlueprintDoor.isReinforced(block)) {
                    world.getBlockMetadata(
                        position.x,
                        position.y,
                        position.z,
                    )
                } else {
                    null
                },
                hatchTexture = BlueprintHatch.texture(placedTile),
            ))
        }
        return SceneBlueprint.capture(
            controller = machine.getStackForm(1),
            origin = origin,
            facing = machine.extendedFacing,
            blocks = blocks,
            channels = emptyMap(),
        )?.takeIf(SceneBlueprint::canStore)
    }

    public fun checkPiece(
        machine: GT_MetaTileEntity_EnhancedMultiBlockBase<*>,
        check: BooleanSupplier,
    ): Boolean {
        val collector = current.get()
        if (collector == null || collector.machine !== machine) {
            return check.asBoolean
        }
        return collector.checkPiece(check)
    }
}
