package space.impact.iblueprints.integration.ae2

import appeng.container.PrimaryGui
import appeng.container.implementations.ContainerMEMonitorable
import appeng.container.slot.AppEngSlot
import appeng.container.slot.SlotNormal
import appeng.container.slot.SlotOutput
import appeng.container.sync.ActionHandler
import appeng.container.sync.SyncCodecs
import appeng.container.sync.handlers.ObjectSyncHandler
import appeng.util.Platform
import io.netty.buffer.Unpooled
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketBuffer
import space.impact.iblueprints.planner.PlannerSession
import space.impact.iblueprints.planner.PlannerSlotHint
import space.impact.iblueprints.planner.tracePlannerStep
import java.io.IOException

internal class MeBlueprintPlannerContainer(
    inventory: InventoryPlayer,
    private val part: IMeBlueprintPlannerHost,
) : ContainerMEMonitorable(
    inventory,
    part,
    false,
) {

    internal val state = part.planner
    private val plannerInventory = MePlannerInventory(part)
    private val sync = syncRegistrar().child("blueprint")
    private val updates: ObjectSyncHandler<ByteArray> = sync.objectS2C(
        "state",
        PAYLOAD,
        ByteArray(0),
    )
    private val actions: ActionHandler<ByteArray> = sync.actionC2S(
        "action",
        PAYLOAD,
    )

    internal val planner = PlannerSession(
        state = state,
        player = {
            inventory.player
        },
        isOpen = {
            inventory.player.openContainer === this && canInteractWith(inventory.player)
        },
        sendToServer = { id, write ->
            actions.send(writePacket(
                id = id,
                write = write,
            ))
        },
        sendToClient = { id, write ->
            updates.set(writePacket(
                id = id,
                write = write,
            ))
        },
    )

    internal val blueprintSlots = buildMap<PlannerSlotHint, AppEngSlot> {
        addPlannerSlot(
            hint = PlannerSlotHint.PROJECTOR,
            x = 90,
            y = 30,
        )
        addPlannerSlot(
            hint = PlannerSlotHint.BLUEPRINT_INPUT,
            x = 90,
            y = 49,
        )
        addPlannerSlot(
            hint = PlannerSlotHint.BLUEPRINT,
            x = 128,
            y = 49,
            output = true,
        )
        addPlannerSlot(
            hint = PlannerSlotHint.BLANK_PATTERN,
            x = 90,
            y = 68,
        )
        addPlannerSlot(
            hint = PlannerSlotHint.PATTERN,
            x = 128,
            y = 68,
            output = true,
        )
    }

    init {
        openContext = part.plannerContext()
        bindPlayerInventory(
            inventory,
            0,
            0,
        )
        updates.onClientChange { _, packet ->
            tracePlannerStep(
                step = "me.receiveState",
                details = "window=$windowId bytes=${packet.size}",
            ) {
                readPacket(
                    bytes = packet,
                    read = planner::readOnClient,
                )
            }
        }
        actions.onServerAction { packet ->
            tracePlannerStep(
                step = "me.receiveAction",
                details = "window=$windowId bytes=${packet.size}",
            ) {
                readPacket(
                    bytes = packet,
                    read = planner::readOnServer,
                )
            }
        }
    }

    override fun canInteractWith(player: EntityPlayer): Boolean {
        return super.canInteractWith(player) && part.accessible(player)
    }

    override fun detectAndSendChanges() {
        if (Platform.isServer()) {
            planner.detectAndSendChanges(false)
        }
        super.detectAndSendChanges()
    }

    override fun createPrimaryGui(): PrimaryGui {
        return part.plannerPrimaryGui()
    }

    private fun MutableMap<PlannerSlotHint, AppEngSlot>.addPlannerSlot(
        hint: PlannerSlotHint,
        x: Int,
        y: Int,
        output: Boolean = false,
    ) {
        val slot = if (output) {
            SlotOutput(
                plannerInventory,
                hint.index,
                x,
                y,
                -1,
            )
        } else {
            object : SlotNormal(
                plannerInventory,
                hint.index,
                x,
                y,
            ) {
                override fun isItemValid(stack: ItemStack): Boolean {
                    return super.isItemValid(stack) && plannerInventory.isItemValidForSlot(
                        hint.index,
                        stack,
                    )
                }
            }
        }
        addSlotToContainer(slot)
        this[hint] = slot
    }

    private fun readPacket(
        bytes: ByteArray,
        read: (Int, PacketBuffer) -> Unit,
    ) {
        if (bytes.isEmpty()) {
            return
        }
        val buffer = PacketBuffer(Unpooled.wrappedBuffer(bytes))
        try {
            read(
                buffer.readUnsignedByte().toInt(),
                buffer,
            )
        } catch (failure: IndexOutOfBoundsException) {
            throw IOException("Truncated blueprint packet", failure)
        } finally {
            buffer.release()
        }
    }

    private fun writePacket(
        id: Int,
        write: (PacketBuffer) -> Unit,
    ): ByteArray {
        val buffer = PacketBuffer(Unpooled.buffer())
        try {
            buffer.writeByte(id)
            write(buffer)
            val payload = ByteArray(buffer.readableBytes())
            buffer.readBytes(payload)
            return payload
        } finally {
            buffer.release()
        }
    }

    internal companion object {

        internal val PAYLOAD = SyncCodecs.of(
            "iblueprints.planner",
            { buffer, payload: ByteArray ->
                buffer.writeShort(payload.size)
                buffer.writeBytes(payload)
            },
            { buffer ->
                if (buffer.readableBytes() < Short.SIZE_BYTES) {
                    throw IOException("Missing blueprint payload size")
                }
                val size = buffer.readUnsignedShort()
                if (size > Short.MAX_VALUE || size > buffer.readableBytes()) {
                    throw IOException("Invalid blueprint payload size: $size")
                }
                val payload = ByteArray(size)
                buffer.readBytes(payload)
                payload
            },
            ByteArray::clone,
            ByteArray::contentEquals,
        )
    }
}
