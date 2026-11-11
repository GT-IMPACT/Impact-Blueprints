package space.impact.iblueprints.integration.ae2

import appeng.api.AEApi
import appeng.api.config.Actionable
import appeng.api.config.SecurityPermissions
import appeng.api.implementations.IPowerChannelState
import appeng.api.networking.GridFlags
import appeng.api.networking.crafting.ICraftingProvider
import appeng.api.networking.crafting.ICraftingPatternDetails
import appeng.api.networking.crafting.ICraftingProviderHelper
import appeng.api.networking.events.MENetworkBootingStatusChange
import appeng.api.networking.events.MENetworkChannelsChanged
import appeng.api.networking.events.MENetworkCraftingPatternChange
import appeng.api.networking.events.MENetworkEventSubscribe
import appeng.api.networking.events.MENetworkPowerStatusChange
import appeng.api.networking.security.MachineSource
import appeng.api.storage.data.IAEItemStack
import appeng.api.storage.data.IAEStack
import appeng.api.util.IInterfaceViewable
import appeng.me.GridAccessException
import appeng.me.helpers.AENetworkProxy
import appeng.tile.TileEvent
import appeng.tile.events.TileEventType
import appeng.tile.grid.AENetworkTile
import appeng.util.Platform
import appeng.util.inv.MEInventoryCrafting
import appeng.util.item.AEItemStack
import com.cleanroommc.modularui.utils.item.ItemStackHandler
import io.netty.buffer.ByteBuf
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.StatCollector
import net.minecraftforge.common.util.Constants.NBT
import space.impact.iblueprints.integration.structures.SceneBlueprint

internal class BlueprintProviderTile : AENetworkTile(), ICraftingProvider, IInterfaceViewable, IPowerChannelState {

    private var active = false
    private var clientFlags = 0
    private var refreshPatterns = true
    private var offers: Set<ICraftingPatternDetails>? = null
    private val pendingMaterials = mutableListOf<ItemStack>()
    private val source = MachineSource(this)
    private val terminalPatterns = BlueprintProviderPatternInventory(this)
    private val fakeCraftingMedium by lazy {
        BlueprintProviderCraftingHost(this).medium
    }

    internal val inventory: ItemStackHandler = object : ItemStackHandler(PATTERN_SLOTS + UPGRADE_SLOTS) {
        override fun getSlotLimit(slot: Int): Int {
            return 1
        }

        override fun isItemValid(
            slot: Int,
            stack: ItemStack?,
        ): Boolean {
            if (stack == null) {
                return false
            }
            return if (slot < PATTERN_SLOTS) {
                slot < capacity() && stack.item === StructurePatternItem &&
                    !PlannerPatterns.isOrder(stack) &&
                    SceneBlueprint.fromItem(stack) != null
            } else {
                AEApi.instance().definitions().materials().cardPatternCapacity().isSameAs(stack)
            }
        }

        override fun onContentsChanged(slot: Int) {
            offers = null
            refreshPatterns = true
            markDirty()
        }
    }

    init {
        proxy.setFlags(GridFlags.REQUIRE_CHANNEL)
        proxy.setIdlePowerUsage(1.0)
    }

    override fun createProxy(): AENetworkProxy {
        return AENetworkProxy(
            this,
            "proxy",
            ItemStack(BlueprintProviderBlock),
            true,
        )
    }

    override fun gridChanged() {
        refreshPatterns = true
    }

    @MENetworkEventSubscribe
    public fun stateChange(event: MENetworkChannelsChanged) {
        markForUpdate()
    }

    @MENetworkEventSubscribe
    public fun stateChange(event: MENetworkPowerStatusChange) {
        markForUpdate()
    }

    @MENetworkEventSubscribe
    public fun stateChange(event: MENetworkBootingStatusChange) {
        markForUpdate()
    }

    @TileEvent(TileEventType.NETWORK_WRITE)
    public fun writeProviderState(data: ByteBuf) {
        clientFlags = 0
        try {
            if (proxy.energy.isNetworkPowered) {
                clientFlags = clientFlags or POWERED_FLAG
            }
            if (proxy.node.meetsChannelRequirements()) {
                clientFlags = clientFlags or CHANNEL_FLAG
            }
            if (proxy.path.isNetworkBooting) {
                clientFlags = clientFlags or BOOTING_FLAG
            }
        } catch (_: GridAccessException) {
            // Keep AE2's offline state when the grid is unavailable.
        }
        data.writeByte(clientFlags)
    }

