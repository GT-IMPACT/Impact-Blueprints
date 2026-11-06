package space.impact.iblueprints.planner

import com.cleanroommc.modularui.api.IGuiHolder
import com.cleanroommc.modularui.factory.PosGuiData
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.ModularScreen
import com.cleanroommc.modularui.screen.UISettings
import com.cleanroommc.modularui.utils.item.ItemStackHandler
import com.cleanroommc.modularui.value.sync.PanelSyncManager
import cpw.mods.fml.common.Loader
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.Constants.NBT
import space.impact.iblueprints.MODID
import space.impact.iblueprints.integration.ae2.PlannerPatterns
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.structures.alignment.constructable.ChannelDataAccessor
import space.impact.structures.item.ItemConstructableTrigger

internal class BlueprintPlannerTile : TileEntity(), IGuiHolder<PosGuiData> {

    internal val inventory: ItemStackHandler = object : ItemStackHandler(5) {
        override fun onContentsChanged(slot: Int) {
            markDirty()
        }
    }
    internal var recipe: String = ""
        private set
    internal var plan: NBTTagCompound? = null
        private set
    internal var selection: Int = 0
        private set
    internal var revision: Long = 0
        private set

    override fun buildUI(
        data: PosGuiData,
        manager: PanelSyncManager,
        settings: UISettings,
    ): ModularPanel {
        return BlueprintPlannerGui.build(
            tile = this,
            manager = manager,
            settings = settings,
        )
    }

    @SideOnly(Side.CLIENT)
    override fun createScreen(
        data: PosGuiData,
        mainPanel: ModularPanel,
    ): ModularScreen {
        return ModularScreen(
            MODID,
            mainPanel,
        )
    }

    internal fun accessible(player: EntityPlayer): Boolean {
        if (worldObj !== player.worldObj || isInvalid) {
            return false
        }
        return worldObj.getTileEntity(
            xCoord,
            yCoord,
            zCoord,
        ) === this && player.getDistanceSq(
            xCoord + 0.5,
            yCoord + 0.5,
            zCoord + 0.5,
        ) <= 64.0
    }

    internal fun select(
        id: String,
        snapshot: NBTTagCompound,
    ): Boolean {
        if (recipe.isNotEmpty() || id.length !in 1..128 || !SceneBlueprint.canStore(snapshot)) {
            return false
        }
        install(
            id = id,
            snapshot = snapshot,
        )
        return true
    }

    private fun install(
        id: String,
        snapshot: NBTTagCompound,
    ) {
        recipe = id
        selection++
        setPlan(snapshot)
    }

    internal fun hasBlueprintInput(): Boolean {
        return inventory.getStackInSlot(PAPER)?.item === StructureBlueprintItem
    }

    internal fun loadBlueprint(): Boolean {
        val blueprint = inventory.getStackInSlot(PAPER) ?: return false
        if (blueprint.item !== StructureBlueprintItem) {
            return false
        }
        val snapshot = SceneBlueprint.fromItem(blueprint) ?: return false
        if (!SceneBlueprint.canStore(snapshot)) {
            return false
        }
        val controller = SceneBlueprint.controller(snapshot) ?: return false
        val id = blueprint.tagCompound.getString(StructureBlueprintItem.RECIPE_TAG).ifEmpty {
            "$MODID:metatile_${controller.itemDamage}"
        }
        if (id.length > 128) {
            return false
        }
        install(
            id = id,
            snapshot = snapshot.copy() as NBTTagCompound,
        )
        return true
    }

    internal fun edit(
        expectedSelection: Int,
        snapshot: NBTTagCompound,
    ): Boolean {
        val previous = plan ?: return false
        val sameController = previous.getCompoundTag("controller") == snapshot.getCompoundTag("controller")
        if (selection != expectedSelection || !sameController || !SceneBlueprint.canStore(snapshot)) {
            return false
        }
        setPlan(snapshot)
        return true
    }

    internal fun reset(expectedSelection: Int) {
        if (selection == expectedSelection && recipe.isNotEmpty()) {
            recipe = ""
            selection++
            setPlan(null)
        }
    }

    private fun setPlan(snapshot: NBTTagCompound?) {
        plan = snapshot
        revision++
        markDirty()
    }

