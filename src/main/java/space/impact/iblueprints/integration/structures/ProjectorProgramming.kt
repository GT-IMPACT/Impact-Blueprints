package space.impact.iblueprints.integration.structures

import net.minecraft.item.ItemStack
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.structures.alignment.constructable.ChannelDataAccessor

internal object ProjectorProgramming {

    @JvmStatic
    public fun hasLayout(stack: ItemStack): Boolean {
        return stack.tagCompound?.hasKey(StructureBlueprintItem.PLAN_TAG) == true
    }

    @JvmStatic
    public fun clearedCopy(stack: ItemStack): ItemStack {
        val cleared = stack.copy()
        cleared.tagCompound?.removeTag(StructureBlueprintItem.PLAN_TAG)
        ChannelDataAccessor.wipeChannelData(cleared)
        return cleared
    }
}
