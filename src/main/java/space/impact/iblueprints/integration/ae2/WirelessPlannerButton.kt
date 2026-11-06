package space.impact.iblueprints.integration.ae2

import appeng.client.gui.AEBaseGui
import appeng.container.AEBaseContainer
import appeng.helpers.ICustomButtonProvider
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.client.event.GuiScreenEvent

internal object WirelessPlannerButton {

    @SubscribeEvent
    public fun onInit(event: GuiScreenEvent.InitGuiEvent.Pre) {
        val gui = event.gui as? AEBaseGui ?: return
        val container = gui.inventorySlots as? AEBaseContainer ?: return
        if (container is MeBlueprintPlannerContainer || WirelessPlannerIntegration.terminalStack(container) == null) {
            return
        }
        val provider = container.target as? ICustomButtonProvider ?: return
        val original = provider.dataObject
        if (original is WirelessPlannerButtons) {
            return
        }
        provider.dataObject = WirelessPlannerButtons(
            original = original,
            openPlanner = {
                WirelessPlannerIntegration.request(container.windowId)
            },
        )
    }
}
