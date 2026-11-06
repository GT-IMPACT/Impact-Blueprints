package space.impact.iblueprints.integration.ae2

import appeng.helpers.ICustomButtonDataObject
import io.netty.buffer.ByteBuf
import net.minecraft.client.gui.GuiButton
import net.minecraft.nbt.NBTTagCompound

internal class WirelessPlannerButtons(
    private val original: ICustomButtonDataObject?,
    private val openPlanner: () -> Unit,
) : ICustomButtonDataObject {

    private var plannerButton: WirelessPlannerIconButton? = null
    private var terminalButtons: List<GuiButton> = emptyList()
    private var firstRow = 0

    override fun initCustomButtons(
        guiLeft: Int,
        guiTop: Int,
        xSize: Int,
        ySize: Int,
        xOffset: Int,
        yOffset: Int,
        buttonList: MutableList<GuiButton>,
    ) {
        val firstButton = buttonList.size
        original?.initCustomButtons(
            guiLeft,
            guiTop,
            xSize,
            ySize,
            xOffset,
            yOffset,
            buttonList,
        )
        firstRow = yOffset
        terminalButtons = buttonList.drop(firstButton).filter {
            it.xPosition == guiLeft - 18
        }
        val button = WirelessPlannerIconButton(
            x = guiLeft - 18,
            y = yOffset,
        )
        plannerButton = button
        updateLayout()
        buttonList.add(button)
    }

    override fun actionPerformedCustomButtons(button: GuiButton): Boolean {
        if (button === plannerButton) {
            openPlanner()
            return true
        }
        return original?.actionPerformedCustomButtons(button) == true
    }

    override fun readData(tag: NBTTagCompound) {
        original?.readData(tag)
        updateLayout()
    }

    override fun writeData(tag: NBTTagCompound) {
        original?.writeData(tag)
    }

    override fun writeByte(buffer: ByteBuf) {
        original?.writeByte(buffer)
    }

    override fun readByte(buffer: ByteBuf) {
        original?.readByte(buffer)
        updateLayout()
    }

    private fun updateLayout() {
        // AE2FC compacts the visible terminal modes at 20-pixel intervals after each mode sync.
        plannerButton?.yPosition = terminalButtons.filter { it.visible }.maxOfOrNull {
            it.yPosition + 20
        } ?: firstRow
    }
}
