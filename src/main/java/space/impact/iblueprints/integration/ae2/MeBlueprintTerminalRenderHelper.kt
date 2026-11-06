package space.impact.iblueprints.integration.ae2

import appeng.api.parts.IPartRenderHelper
import appeng.client.texture.CableBusTextures
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.util.IIcon
import net.minecraftforge.common.util.ForgeDirection

// The AE2 part API exposes the front layers through a fixed texture enum.
@SideOnly(Side.CLIENT)
internal class MeBlueprintTerminalRenderHelper(
    private val helper: IPartRenderHelper,
) : IPartRenderHelper by helper {

    override fun renderFace(
        x: Int,
        y: Int,
        z: Int,
        icon: IIcon,
        face: ForgeDirection,
        renderer: RenderBlocks,
    ) {
        helper.renderFace(
            x,
            y,
            z,
            frontIcon(icon),
            face,
            renderer,
        )
    }

    override fun renderInventoryFace(
        icon: IIcon,
        face: ForgeDirection,
        renderer: RenderBlocks,
    ) {
        helper.renderInventoryFace(
            frontIcon(icon),
            face,
            renderer,
        )
    }

    private fun frontIcon(icon: IIcon): IIcon {
        return when {
            icon === CableBusTextures.PartPatternTerm_Bright.icon -> {
                MeBlueprintPlannerItem.brightIcon ?: icon
            }
            icon === CableBusTextures.PartPatternTerm_Dark.icon -> {
                MeBlueprintPlannerItem.darkIcon ?: icon
            }
            else -> icon
        }
    }
}
