package space.impact.iblueprints.integration.ae2

import appeng.client.gui.AEBaseGui
import appeng.core.localization.ColorUtils
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.util.StatCollector

internal class BlueprintProviderGui(
    private val provider: BlueprintProviderContainer,
) : AEBaseGui(provider) {

    init {
        xSize = BlueprintProviderContainer.WIDTH
        ySize = BlueprintProviderContainer.HEIGHT
    }

    override fun drawBG(
        offsetX: Int,
        offsetY: Int,
        mouseX: Int,
        mouseY: Int,
    ) {
        bindTexture("guis/interface.png")
        val patternEnd = BlueprintProviderContainer.PATTERN_END
        drawTexturedModalRect(
            offsetX,
            offsetY,
            0,
            0,
            xSize,
            BlueprintProviderContainer.PATTERN_TOP,
        )
        val rows = provider.activeRows()
        for (row in 0 until BlueprintProviderTile.PATTERN_ROWS) {
            val sourceY = if (row < rows) {
                107
            } else {
                89
            }
            drawTexturedModalRect(
                offsetX,
                offsetY + patternEnd - (row + 1) * 18,
                0,
                sourceY,
                xSize,
                18,
            )
        }
        drawTexturedModalRect(
            offsetX,
            offsetY + patternEnd,
            0,
            125,
            xSize,
            ySize - patternEnd,
        )
        val upgradeWidth = BlueprintProviderContainer.UPGRADE_WIDTH
        val upgradeBody = BlueprintProviderContainer.UPGRADE_HEIGHT - 7
        // Use the native frame around the expansion slots and its original bottom edge.
        drawTexturedModalRect(
            offsetX + xSize,
            offsetY,
            177,
            0,
            upgradeWidth,
            upgradeBody,
        )
        drawTexturedModalRect(
            offsetX + xSize,
            offsetY + upgradeBody,
            177,
            79,
            upgradeWidth,
            7,
        )
    }

    override fun drawFG(
        offsetX: Int,
        offsetY: Int,
        mouseX: Int,
        mouseY: Int,
    ) {
        fontRendererObj.drawString(
            getGuiDisplayName(StatCollector.translateToLocal("iblueprints.provider.title")),
            8,
            6,
            ColorUtils.guiTextColorGray.color,
        )
        val localX = mouseX - offsetX
        val localY = mouseY - offsetY
        if (provider.upgradeSlots.any {
                !it.hasStack && localX in it.x until it.x + 16 && localY in it.y until it.y + 16
            }
        ) {
            drawTooltip(
                localX,
                localY,
                StatCollector.translateToLocal("iblueprints.provider.expansion_hint"),
            )
        }
    }

    override fun hideItemPanelSlot(
        gui: GuiContainer,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ): Boolean {
        val upgradeX = guiLeft + xSize
        return x < upgradeX + BlueprintProviderContainer.UPGRADE_WIDTH && x + width > upgradeX &&
            y < guiTop + BlueprintProviderContainer.UPGRADE_HEIGHT && y + height > guiTop
    }
}
