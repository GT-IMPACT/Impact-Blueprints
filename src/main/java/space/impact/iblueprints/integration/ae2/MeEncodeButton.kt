package space.impact.iblueprints.integration.ae2

import appeng.client.gui.widgets.GuiSimpleImgButton
import org.lwjgl.opengl.GL11

internal class MeEncodeButton(
    x: Int,
    y: Int,
    tooltip: String,
) : GuiSimpleImgButton(
    x,
    y,
    ICON,
    tooltip,
) {

    override fun drawTexturedModalRect(
        x: Int,
        y: Int,
        u: Int,
        v: Int,
        width: Int,
        height: Int,
    ) {
        if (u == ICON * 16 && v == 0) {
            // Rotate the arrow while retaining AE2's native frame and disabled tint.
            val centerX = x + width / 2f
            val centerY = y + height / 2f
            GL11.glPushMatrix()
            try {
                GL11.glTranslatef(
                    centerX,
                    centerY,
                    0f,
                )
                GL11.glRotatef(
                    -90f,
                    0f,
                    0f,
                    1f,
                )
                GL11.glTranslatef(
                    -centerX,
                    -centerY,
                    0f,
                )
                super.drawTexturedModalRect(
                    x,
                    y,
                    u,
                    v,
                    width,
                    height,
                )
            } finally {
                GL11.glPopMatrix()
            }
        } else {
            super.drawTexturedModalRect(
                x,
                y,
                u,
                v,
                width,
                height,
            )
        }
    }

    internal companion object {

        internal const val ICON = 8
    }
}
