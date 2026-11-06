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
        tracePlannerStep(
            step = "client.select",
            details = "recipe=$id",
        ) {
            sendToServer(SELECT) {
                it.writeStringToBuffer(id)
                it.writeNBTTagCompoundToBuffer(plan)
            }
        }
        return true
    }

    internal fun edit(
        selection: Int,
        plan: NBTTagCompound,
    ) {
        val details = "recipe=${state.recipe} selection=$selection edit=${editSequence + 1}"
        val rejection = tracePlannerStep(
            step = "client.prepareEdit",
            details = details,
        ) {
            when {
                !isOpen() -> {
                    "inactive container window=${player().openContainer.windowId}"
                }
                selection != state.selection -> {
                    "selection changed current=${state.selection}"
                }
                !checkSize(plan) -> {
                    "invalid or oversized plan"
                }
                else -> {
                    null
                }
            }
        }
        if (rejection != null) {
            logPlannerTrace("client.prepareEdit rejected reason=$rejection $details")
            return
        }
        pendingPlan = plan
        editSequence++
        tracePlannerStep(
            step = "client.sendEdit",
            details = "$details window=${player().openContainer.windowId}",
        ) {
            sendToServer(EDIT) {
                it.writeInt(selection)
                it.writeInt(editSequence)
                it.writeNBTTagCompoundToBuffer(plan)
            }
        }
    }

    internal fun action(id: Int) {
        if (!isOpen()) {
            return
        }
        tracePlannerStep(
            step = "client.action",
            details = "action=$id recipe=${state.recipe} selection=${state.selection}",
        ) {
            sendToServer(id) {
                it.writeInt(state.selection)
            }
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
        if (pendingPlan != null) {
            logPlannerTrace(
                "client.editAck recipe=${state.recipe} selection=${state.selection} " +
                    "acknowledged=$acknowledged pending=$editSequence",
            )
        }
        if (state.selection != previousSelection || acknowledged == editSequence) {
            pendingPlan = null
        }
    }

    internal fun readOnServer(
        id: Int,
        buf: PacketBuffer,
    ) {
        val player = player()
        if (id == EDIT) {
            logPlannerTrace(
                "server.receiveEdit recipe=${state.recipe} window=${player.openContainer.windowId} " +
                    "bytes=${buf.readableBytes()}",
            )
        }
        if (!isOpen()) {
            if (id == EDIT) {
                logPlannerTrace("server.receiveEdit rejected closed container")
            }
            return
        }
        if (id == SELECT) {
            val recipe = buf.readStringFromBuffer(128)
            tracePlannerStep(
                step = "server.select",
                details = "recipe=$recipe",
            ) {
                val plan = buf.readNBTTagCompoundFromBuffer()
                val selected = plan != null && state.select(
                    id = recipe,
                    snapshot = plan,
                )
                logPlannerTrace("server.select result selected=$selected recipe=$recipe")
            }
            return
        }
        val selection = buf.readInt()
        if (id == EDIT) {
            receivedEdit = buf.readInt()
            val details = "recipe=${state.recipe} selection=$selection edit=$receivedEdit"
            val plan = tracePlannerStep(
                step = "server.decodeEdit",
                details = details,
            ) {
                buf.readNBTTagCompoundFromBuffer()
            }
            if (plan == null) {
                logPlannerTrace("server.decodeEdit rejected missing plan $details")
                return
            }
            val applied = state.edit(
                expectedSelection = selection,
                snapshot = plan,
            )
            logPlannerTrace("server.editResult applied=$applied revision=${state.revision} $details")
            return
        }
        if (selection != state.selection) {
            return
        }
        tracePlannerStep<Unit>(
            step = "server.action",
            details = "action=$id recipe=${state.recipe} selection=$selection",
        ) {
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
    }

    internal fun detectAndSendChanges(init: Boolean) {
        if (init || sentRevision != state.revision || receivedEdit != acknowledgedEdit) {
            sentRevision = state.revision
            if (receivedEdit != acknowledgedEdit) {
                logPlannerTrace(
                    "server.sendEditAck recipe=${state.recipe} selection=${state.selection} " +
                        "edit=$receivedEdit revision=$sentRevision",
                )
            }
            acknowledgedEdit = receivedEdit
            tracePlannerStep(
                step = "server.syncState",
                details = "recipe=${state.recipe} selection=${state.selection} revision=$sentRevision",
            ) {
                val snapshot = NBTTagCompound().also(state::writeState)
                sendToClient(STATE) {
                    it.writeInt(acknowledgedEdit)
                    it.writeNBTTagCompoundToBuffer(snapshot)
                }
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
