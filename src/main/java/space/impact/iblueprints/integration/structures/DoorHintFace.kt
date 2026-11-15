package space.impact.iblueprints.integration.structures

import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.IIcon
import net.minecraftforge.common.util.ForgeDirection

public class DoorHintFace(
    public val bounds: AxisAlignedBB,
    private val side: ForgeDirection,
    private val icon: IIcon,
) : IIcon by icon {

    // ImpactStructures maps the full sprite onto a centered cube. Match the door's cropped edge UVs.
    override fun getMinU(): Float {
        val min = if (side.offsetX != 0) bounds.minZ else bounds.minX
        return icon.getInterpolatedU(min * 16)
    }

    override fun getMaxU(): Float {
        val max = if (side.offsetX != 0) bounds.maxZ else bounds.maxX
        return icon.getInterpolatedU(max * 16)
    }

    override fun getMinV(): Float {
        return if (side.offsetY != 0) {
            icon.getInterpolatedV(bounds.minZ * 16)
        } else {
            icon.minV
        }
    }

    override fun getMaxV(): Float {
        return if (side.offsetY != 0) {
            icon.getInterpolatedV(bounds.maxZ * 16)
        } else {
            icon.maxV
        }
    }

    public fun sprite(): IIcon {
        return icon
    }
}