    @TileEvent(TileEventType.NETWORK_READ)
    public fun readProviderState(data: ByteBuf): Boolean {
        val previousFlags = clientFlags
        clientFlags = data.readByte().toInt()
        return clientFlags != previousFlags
    }

    override fun isPowered(): Boolean {
        return clientFlags and POWERED_FLAG == POWERED_FLAG
    }

    override fun isActive(): Boolean {
        return clientFlags and CHANNEL_FLAG == CHANNEL_FLAG
    }

    override fun isBooting(): Boolean {
        return clientFlags and BOOTING_FLAG == BOOTING_FLAG
    }

    @TileEvent(TileEventType.TICK)
    public fun updateProvider() {
        if (worldObj.isRemote) {
            return
        }
        val online = proxy.isActive
        try {
            if (refreshPatterns || active != online) {
                val grid = proxy.grid
                grid.postEvent(MENetworkCraftingPatternChange(
                    this,
                    proxy.node,
                ))
                active = online
                refreshPatterns = false
            }
            if (online && pendingMaterials.isNotEmpty()) {
                returnMaterials()
            }
        } catch (_: GridAccessException) {
            // An unloaded or disconnected grid will be retried after reconnecting.
        }
    }

    private fun returnMaterials() {
        val monitor = proxy.storage.itemInventory
        val pending = pendingMaterials.listIterator()
        while (pending.hasNext()) {
            val material = pending.next()
            val requested = AEItemStack.create(material) ?: continue
            val remainder = monitor.injectItems(
                requested,
                Actionable.MODULATE,
                source,
            )
            if (remainder == null) {
                pending.remove()
                markDirty()
            } else if (remainder.stackSize != material.stackSize.toLong()) {
                material.stackSize = remainder.stackSize.toInt()
                markDirty()
            }
        }
    }

    override fun provideCrafting(tracker: ICraftingProviderHelper) {
        if (!proxy.isActive) {
            return
        }
        for (pattern in patterns()) {
            tracker.addCraftingOption(
                fakeCraftingMedium,
                pattern,
            )
        }
    }

    override fun isBusy(): Boolean {
        return !proxy.isActive || pendingMaterials.isNotEmpty()
    }

    override fun getCrafterIcon(): ItemStack {
        return ItemStack(BlueprintProviderBlock)
    }

    override fun rows(): Int {
        return capacity() / BASE_SLOTS
    }

    override fun rowSize(): Int {
        return BASE_SLOTS
    }

    override fun getPatterns(): IInventory {
        return terminalPatterns
    }

    override fun getName(): String {
        return StatCollector.translateToLocal(rawName)
    }

    override fun getRawName(): String {
        return "tile.iblueprints.blueprint_provider.name"
    }

    override fun getTileEntity(): TileEntity {
        return this
    }

    override fun shouldDisplay(): Boolean {
        return true
    }

    override fun allowsPatternOptimization(): Boolean {
        return false
    }

    override fun getSelfRep(): ItemStack {
        return crafterIcon
    }

    override fun getDisplayRep(): ItemStack {
        return crafterIcon
    }

    override fun pushPattern(
        pattern: ICraftingPatternDetails,
        table: InventoryCrafting,
    ): Boolean {
        if (isBusy || pattern !in patterns() || !matchesInputs(
                pattern = pattern,
                table = table,
            )
        ) {
            return false
        }
        // AE2 completes the virtual output itself. Retain the accepted materials as individual stacks.
        pendingMaterials.addAll(pattern.condensedAEInputs.filterIsInstance<IAEItemStack>().map(IAEItemStack::getItemStack))
        markDirty()
        return true
    }

    private fun matchesInputs(
        pattern: ICraftingPatternDetails,
        table: InventoryCrafting,
    ): Boolean {
        val counts = mutableMapOf<IAEStack<*>, Long>()
        for (index in 0 until table.sizeInventory) {
            val stack = if (table is MEInventoryCrafting) {
                table.getAEStackInSlot(index)
            } else {
                AEItemStack.create(table.getStackInSlot(index))
            } ?: continue
            counts[stack] = (counts[stack] ?: 0) + stack.stackSize
        }
        val expected = pattern.condensedAEInputs
        return expected.size == counts.size && expected.all {
            counts[it] == it.stackSize
        }
    }

