package space.impact.iblueprints.integration.ae2

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

internal class BlueprintProviderPatternInventory(
    private val provider: BlueprintProviderTile,
) : IInventory {

    override fun getSizeInventory(): Int {
        return provider.capacity()
    }

    override fun getStackInSlot(slot: Int): ItemStack? {
        if (slot !in 0 until sizeInventory) {
            return null
        }
        return provider.inventory.getStackInSlot(slot)
    }

    override fun decrStackSize(
        slot: Int,
        amount: Int,
    ): ItemStack? {
        if (slot !in 0 until sizeInventory || amount <= 0) {
            return null
        }
        return provider.inventory.extractItem(
            slot,
            amount,
            false,
        )
    }

    override fun getStackInSlotOnClosing(slot: Int): ItemStack? {
        return decrStackSize(
            slot = slot,
            amount = inventoryStackLimit,
        )
    }

    override fun setInventorySlotContents(
        slot: Int,
        stack: ItemStack?,
    ) {
        if (slot !in 0 until sizeInventory || stack != null && !isItemValidForSlot(
                slot = slot,
                stack = stack,
            )
        ) {
            return
        }
        provider.inventory.setStackInSlot(
            slot,
            stack,
        )
    }

    override fun getInventoryName(): String {
        return provider.rawName
    }

    override fun hasCustomInventoryName(): Boolean {
        return false
    }

    override fun getInventoryStackLimit(): Int {
        return 1
    }

    override fun markDirty() {
        provider.markDirty()
    }

    override fun isUseableByPlayer(player: EntityPlayer): Boolean {
        return provider.accessible(player)
    }

    override fun openInventory() {
    }

    override fun closeInventory() {
    }

    override fun isItemValidForSlot(
        slot: Int,
        stack: ItemStack?,
    ): Boolean {
        return slot in 0 until sizeInventory && stack != null && stack.stackSize == 1 &&
            provider.inventory.isItemValid(
                slot,
                stack,
            )
    }
}
