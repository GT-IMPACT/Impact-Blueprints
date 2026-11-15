package space.impact.iblueprints.integration.gregtech

import cpw.mods.fml.common.registry.GameData
import gregtech.api.enums.Casings
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_Cleanroom
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import space.impact.iblueprints.integration.structures.BlueprintDoor
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.world.SceneBlockState
import space.impact.iblueprints.scene.world.SceneWorld
import space.impact.structures.structure.IStructureElement

internal class CleanroomAlternatives(
    scene: BlockScene,
    private val machine: GT_MetaTileEntity_Cleanroom,
    private val trigger: ItemStack,
    private val elements: Map<ScenePosition, IStructureElement<Any>>,
) {

    private val bounds = scene.bounds()

    internal fun at(position: ScenePosition): List<ItemStack> {
        val choices = mutableListOf<ItemStack>()
        val glass = SceneBlockState(
            block = Casings.CLEANROOM_GLASS.block,
            metadata = Casings.CLEANROOM_GLASS.metadata,
            tileData = null,
        )
        val door = BlueprintDoor.material()
        val doorStates = doorStates(position)
        val probe = SceneWorld()
        try {
            probe.setBlockState(
                position = position,
                state = glass,
            )
            if (accepts(
                    probe = probe,
                    position = position,
                )
            ) {
                choices.add(ItemStack(
                    glass.block,
                    1,
                    glass.metadata,
                ))
            }
            if (door != null && doorStates != null && acceptsDoor(
                    probe = probe,
                    states = doorStates,
                )
            ) {
                choices.add(door)
            }
        } finally {
            probe.clear()
        }
        return choices
    }

    internal fun doorStates(position: ScenePosition): Map<ScenePosition, SceneBlockState>? {
        val above = position.copy(y = position.y + 1)
        if (position !in elements || above !in elements || position.y <= bounds.min.y ||
            above.y >= bounds.max.y
        ) {
            return null
        }
        val side = when {
            position.x == bounds.min.x -> ForgeDirection.WEST
            position.x == bounds.max.x -> ForgeDirection.EAST
            position.z == bounds.min.z -> ForgeDirection.NORTH
            position.z == bounds.max.z -> ForgeDirection.SOUTH
            else -> return null
        }
        val block: Block = GameData.getBlockRegistry().getObject(BlueprintDoor.BLOCK_NAME)
        if (!BlueprintDoor.isReinforced(block)) {
            return null
        }
        return linkedMapOf(
            position to SceneBlockState(
                block = block,
                metadata = BlueprintDoor.closedMetadata(side),
                tileData = null,
            ),
            above to SceneBlockState(
                block = block,
                metadata = BlueprintDoor.UPPER_HALF,
                tileData = null,
            ),
        )
    }

    internal fun acceptsDoor(
        probe: SceneWorld,
        states: Map<ScenePosition, SceneBlockState>,
    ): Boolean {
        for ((position, state) in states) {
            probe.setBlockState(
                position = position,
                state = state,
            )
        }
        return states.keys.all {
            accepts(
                probe = probe,
                position = it,
            )
        }
    }

    private fun accepts(
        probe: SceneWorld,
        position: ScenePosition,
    ): Boolean {
        return elements[position]?.couldBeValid(
            machine,
            probe,
            position.x,
            position.y,
            position.z,
            trigger,
        ) == true
    }
}
