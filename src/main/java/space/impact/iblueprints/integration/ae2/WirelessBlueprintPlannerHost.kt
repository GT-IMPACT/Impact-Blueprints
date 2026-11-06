package space.impact.iblueprints.integration.ae2

import appeng.api.config.SecurityPermissions
import appeng.api.features.IWirelessTermHandler
import appeng.api.networking.security.ISecurityGrid
import appeng.container.ContainerOpenContext
import appeng.container.PrimaryGui
import appeng.helpers.WirelessTerminalGuiObject
import appeng.util.Platform
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import space.impact.iblueprints.planner.PlannerState

internal class WirelessBlueprintPlannerHost(
    private val handler: IWirelessTermHandler,
    stack: ItemStack,
    private val player: EntityPlayer,
    slot: Int,
) : WirelessTerminalGuiObject(
    handler,
    stack,
    player,
    player.worldObj,
    slot,
    0,
    0,
), IMeBlueprintPlannerHost {

    private var loading = true
    override val planner = PlannerState(
        networked = true,
        world = {
            player.worldObj
        },
        onChanged = {
            markPlannerDirty()
        },
    )

    init {
        val data = stack.tagCompound
        if (data?.hasKey(PLANNER_TAG) == true) {
            planner.readFromNBT(data.getCompoundTag(PLANNER_TAG))
        }
        loading = false
    }

    override fun markPlannerDirty() {
        if (loading || player.worldObj.isRemote) {
            return
        }
        val data = NBTTagCompound()
        planner.writeToNBT(data)
        val tag = itemStack.tagCompound ?: NBTTagCompound().also {
            itemStack.tagCompound = it
        }
        tag.setTag(
            PLANNER_TAG,
            data,
        )
        player.inventory.markDirty()
    }

    override fun accessible(player: EntityPlayer): Boolean {
        if (player !== this.player) {
            return false
        }
        val current = Platform.getItemFromPlayerInventoryBySlotIndex(
            player,
            inventorySlot,
        ) ?: return false
        if (player.worldObj.isRemote) {
            // Inventory packets replace the client stack while the GUI keeps its original host.
            return current.isItemEqual(itemStack)
        }
        if (current !== itemStack) {
            return false
        }
        if (!rangeCheck() || !handler.hasInfinityPower(itemStack) && !handler.hasPower(
                player,
                1.0,
                itemStack,
            )
        ) {
            return false
        }
        val node = actionableNode ?: return false
        val security = node.grid.getCache<ISecurityGrid>(ISecurityGrid::class.java)
        return security.hasPermission(
            player,
            SecurityPermissions.CRAFT,
        )
    }

    override fun plannerContext(): ContainerOpenContext {
        return ContainerOpenContext(this).apply {
            setWorld(player.worldObj)
            side = ForgeDirection.UNKNOWN
        }
    }

    override fun plannerPrimaryGui(): PrimaryGui {
        return object : PrimaryGui(
            null,
            itemStack.copy(),
            null,
            ForgeDirection.UNKNOWN,
        ) {
            override fun open(player: EntityPlayer) {
                if (accessible(player)) {
                    WirelessPlannerIntegration.open(
                        player = player,
                        slot = inventorySlot,
                    )
                }
            }
        }
    }

    internal companion object {

        internal const val PLANNER_TAG = "iblueprints:planner"
    }
}
