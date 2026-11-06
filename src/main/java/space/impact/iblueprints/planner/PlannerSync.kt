package space.impact.iblueprints.planner

import com.cleanroommc.modularui.value.sync.SyncHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.PacketBuffer

internal class PlannerSync(
    state: PlannerState,
    accessible: (EntityPlayer) -> Boolean,
) : SyncHandler<PlannerSync>() {

    internal val session = PlannerSession(
        state = state,
        player = {
            syncManager.player
        },
        isOpen = {
            isValid && (syncManager.player.worldObj.isRemote ||
                (syncManager.player.openContainer === syncManager.modularSyncManager.container &&
                    accessible(syncManager.player)))
        },
        sendToServer = { id, write ->
            syncToServer(id) {
                write(it)
            }
        },
        sendToClient = { id, write ->
            syncToClient(id) {
                write(it)
            }
        },
    )

    override fun readOnClient(
        id: Int,
        buf: PacketBuffer,
    ) {
        session.readOnClient(
            id = id,
            buf = buf,
        )
    }

    override fun readOnServer(
        id: Int,
        buf: PacketBuffer,
    ) {
        session.readOnServer(
            id = id,
            buf = buf,
        )
    }

    override fun detectAndSendChanges(init: Boolean) {
        session.detectAndSendChanges(init)
    }
}
