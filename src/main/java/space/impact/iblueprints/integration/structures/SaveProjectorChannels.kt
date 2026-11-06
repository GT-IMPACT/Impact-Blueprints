package space.impact.iblueprints.integration.structures

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import java.util.Locale
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatComponentTranslation
import space.impact.structures.alignment.constructable.ChannelDataAccessor
import space.impact.structures.item.ItemConstructableTrigger
import space.impact.structures.net.ServerTasks

internal class SaveProjectorChannels : IMessage {

    private var slot: Int = -1
    private var expectedStack: Int = 0
    private var channels: Map<String, Int> = emptyMap()

    public constructor()

    internal constructor(
        slot: Int,
        stack: ItemStack,
        channels: Map<String, Int>,
    ) {
        require(isValid(channels))
        this.slot = slot
        this.expectedStack = fingerprint(stack)
        this.channels = channels.toMap()
    }

    override fun fromBytes(buf: ByteBuf) {
        slot = buf.readUnsignedByte().toInt()
        expectedStack = buf.readInt()
        val count = ByteBufUtils.readVarShort(buf)
        require(count in 1..MAX_CHANNELS)
        val decoded = linkedMapOf<String, Int>()
        repeat(count) {
            val name = ByteBufUtils.readUTF8String(buf)
            val value = ByteBufUtils.readVarInt(
                buf,
                5,
            )
            require(decoded.put(
                key = name,
                value = value,
            ) == null)
        }
        require(isValid(decoded))
        channels = decoded
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeByte(slot)
        buf.writeInt(expectedStack)
        ByteBufUtils.writeVarShort(
            buf,
            channels.size,
        )
        for ((name, value) in channels) {
            ByteBufUtils.writeUTF8String(
                buf,
                name,
            )
            ByteBufUtils.writeVarInt(
                buf,
                value,
                5,
            )
        }
    }

    internal fun updatedStack(current: ItemStack?): ItemStack? {
        if (current == null || current.item !is ItemConstructableTrigger || fingerprint(current) != expectedStack) {
            return null
        }
        val updated = current.copy()
        for ((name, value) in channels) {
            ChannelDataAccessor.setChannelData(
                updated,
                name,
                value,
            )
        }
        return updated
    }

    public class Handler : IMessageHandler<SaveProjectorChannels, IMessage> {

        override fun onMessage(
            message: SaveProjectorChannels,
            context: MessageContext,
        ): IMessage? {
            val player = context.serverHandler.playerEntity
            ServerTasks.enqueue {
                message.applyTo(player)
            }
            return null
        }
    }

    private fun applyTo(player: EntityPlayerMP) {
        if (!player.playerNetServerHandler.netManager.isChannelOpen) {
            return
        }
        val inventory = player.inventory
        val updated = updatedStack(inventory.mainInventory.getOrNull(slot))
        if (updated == null) {
            player.addChatComponentMessage(ChatComponentTranslation("iblueprints.projector.changed"))
            return
        }
        inventory.setInventorySlotContents(
            slot,
            updated,
        )
        inventory.markDirty()
        player.openContainer.detectAndSendChanges()
        if (player.openContainer !== player.inventoryContainer) {
            player.sendContainerToPlayer(player.inventoryContainer)
        }
        player.addChatComponentMessage(ChatComponentTranslation("iblueprints.projector.saved"))
    }

    internal companion object {

        private const val MAX_CHANNELS = 256

        internal fun isValid(channels: Map<String, Int>): Boolean {
            return channels.size in 1..MAX_CHANNELS && channels.all { (name, value) ->
                name.isNotBlank() && name.length <= 128 && name == name.lowercase(Locale.ROOT) && value > 0
            }
        }

        internal fun fingerprint(stack: ItemStack): Int {
            var hash = Item.getIdFromItem(stack.item)
            hash = 31 * hash + stack.itemDamage
            hash = 31 * hash + stack.stackSize
            return 31 * hash + (stack.tagCompound?.hashCode() ?: 0)
        }
    }
}
