package space.impact.iblueprints.integration.ae2

import appeng.container.AEBaseContainer
import appeng.util.Platform
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import space.impact.structures.net.ServerTasks

internal class OpenWirelessPlanner(private var windowId: Int = 0) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        windowId = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(windowId)
    }

    public class Handler : IMessageHandler<OpenWirelessPlanner, IMessage> {

        override fun onMessage(
            message: OpenWirelessPlanner,
            context: MessageContext,
        ): IMessage? {
            val player = context.serverHandler.playerEntity
            ServerTasks.enqueue {
                val container = player.openContainer as? AEBaseContainer ?: return@enqueue
                if (!player.playerNetServerHandler.netManager.isChannelOpen ||
                    container.windowId != message.windowId || !container.canInteractWith(player)
                ) {
                    return@enqueue
                }
                val stack = WirelessPlannerIntegration.terminalStack(container) ?: return@enqueue
                val slot = container.targetSlotIndex
                if (Platform.getItemFromPlayerInventoryBySlotIndex(
                        player,
                        slot,
                    ) !== stack
                ) {
                    return@enqueue
                }
                WirelessPlannerIntegration.open(
                    player = player,
                    slot = slot,
                )
            }
            return null
        }
    }
}
