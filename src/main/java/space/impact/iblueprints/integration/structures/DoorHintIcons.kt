package space.impact.iblueprints.integration.structures

import net.minecraft.block.Block
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.IIcon
import net.minecraftforge.common.util.ForgeDirection
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.world.SceneWorld

internal object DoorHintIcons {

    internal fun resolve(placements: List<BlueprintPlacement>): Map<ScenePosition, Array<IIcon?>> {
        val doors = placements.filter {
            it.doorMetadata != null
        }
        if (doors.isEmpty()) {
            return emptyMap()
        }

        val world = SceneWorld()
        try {
            for ((position, stack, _, metadata) in doors) {
                world.setBlock(
                    x = position.x,
                    y = position.y,
                    z = position.z,
                    block = Block.getBlockFromItem(stack.item),
                    meta = requireNotNull(metadata),
                    flags = 0,
                )
            }
            val iconsByPosition = mutableMapOf<ScenePosition, Array<IIcon?>>()
            for ((position, _, _, metadata) in doors) {
                val (x, y, z) = position
                val block = world.getBlock(
                    x = x,
                    y = y,
                    z = z,
                )
                if (!BlueprintDoor.isReinforced(block)) {
                    continue
                }
                val upper = requireNotNull(metadata) and BlueprintDoor.UPPER_HALF != 0
                val adjoiningSide = if (upper) ForgeDirection.DOWN else ForgeDirection.UP
                val adjoiningY = y + adjoiningSide.offsetY
                val adjoining = world.getBlock(
                    x = x,
                    y = adjoiningY,
                    z = z,
                )
                if (adjoining !== block) {
                    continue
                }
                val adjoiningMeta = world.getBlockMetadata(
                    x = x,
                    y = adjoiningY,
                    z = z,
                )
                if ((adjoiningMeta and BlueprintDoor.UPPER_HALF != 0) == upper) {
                    continue
                }
                iconsByPosition[position] = faces(
                    world = world,
                    block = block,
                    position = position,
                    adjoiningSide = adjoiningSide,
                )
            }
            return iconsByPosition
        } finally {
            world.clear()
        }
    }

    private fun faces(
        world: SceneWorld,
        block: Block,
        position: ScenePosition,
        adjoiningSide: ForgeDirection,
    ): Array<IIcon?> {
        val (x, y, z) = position
        val minX = block.blockBoundsMinX
        val minY = block.blockBoundsMinY
        val minZ = block.blockBoundsMinZ
        val maxX = block.blockBoundsMaxX
        val maxY = block.blockBoundsMaxY
        val maxZ = block.blockBoundsMaxZ
        val bounds = try {
            block.setBlockBoundsBasedOnState(
                world,
                x,
                y,
                z,
            )
            AxisAlignedBB.getBoundingBox(
                block.blockBoundsMinX,
                block.blockBoundsMinY,
                block.blockBoundsMinZ,
                block.blockBoundsMaxX,
                block.blockBoundsMaxY,
                block.blockBoundsMaxZ,
            )
        } finally {
            block.setBlockBounds(
                minX.toFloat(),
                minY.toFloat(),
                minZ.toFloat(),
                maxX.toFloat(),
                maxY.toFloat(),
                maxZ.toFloat(),
            )
        }
        return Array(ForgeDirection.VALID_DIRECTIONS.size) { index ->
            val side = ForgeDirection.VALID_DIRECTIONS[index]
            if (side == adjoiningSide) {
                null
            } else {
                DoorHintFace(
                    bounds = bounds,
                    side = side,
                    icon = block.getIcon(
                        world,
                        x,
                        y,
                        z,
                        index,
                    ),
                )
            }
        }
    }
}
