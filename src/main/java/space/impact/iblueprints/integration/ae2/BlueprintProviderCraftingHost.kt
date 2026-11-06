package space.impact.iblueprints.integration.ae2

import appeng.api.config.Upgrades
import appeng.api.networking.IGridNode
import appeng.api.networking.crafting.ICraftingLink
import appeng.api.networking.crafting.ICraftingPatternDetails
import appeng.api.networking.crafting.ICraftingProviderHelper
import appeng.api.util.AECableType
import appeng.api.util.DimensionalCoord
import appeng.api.util.IConfigManager
import appeng.helpers.DualityInterface
import appeng.helpers.IInterfaceHost
import com.google.common.collect.ImmutableSet
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import java.util.EnumSet

// The pinned AE2 CPU recognizes fake crafting through DualityInterface.
internal class BlueprintProviderCraftingHost(
    private val provider: BlueprintProviderTile,
) : IInterfaceHost {

    internal val medium: DualityInterface = object : DualityInterface(
        provider.proxy,
        this,
    ) {
        override fun isFakeCraftingMode(): Boolean {
            return true
        }

        override fun isBusy(): Boolean {
            return provider.isBusy()
        }

        override fun getCrafterIcon(): ItemStack {
            return provider.crafterIcon
        }

        override fun pushPattern(
            pattern: ICraftingPatternDetails,
            table: InventoryCrafting,
        ): Boolean {
            return provider.pushPattern(
                pattern = pattern,
                table = table,
            )
        }
    }

    override fun getInterfaceDuality(): DualityInterface {
        return medium
    }

    override fun getTargets(): EnumSet<ForgeDirection> {
        return EnumSet.noneOf(ForgeDirection::class.java)
    }

    override fun getTileEntity(): TileEntity {
        return provider
    }

    override fun getTile(): TileEntity {
        return provider
    }

    override fun getLocation(): DimensionalCoord {
        return provider.location
    }

    override fun getActionableNode(): IGridNode? {
        return provider.actionableNode
    }

    override fun getGridNode(side: ForgeDirection): IGridNode? {
        return provider.getGridNode(side)
    }

    override fun getCableConnectionType(side: ForgeDirection): AECableType {
        return provider.getCableConnectionType(side)
    }

    override fun securityBreak() {
        provider.securityBreak()
    }

    override fun saveChanges() {
        provider.markDirty()
    }

    override fun getInstalledUpgrades(upgrade: Upgrades): Int {
        return 0
    }

    override fun getInventoryByName(name: String): IInventory? {
        return null
    }

    override fun getConfigManager(): IConfigManager {
        return medium.configManager
    }

    override fun getRequestedJobs(): ImmutableSet<ICraftingLink> {
        return ImmutableSet.of()
    }

    override fun jobStateChange(link: ICraftingLink) {
    }

    override fun provideCrafting(tracker: ICraftingProviderHelper) {
        provider.provideCrafting(tracker)
    }

    override fun isBusy(): Boolean {
        return provider.isBusy()
    }

    override fun pushPattern(
        pattern: ICraftingPatternDetails,
        table: InventoryCrafting,
    ): Boolean {
        return provider.pushPattern(
            pattern = pattern,
            table = table,
        )
    }
}