    private fun patterns(): Set<ICraftingPatternDetails> {
        val cached = offers
        if (cached != null) {
            return cached
        }
        val installed = linkedSetOf<ICraftingPatternDetails>()
        for (slot in 0 until capacity()) {
            val pattern = inventory.getStackInSlot(slot) ?: continue
            val details = PlannerPatterns.details(
                pattern = pattern,
                world = worldObj,
            ) ?: continue
            installed.add(details)
        }
        offers = installed
        return installed
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public fun readProvider(tag: NBTTagCompound) {
        val stored = tag.getCompoundTag("Patterns").copy() as NBTTagCompound
        if (stored.getInteger("Size") == OLD_PATTERN_SLOTS + OLD_UPGRADE_SLOTS) {
            val items = stored.getTagList(
                "Items",
                NBT.TAG_COMPOUND,
            )
            for (index in 0 until items.tagCount()) {
                val item = items.getCompoundTagAt(index)
                val slot = item.getInteger("Slot")
                if (slot in OLD_PATTERN_SLOTS until OLD_PATTERN_SLOTS + OLD_UPGRADE_SLOTS) {
                    item.setInteger(
                        "Slot",
                        PATTERN_SLOTS + slot - OLD_PATTERN_SLOTS,
                    )
                }
            }
        }
        stored.setInteger(
            "Size",
            PATTERN_SLOTS + UPGRADE_SLOTS,
        )
        inventory.deserializeNBT(stored)
        pendingMaterials.clear()
        val materials = tag.getTagList(
            "PendingMaterials",
            NBT.TAG_COMPOUND,
        )
        for (index in 0 until materials.tagCount()) {
            val saved = materials.getCompoundTagAt(index)
            val stack = ItemStack.loadItemStackFromNBT(saved) ?: continue
            val amount = saved.getInteger("Amount")
            if (amount > 0) {
                stack.stackSize = amount
                pendingMaterials.add(stack)
            }
        }
        offers = null
        refreshPatterns = true
    }

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public fun writeProvider(tag: NBTTagCompound) {
        tag.setTag(
            "Patterns",
            inventory.serializeNBT(),
        )
        val materials = NBTTagList()
        for (stack in pendingMaterials) {
            materials.appendTag(stack.writeToNBT(NBTTagCompound()).apply {
                setByte("Count", 1)
                setInteger("Amount", stack.stackSize)
            })
        }
        tag.setTag(
            "PendingMaterials",
            materials,
        )
    }

    internal fun dropContents() {
        val drops = mutableListOf<ItemStack>()
        for (slot in 0 until inventory.slots) {
            val stack = inventory.getStackInSlot(slot) ?: continue
            drops.add(stack)
            inventory.setStackInSlot(
                slot,
                null,
            )
        }
        for (stack in pendingMaterials) {
            while (stack.stackSize > 0) {
                val size = minOf(
                    stack.stackSize,
                    stack.maxStackSize.coerceAtLeast(1),
                )
                drops.add(stack.splitStack(size))
            }
        }
        pendingMaterials.clear()
        Platform.spawnDrops(
            worldObj,
            xCoord,
            yCoord,
            zCoord,
            drops,
        )
    }

    internal fun accessible(player: EntityPlayer): Boolean {
        if (isInvalid || worldObj !== player.worldObj || player.getDistanceSq(
                xCoord + 0.5,
                yCoord + 0.5,
                zCoord + 0.5,
            ) > 64.0
        ) {
            return false
        }
        return worldObj.isRemote || try {
            proxy.security.hasPermission(
                player,
                SecurityPermissions.BUILD,
            )
        } catch (_: GridAccessException) {
            true
        }
    }

    internal fun capacity(): Int {
        var slots = BASE_SLOTS
        for (index in PATTERN_SLOTS until inventory.slots) {
            if (inventory.getStackInSlot(index) != null) {
                slots += BASE_SLOTS
            }
        }
        return slots
    }

    internal fun canRemoveUpgrade(): Boolean {
        for (index in capacity() - BASE_SLOTS until PATTERN_SLOTS) {
            if (inventory.getStackInSlot(index) != null) {
                return false
            }
        }
        return true
    }

    internal companion object {

        const val BASE_SLOTS = 9
        const val UPGRADE_SLOTS = 3
        const val PATTERN_ROWS = 1 + UPGRADE_SLOTS
        const val PATTERN_SLOTS = BASE_SLOTS * PATTERN_ROWS
        private const val POWERED_FLAG = 1
        private const val CHANNEL_FLAG = 2
        private const val BOOTING_FLAG = 4
        private const val OLD_UPGRADE_SLOTS = 2
        private const val OLD_PATTERN_SLOTS = BASE_SLOTS * (1 + OLD_UPGRADE_SLOTS)
    }
}
