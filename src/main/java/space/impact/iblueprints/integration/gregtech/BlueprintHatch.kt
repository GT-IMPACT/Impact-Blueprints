package space.impact.iblueprints.integration.gregtech

import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection

internal object BlueprintHatch {

    internal fun facing(tile: TileEntity?): ForgeDirection? {
        val hatch = hatch(tile) ?: return null
        return ForgeDirection.getOrientation(hatch.frontFacing.toInt())
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
