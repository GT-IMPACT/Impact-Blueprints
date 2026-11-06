package space.impact.iblueprints.integration.modularui

import com.cleanroommc.modularui.api.widget.Interactable.Result
import com.cleanroommc.modularui.screen.RichTooltip
import com.cleanroommc.modularui.utils.item.ItemStackHandler
import com.cleanroommc.modularui.widgets.slot.ItemSlot
import com.cleanroommc.modularui.widgets.slot.ModularSlot
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector

internal class SceneMaterialSlot(
    stack: ItemStack,
    private val replace: (() -> Boolean)? = null,
    private val onTap: ((Int) -> Boolean)? = null,
) : ItemSlot() {

    init {
        val inventory = ItemStackHandler(1)
        inventory.setStackInSlot(
            0,
            stack.copy(),
        )
        slot(ModularSlot(
            inventory,
            0,
        ).canPut(false).canTake(false).canDragInto(false))
    }

    override fun buildTooltip(
        stack: ItemStack?,
        tooltip: RichTooltip,
    ) {
        super.buildTooltip(
            stack,
            tooltip,
        )
        if (replace != null) {
            tooltip.addLine(StatCollector.translateToLocal("iblueprints.variants.replace"))
        }
    }

    override fun onMousePressed(mouseButton: Int): Result {
        if (mouseButton == 0 && replace?.invoke() == true) {
            return Result.SUCCESS
        }
        return Result.ACCEPT
    }

    override fun onMouseRelease(mouseButton: Int): Boolean {
        return true
    }

    override fun onMouseTapped(mouseButton: Int): Result {
        return if (onTap?.invoke(mouseButton) == true) {
            Result.SUCCESS
        } else {
            Result.IGNORE
        }
    }

    override fun onMouseDrag(
        mouseButton: Int,
        timeSinceClick: Long,
    ) {
    }
}
