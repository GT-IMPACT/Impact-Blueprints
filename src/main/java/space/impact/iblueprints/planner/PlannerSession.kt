package space.impact.iblueprints.planner

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ChatComponentTranslation
import space.impact.iblueprints.integration.structures.SceneBlueprint

internal class PlannerSession(
    private val state: PlannerState,
    private val player: () -> EntityPlayer,
    private val isOpen: () -> Boolean,
    private val sendToServer: (Int, (PacketBuffer) -> Unit) -> Unit,
    private val sendToClient: (Int, (PacketBuffer) -> Unit) -> Unit,
) {

    private var sentRevision: Long = -1
    private var pendingPlan: NBTTagCompound? = null
    private var editSequence: Int = 0
    private var receivedEdit: Int = 0
    private var acknowledgedEdit: Int = 0

    internal fun currentPlan(): NBTTagCompound? {
        return pendingPlan ?: state.plan
    }

    internal fun select(
        id: String,
        plan: NBTTagCompound,
    ): Boolean {
        if (!isOpen() || state.recipe.isNotEmpty() || !checkSize(plan)) {
            return false
        }
        sendToServer(SELECT) {
            it.writeStringToBuffer(id)
            it.writeNBTTagCompoundToBuffer(plan)
        }
        return true
    }

    internal fun edit(
        selection: Int,
        plan: NBTTagCompound,
    ) {
        if (!isOpen() || selection != state.selection || !checkSize(plan)) {
            return
        }
        pendingPlan = plan
        editSequence++
        sendToServer(EDIT) {
            it.writeInt(selection)
            it.writeInt(editSequence)
            it.writeNBTTagCompoundToBuffer(plan)
        }
    }

    internal fun action(id: Int) {
        if (!isOpen()) {
            return
        }
        sendToServer(id) {
            it.writeInt(state.selection)
        }
    }

    private fun checkSize(plan: NBTTagCompound): Boolean {
        if (SceneBlueprint.canStore(plan)) {
            return true
        }
        player().addChatMessage(ChatComponentTranslation("iblueprints.planner.save_failed"))
        return false
    }

    internal fun readOnClient(
        id: Int,
        buf: PacketBuffer,
    ) {
        if (id != STATE) {
            return
        }
        val acknowledged = buf.readInt()
        val snapshot = buf.readNBTTagCompoundFromBuffer() ?: return
        val previousSelection = state.selection
        state.readState(snapshot)
        if (state.selection != previousSelection || acknowledged == editSequence) {
            pendingPlan = null
        }
    }

    internal fun readOnServer(
        id: Int,
        buf: PacketBuffer,
    ) {
        val player = player()
        if (!isOpen()) {
            return
        }
        if (id == SELECT) {
            val recipe = buf.readStringFromBuffer(128)
            val plan = buf.readNBTTagCompoundFromBuffer() ?: return
            state.select(
                id = recipe,
                snapshot = plan,
            )
            return
        }
        val selection = buf.readInt()
        if (id == EDIT) {
            receivedEdit = buf.readInt()
            val plan = buf.readNBTTagCompoundFromBuffer() ?: return
            state.edit(
                expectedSelection = selection,
                snapshot = plan,
            )
            return
        }
        if (selection != state.selection) {
            return
        }
        when (id) {
            RESET -> state.reset(selection)
            BLUEPRINT -> {
                if (!state.saveBlueprint()) {
                    player.addChatMessage(ChatComponentTranslation("iblueprints.planner.save_failed"))
                }
            }
            PROJECTOR -> {
                if (!state.saveProjector()) {
                    player.addChatMessage(ChatComponentTranslation("iblueprints.planner.save_failed"))
                }
            }
            PATTERN -> state.savePattern()
            LOAD_BLUEPRINT -> {
                if (!state.loadBlueprint()) {
                    player.addChatMessage(ChatComponentTranslation("iblueprints.planner.load_failed"))
                }
            }
        }
    }

    internal fun detectAndSendChanges(init: Boolean) {
        if (init || sentRevision != state.revision || receivedEdit != acknowledgedEdit) {
            sentRevision = state.revision
            acknowledgedEdit = receivedEdit
            val state = NBTTagCompound().also(state::writeState)
            sendToClient(STATE) {
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
