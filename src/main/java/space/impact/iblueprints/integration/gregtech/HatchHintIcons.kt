package space.impact.iblueprints.integration.gregtech

import gregtech.api.GregTech_API
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch
import gregtech.api.util.GTStructureUtility
import net.minecraft.util.IIcon
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import space.impact.iblueprints.integration.structures.BlueprintPlacement
import space.impact.iblueprints.integration.structures.HatchHintFace
import space.impact.iblueprints.scene.world.SceneWorld

internal object HatchHintIcons {

    internal fun resolve(placement: BlueprintPlacement): Array<IIcon>? {
        val stack = placement.stack
        val prototype = GTStructureUtility.getMetaTileEntity(stack) as? GT_MetaTileEntity_Hatch ?: return null
        val block = GregTech_API.sBlockMachines
        val world = SceneWorld()
        val (x, y, z) = placement.position
        try {
            world.setBlock(
                x = x,
                y = y,
                z = z,
                block = block,
                meta = prototype.tileEntityBaseType.toInt(),
                flags = 0,
            )
            val tile = world.getTileEntity(
                x = x,
                y = y,
                z = z,
            ) as? IGregTechTileEntity ?: return null
            tile.setInitialValuesAsNBT(
                null,
                stack.itemDamage.toShort(),
            )
            val hatch = tile.metaTileEntity as? GT_MetaTileEntity_Hatch ?: return null
            val facing = placement.hatchFacing ?: ForgeDirection.getOrientation(tile.frontFacing.toInt())
            BlueprintHatch.orient(
                tile = tile as TileEntity,
                direction = facing,
            )
            placement.hatchTexture?.let {
                BlueprintHatch.applyTexture(
                    hatch = hatch,
                    texture = it,
                )
            }
            val renderer = HatchIconCollector(world)
            val icons = mutableListOf<IIcon>()
            for (side in ForgeDirection.VALID_DIRECTIONS) {
                val textures = hatch.getTexture(
                    tile,
                    side.ordinal.toByte(),
                    facing.ordinal.toByte(),
                    -1,
                    false,
                    false,
                )
                renderer.icons.clear()
                for (texture in textures) {
                    if (texture == null) {
                        continue
                    }
                    when (side) {
                        ForgeDirection.DOWN -> texture.renderYNeg(renderer, block, x, y, z)
                        ForgeDirection.UP -> texture.renderYPos(renderer, block, x, y, z)
                        ForgeDirection.NORTH -> texture.renderZNeg(renderer, block, x, y, z)
                        ForgeDirection.SOUTH -> texture.renderZPos(renderer, block, x, y, z)
                        ForgeDirection.WEST -> texture.renderXNeg(renderer, block, x, y, z)
                        ForgeDirection.EAST -> texture.renderXPos(renderer, block, x, y, z)
                        else -> error("Unexpected hatch side $side")
                    }
                }
                val base = renderer.icons.firstOrNull() ?: block.getIcon(
                    side.ordinal,
                    prototype.tileEntityBaseType.toInt(),
                )
                icons.add(HatchHintFace(
                    base = base,
                    overlays = renderer.icons.drop(1),
                ))
            }
            return icons.toTypedArray()
        } finally {
            world.clear()
        }
    }
}