    internal fun canSaveBlueprint(): Boolean {
        val input = inventory.getStackInSlot(PAPER) ?: return false
        val writable = input.stackSize > 0 && (input.item === Items.paper || input.item === StructureBlueprintItem)
        return plan != null && writable && inventory.getStackInSlot(BLUEPRINT) == null
    }

    internal fun saveBlueprint(): Boolean {
        val snapshot = plan ?: return false
        val input = inventory.getStackInSlot(PAPER) ?: return false
        if (!canSaveBlueprint()) {
            return false
        }
        val blueprint = if (input.item === StructureBlueprintItem) {
            input.copy()
        } else {
            ItemStack(StructureBlueprintItem)
        }
        blueprint.stackSize = 1
        val tag = (blueprint.tagCompound ?: NBTTagCompound()).apply {
            setString(
                StructureBlueprintItem.RECIPE_TAG,
                recipe,
            )
            setTag(
                StructureBlueprintItem.PLAN_TAG,
                snapshot.copy(),
            )
        }
        if (CompressedStreamTools.compress(tag).size > Short.MAX_VALUE) {
            return false
        }
        blueprint.tagCompound = tag
        inventory.extractItem(
            PAPER,
            1,
            false,
        )
        inventory.setStackInSlot(
            BLUEPRINT,
            blueprint,
        )
        return true
    }

    internal fun canSaveProjector(): Boolean {
        return plan != null && acceptsProjector(inventory.getStackInSlot(PROJECTOR))
    }

    internal fun saveProjector(): Boolean {
        val snapshot = plan ?: return false
        if (!canSaveProjector()) {
            return false
        }
        val projector = inventory.getStackInSlot(PROJECTOR)?.copy() ?: return false
        for ((name, value) in SceneBlueprint.channels(snapshot)) {
            ChannelDataAccessor.setChannelData(
                projector,
                name,
                value,
            )
        }
        val tag = projector.tagCompound ?: NBTTagCompound()
        tag.setTag(
            StructureBlueprintItem.PLAN_TAG,
            snapshot.copy(),
        )
        if (CompressedStreamTools.compress(tag).size > SceneBlueprint.MAX_BYTES) {
            return false
        }
        projector.tagCompound = tag
        inventory.setStackInSlot(
            PROJECTOR,
            projector,
        )
        return true
    }

    internal fun canSavePattern(): Boolean {
        return plan != null && acceptsBlankPattern(inventory.getStackInSlot(BLANK_PATTERN)) &&
            inventory.getStackInSlot(PATTERN) == null
    }

    internal fun savePattern() {
        val snapshot = plan ?: return
        if (!canSavePattern()) {
            return
        }
        val pattern = PlannerPatterns.encode(
            plan = snapshot,
            world = worldObj,
        ) ?: return
        inventory.extractItem(
            BLANK_PATTERN,
            1,
            false,
        )
        inventory.setStackInSlot(
            PATTERN,
            pattern,
        )
    }

    internal fun writeState(tag: NBTTagCompound) {
        tag.setString(
            "Recipe",
            recipe,
        )
        tag.setInteger(
            "Selection",
            selection,
        )
        plan?.let {
            tag.setTag(
                "Plan",
                it,
            )
        }
    }

    internal fun readState(tag: NBTTagCompound) {
        selection = tag.getInteger("Selection")
        plan = if (tag.hasKey(
                "Plan",
                NBT.TAG_COMPOUND,
            )
        ) {
            tag.getCompoundTag("Plan")
        } else {
            null
        }
        recipe = if (plan == null) {
            ""
        } else {
            tag.getString("Recipe")
        }
    }

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        writeState(tag)
        tag.setTag(
            "Inventory",
            inventory.serializeNBT(),
        )
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        readState(tag)
        inventory.deserializeNBT(tag.getCompoundTag("Inventory"))
    }

    internal companion object {

        internal const val PROJECTOR = 0
        internal const val PAPER = 1
        internal const val BLUEPRINT = 2
        internal const val BLANK_PATTERN = 3
        internal const val PATTERN = 4

        internal fun acceptsProjector(stack: ItemStack?): Boolean {
            return Loader.isModLoaded("impactapi") && stack?.item is ItemConstructableTrigger
        }

        internal fun acceptsBlankPattern(stack: ItemStack?): Boolean {
            return Loader.isModLoaded("appliedenergistics2") && PlannerPatterns.isBlank(stack)
        }
    }
}
