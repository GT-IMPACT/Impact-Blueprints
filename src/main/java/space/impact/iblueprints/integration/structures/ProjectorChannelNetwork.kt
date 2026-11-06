package space.impact.iblueprints.integration.structures

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper
import cpw.mods.fml.relauncher.Side
import io.netty.util.AttributeKey
import net.minecraft.network.NetworkManager

internal object ProjectorChannelNetwork {

    private const val CHANNEL = "iblueprints_holo1"
    private const val BLUEPRINT_CHANNEL = "iblueprints_bp1"
    private val remoteSupport = AttributeKey<Boolean>(CHANNEL)
    private val remoteBlueprintSupport = AttributeKey<Boolean>(BLUEPRINT_CHANNEL)
    private lateinit var network: SimpleNetworkWrapper
    private lateinit var blueprints: SimpleNetworkWrapper

    internal fun register() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL)
        network.registerMessage(
            SaveProjectorChannels.Handler::class.java,
            SaveProjectorChannels::class.java,
            0,
            Side.SERVER,
        )
        blueprints = NetworkRegistry.INSTANCE.newSimpleChannel(BLUEPRINT_CHANNEL)
        blueprints.registerMessage(
            SaveStructureBlueprint.Handler::class.java,
            SaveStructureBlueprint::class.java,
            0,
            Side.SERVER,
        )
        FMLCommonHandler.instance().bus().register(this)
    }

    @SubscribeEvent
    public fun onChannelRegistration(event: FMLNetworkEvent.CustomPacketRegistrationEvent<*>) {
        if (event.side == Side.CLIENT && CHANNEL in event.registrations) {
            event.manager.channel().attr(remoteSupport).set(event.operation == "REGISTER")
        }
        if (event.side == Side.CLIENT && BLUEPRINT_CHANNEL in event.registrations) {
            event.manager.channel().attr(remoteBlueprintSupport).set(event.operation == "REGISTER")
        }
    }

    internal fun isAvailable(manager: NetworkManager?): Boolean {
        return manager != null && (manager.isLocalChannel || manager.channel().attr(remoteSupport).get() == true)
    }

    internal fun send(message: SaveProjectorChannels) {
        network.sendToServer(message)
    }

    internal fun canSaveBlueprint(manager: NetworkManager?): Boolean {
        return manager != null && (manager.isLocalChannel || manager.channel().attr(remoteBlueprintSupport).get() == true)
    }

    internal fun sendBlueprint(message: SaveStructureBlueprint) {
        blueprints.sendToServer(message)
    }
}
