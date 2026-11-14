package space.impact.iblueprints.integration.gregtech

import gregtech.api.enums.Textures
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection

internal object BlueprintHatch {

    internal fun facing(tile: TileEntity?): ForgeDirection? {
        val hatch = hatch(tile) ?: return null
        return ForgeDirection.getOrientation(hatch.frontFacing.toInt())
    }

    internal fun texture(tile: TileEntity?): Int? {
        val hatch = hatch(tile)?.metaTileEntity as? GT_MetaTileEntity_Hatch ?: return null
        return texture(hatch)
    }

    internal fun texture(hatch: GT_MetaTileEntity_Hatch): Int {
        return (hatch.texturePage.toInt() shl 7) or (hatch.updateData.toInt() and 127)
    }

    internal fun applyTexture(
        hatch: GT_MetaTileEntity_Hatch,
        texture: Int,
    ) {
        val page = Textures.BlockIcons.casingTexturePages.getOrNull(texture shr 7)
        if (texture == 0 || page?.getOrNull(texture and 127) != null) {
            hatch.updateTexture(texture)
        }
    }

    internal fun orient(
        tile: TileEntity?,
        direction: ForgeDirection,
    ) {
        val hatch = hatch(tile) ?: return
        val side = direction.ordinal.toByte()
        if (hatch.frontFacing != side && hatch.isValidFacing(side)) {
            hatch.frontFacing = side
        }
    }

    private fun hatch(tile: TileEntity?): IGregTechTileEntity? {
        val machine = tile as? IGregTechTileEntity ?: return null
        return machine.takeIf {
            it.metaTileEntity is GT_MetaTileEntity_Hatch
        }
    }
}
