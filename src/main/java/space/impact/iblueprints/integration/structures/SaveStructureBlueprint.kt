package space.impact.iblueprints.integration.structures

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import java.io.IOException
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTSizeTracker
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ChatComponentTranslation
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.structures.net.ServerTasks

internal class SaveStructureBlueprint : IMessage {

    private var slot = -1
    private var expectedStack = 0
    private var data = ByteArray(0)

    override fun fromBytes(buf: ByteBuf) {
        slot = buf.readUnsignedByte().toInt()
        expectedStack = buf.readInt()
        require(buf.readableBytes() in 1..MAX_BYTES)
        data = ByteArray(buf.readableBytes())
        buf.readBytes(data)
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeByte(slot)
        buf.writeInt(expectedStack)
        buf.writeBytes(data)
    }

    internal fun updatedStack(current: ItemStack?): ItemStack? {
        if (current?.item !== StructureBlueprintItem || SaveProjectorChannels.fingerprint(current) != expectedStack) {
            return null
        }
        try {
            val plan = CompressedStreamTools.func_152457_a(
                data,
                NBTSizeTracker(2_097_152L),
            )
            if (!SceneBlueprint.isValid(plan)) {
                return null
            }
            val updated = current.copy()
            val tag = updated.tagCompound ?: NBTTagCompound()
            tag.setTag(
                StructureBlueprintItem.PLAN_TAG,
                plan,
            )
            updated.tagCompound = tag
            return updated.takeIf {
                CompressedStreamTools.compress(tag).size <= MAX_BYTES
            }
        } catch (_: IOException) {
            return null
        } catch (_: RuntimeException) {
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
            player.addChatComponentMessage(ChatComponentTranslation("iblueprints.blueprint.failed"))
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
        player.addChatComponentMessage(ChatComponentTranslation("iblueprints.blueprint.saved"))
    }

    public class Handler : IMessageHandler<SaveStructureBlueprint, IMessage> {

        override fun onMessage(
            message: SaveStructureBlueprint,
            context: MessageContext,
        ): IMessage? {
            val player = context.serverHandler.playerEntity
            ServerTasks.enqueue {
                message.applyTo(player)
            }
            return null
        }
    }

    internal companion object {

        // The item must also fit into Minecraft's signed-short NBT packet field.
        private const val MAX_BYTES = 30_000

        internal fun create(
            slot: Int,
            stack: ItemStack,
            plan: NBTTagCompound,
        ): SaveStructureBlueprint? {
            val encoded = CompressedStreamTools.compress(plan)
            if (encoded.size > MAX_BYTES) {
                return null
            }
            return SaveStructureBlueprint().apply {
                this.slot = slot
                expectedStack = SaveProjectorChannels.fingerprint(stack)
                data = encoded
            }
        }
    }
}
