package space.impact.iblueprints.integration.ae2

import appeng.client.gui.widgets.GuiSimpleImgButton
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
import org.lwjgl.opengl.GL11

internal class WirelessPlannerIconButton(
    x: Int,
    y: Int,
) : GuiSimpleImgButton(
    x,
    y,
    0,
    StatCollector.translateToLocal("iblueprints.planner.me_title"),
) {

    private val pattern = ItemStack(StructurePatternItem)

    override fun drawTexturedModalRect(
        x: Int,
        y: Int,
        u: Int,
        v: Int,
        width: Int,
        height: Int,
    ) {
        if (u != 0 || v != 0) {
            super.drawTexturedModalRect(
                x,
                y,
                u,
                v,
                width,
                height,
            )
            return
        }
        val client = Minecraft.getMinecraft()
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT or GL11.GL_CURRENT_BIT or GL11.GL_LIGHTING_BIT)
        GL11.glPushMatrix()
        try {
            GL11.glTranslatef(
                x + 2f,
                y + 2f,
                0f,
            )
            GL11.glScalef(
                0.75f,
                0.75f,
                1f,
            )
            RenderHelper.enableGUIStandardItemLighting()
            RenderItem.getInstance().renderItemAndEffectIntoGUI(
                client.fontRenderer,
                client.textureManager,
                pattern,
                0,
                0,
            )
        } finally {
            GL11.glPopMatrix()
            GL11.glPopAttrib()
        }
    }
}
