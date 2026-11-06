package space.impact.iblueprints.integration.ae2

import appeng.container.AEBaseContainer
import appeng.container.slot.AppEngSlot
import appeng.container.slot.SlotNormal
import appeng.container.slot.SlotRestrictedInput
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack

internal class BlueprintProviderContainer(
    playerInventory: InventoryPlayer,
    private val provider: BlueprintProviderTile,
) : AEBaseContainer(
    playerInventory,
    provider,
) {

    private val providerInventory = MePlannerInventory(
        title = "iblueprints.provider.title",
        stacks = provider.inventory,
        onChanged = provider::markDirty,
    )
    internal val upgradeSlots: List<AppEngSlot> = List(BlueprintProviderTile.UPGRADE_SLOTS) { index ->
        val slotIndex = BlueprintProviderTile.PATTERN_SLOTS + index
        val slot = object : SlotRestrictedInput(
            PlacableItemType.UPGRADES,
            providerInventory,
            slotIndex,
            187,
            8 + index * 18,
            playerInventory,
        ) {
            override fun isItemValid(stack: ItemStack): Boolean {
                return providerInventory.isItemValidForSlot(
                    slotIndex,
                    stack,
                )
            }

            override fun canTakeStack(player: EntityPlayer): Boolean {
                return provider.canRemoveUpgrade() && super.canTakeStack(player)
            }
        }
        slot.setStackLimit(1)
        slot.setNotDraggable()
        addSlotToContainer(slot)
        slot
    }

    init {
        bindPlayerInventory(
            playerInventory,
            0,
            HEIGHT - 82,
        )
        for (index in 0 until BlueprintProviderTile.PATTERN_SLOTS) {
            addSlotToContainer(object : SlotNormal(
                providerInventory,
                index,
                8 + index % 9 * 18,
                PATTERN_BOTTOM - index / 9 * 18,
            ) {
                override fun isEnabled(): Boolean {
                    return index < provider.capacity()
                }

                override fun isItemValid(stack: ItemStack): Boolean {
                    return providerInventory.isItemValidForSlot(
                        index,
                        stack,
                    )
                }

                override fun getSlotStackLimit(): Int {
                    return 1
                }
            })
        }
    }

    override fun canInteractWith(player: EntityPlayer): Boolean {
        return super.canInteractWith(player) && provider.accessible(player)
    }

    override fun isValidSrcSlotForTransfer(slot: AppEngSlot?): Boolean {
        return slot != null && slot.canTakeStack(inventoryPlayer.player) && super.isValidSrcSlotForTransfer(slot)
    }

    internal fun activeRows(): Int {
        return provider.capacity() / BlueprintProviderTile.BASE_SLOTS
    }

    internal companion object {

        internal const val WIDTH = 177
        internal const val PATTERN_TOP = 14
        internal const val PATTERN_END = PATTERN_TOP + BlueprintProviderTile.PATTERN_ROWS * 18
        internal const val HEIGHT = PATTERN_END + 86
        internal const val UPGRADE_WIDTH = 35
        internal const val UPGRADE_HEIGHT = 14 + BlueprintProviderTile.UPGRADE_SLOTS * 18
        private const val PATTERN_BOTTOM = PATTERN_END - 17
    }
}
