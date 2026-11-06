package space.impact.iblueprints.integration.ae2

import appeng.api.AEApi
import appeng.api.features.IWirelessTermHandler
import appeng.api.implementations.guiobjects.IGuiItemObject
import appeng.container.AEBaseContainer
import appeng.util.Platform
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.Side
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import space.impact.iblueprints.ImpactBlueprintsMod

internal object WirelessPlannerIntegration {

    internal const val GUI_ID = 8
    private lateinit var network: SimpleNetworkWrapper

    internal fun register() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel("iblueprints_wbp")
        network.registerMessage(
            OpenWirelessPlanner.Handler::class.java,
            OpenWirelessPlanner::class.java,
            0,
            Side.SERVER,
        )
    }

    internal fun supports(stack: ItemStack?): Boolean {
        val item = stack?.item ?: return false
        return item is IWirelessTermHandler && GameRegistry.findUniqueIdentifierFor(item)?.modId == "ae2fc"
    }

    internal fun terminalStack(container: AEBaseContainer): ItemStack? {
        return (container.target as? IGuiItemObject)?.itemStack?.takeIf(::supports)
    }

    internal fun request(windowId: Int) {
        network.sendToServer(OpenWirelessPlanner(windowId))
    }

    internal fun open(
        player: EntityPlayer,
        slot: Int,
    ) {
        player.openGui(
            ImpactBlueprintsMod,
            GUI_ID,
            player.worldObj,
            slot,
            0,
            0,
        )
    }

    internal fun createHost(
        player: EntityPlayer,
        slot: Int,
    ): WirelessBlueprintPlannerHost? {
        val stack = Platform.getItemFromPlayerInventoryBySlotIndex(
            player,
            slot,
        ) ?: return null
        if (!supports(stack)) {
            return null
        }
        val handler = AEApi.instance().registries().wireless().getWirelessTerminalHandler(stack) ?: return null
        val host = WirelessBlueprintPlannerHost(
            handler = handler,
            stack = stack,
            player = player,
            slot = slot,
        )
        return host.takeIf {
            it.accessible(player)
        }
    }
}
