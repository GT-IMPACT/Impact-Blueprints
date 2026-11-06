package space.impact.iblueprints.integration.ae2

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

public object WirelessPlannerMode {

    private const val SELECTED_TAG = "iblueprints:plannerSelected"

    @JvmStatic
    public fun isSelected(stack: ItemStack): Boolean {
        return stack.tagCompound?.getBoolean(SELECTED_TAG) == true
    }

    internal fun select(stack: ItemStack) {
        val tag = stack.tagCompound ?: NBTTagCompound().also {
            stack.tagCompound = it
        }
        tag.setBoolean(
            SELECTED_TAG,
            true,
        )
    }

    @JvmStatic
    public fun clear(stack: ItemStack) {
        stack.tagCompound?.removeTag(SELECTED_TAG)
    }

    @JvmStatic
    public fun restore(
        stack: ItemStack,
        player: EntityPlayer,
        slot: Int,
    ): Boolean {
        if (!isSelected(stack)) {
            return false
        }
        if (!player.worldObj.isRemote) {
            WirelessPlannerIntegration.open(
                player = player,
                slot = if (slot == Int.MIN_VALUE) {
                    player.inventory.currentItem
                } else {
                    slot
                },
            )
        }
        return true
    }
}
