package space.impact.iblueprints.integration.ae2

import com.cleanroommc.modularui.utils.item.ItemStackHandler
import net.minecraft.inventory.InventoryBasic
import net.minecraft.item.ItemStack
import space.impact.iblueprints.planner.PlannerState

internal class MePlannerInventory(
    title: String,
    private val stacks: ItemStackHandler,
    private val onChanged: () -> Unit,
    private val accepts: (Int, ItemStack) -> Boolean = stacks::isItemValid,
) : InventoryBasic(
    title,
    false,
    stacks.slots,
) {

    constructor(part: IMeBlueprintPlannerHost) : this(
        title = "iblueprints.planner.me_title",
        stacks = part.planner.inventory,
        onChanged = {
            part.markPlannerDirty()
        },
        accepts = { slot, stack ->
            when (slot) {
                PlannerState.PROJECTOR -> PlannerState.acceptsProjector(stack)
                PlannerState.BLUEPRINT_INPUT -> PlannerState.acceptsBlueprintInput(stack)
                PlannerState.BLANK_PATTERN -> PlannerState.acceptsBlankPattern(stack)
                else -> false
            }
        },
    )

    override fun getStackInSlot(slot: Int): ItemStack? {
        return stacks.getStackInSlot(slot)
    }

    override fun decrStackSize(
        slot: Int,
        amount: Int,
    ): ItemStack? {
        return stacks.extractItem(
            slot,
            amount,
            false,
        )
    }

    override fun getStackInSlotOnClosing(slot: Int): ItemStack? {
        val stack = stacks.getStackInSlot(slot)
        stacks.setStackInSlot(
            slot,
            null,
        )
        return stack
    }

    override fun setInventorySlotContents(
        slot: Int,
        stack: ItemStack?,
    ) {
        stacks.setStackInSlot(
            slot,
            stack,
        )
    }

    override fun markDirty() {
        onChanged()
    }

    override fun isItemValidForSlot(
        slot: Int,
        stack: ItemStack,
    ): Boolean {
        return accepts(
            slot,
            stack,
        )
    }
}
