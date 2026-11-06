package space.impact.iblueprints.planner

import com.cleanroommc.modularui.value.sync.SyncHandler
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ChatComponentTranslation
import space.impact.iblueprints.integration.structures.SceneBlueprint

internal class PlannerSync(private val tile: BlueprintPlannerTile) : SyncHandler<PlannerSync>() {

    private var sentRevision: Long = -1
    private var pendingPlan: NBTTagCompound? = null
    private var editSequence: Int = 0
    private var receivedEdit: Int = 0
    private var acknowledgedEdit: Int = 0

    internal fun currentPlan(): NBTTagCompound? {
        return pendingPlan ?: tile.plan
    }

    internal fun select(
        id: String,
        plan: NBTTagCompound,
    ): Boolean {
        if (!isValid || tile.recipe.isNotEmpty() || !checkSize(plan)) {
            return false
        }
        syncToServer(SELECT) {
            it.writeStringToBuffer(id)
            it.writeNBTTagCompoundToBuffer(plan)
        }
        return true
    }

    internal fun edit(
        selection: Int,
        plan: NBTTagCompound,
    ) {
        if (!isValid || selection != tile.selection || !checkSize(plan)) {
            return
        }
        pendingPlan = plan
        editSequence++
        syncToServer(EDIT) {
            it.writeInt(selection)
            it.writeInt(editSequence)
            it.writeNBTTagCompoundToBuffer(plan)
        }
    }

    internal fun action(id: Int) {
        syncToServer(id) {
            it.writeInt(tile.selection)
        }
    }

    private fun checkSize(plan: NBTTagCompound): Boolean {
        if (SceneBlueprint.canStore(plan)) {
            return true
        }
        syncManager.player.addChatMessage(ChatComponentTranslation("iblueprints.planner.save_failed"))
        return false
    }

    override fun readOnClient(
        id: Int,
        buf: PacketBuffer,
    ) {
        if (id != STATE) {
            return
        }
        val acknowledged = buf.readInt()
        val state = buf.readNBTTagCompoundFromBuffer() ?: return
        val previousSelection = tile.selection
        tile.readState(state)
        if (tile.selection != previousSelection || acknowledged == editSequence) {
            pendingPlan = null
        }
    }

    override fun readOnServer(
        id: Int,
        buf: PacketBuffer,
    ) {
        val player = syncManager.player
        if (player.openContainer !== syncManager.modularSyncManager.container || !tile.accessible(player)) {
            return
        }
        if (id == SELECT) {
            val recipe = buf.readStringFromBuffer(128)
            val plan = buf.readNBTTagCompoundFromBuffer() ?: return
            tile.select(
                id = recipe,
                snapshot = plan,
            )
            return
        }
        val selection = buf.readInt()
        if (id == EDIT) {
            receivedEdit = buf.readInt()
            val plan = buf.readNBTTagCompoundFromBuffer() ?: return
            tile.edit(
                expectedSelection = selection,
                snapshot = plan,
            )
            return
        }
        if (selection != tile.selection) {
            return
        }
        when (id) {
            RESET -> tile.reset(selection)
            BLUEPRINT -> {
                if (!tile.saveBlueprint()) {
                    player.addChatMessage(ChatComponentTranslation("iblueprints.planner.save_failed"))
                }
            }
            PROJECTOR -> {
                if (!tile.saveProjector()) {
                    player.addChatMessage(ChatComponentTranslation("iblueprints.planner.save_failed"))
                }
            }
            PATTERN -> tile.savePattern()
            LOAD_BLUEPRINT -> {
                if (!tile.loadBlueprint()) {
                    player.addChatMessage(ChatComponentTranslation("iblueprints.planner.load_failed"))
                }
            }
        }
    }

    override fun detectAndSendChanges(init: Boolean) {
        if (init || sentRevision != tile.revision || receivedEdit != acknowledgedEdit) {
            sentRevision = tile.revision
            acknowledgedEdit = receivedEdit
            val state = NBTTagCompound().also(tile::writeState)
            syncToClient(STATE) {
                it.writeInt(acknowledgedEdit)
                it.writeNBTTagCompoundToBuffer(state)
            }
        }
    }

    internal companion object {

        private const val STATE = 0
        private const val SELECT = 1
        private const val EDIT = 2
        internal const val RESET = 3
        internal const val BLUEPRINT = 4
        internal const val PROJECTOR = 5
        internal const val PATTERN = 6
        internal const val LOAD_BLUEPRINT = 7
    }
}
