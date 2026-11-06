package space.impact.iblueprints.planner

import com.cleanroommc.modularui.utils.item.ItemStackHandler
import cpw.mods.fml.common.Loader
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT
import space.impact.iblueprints.MODID
import space.impact.iblueprints.integration.ae2.PlannerPatterns
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.item.BlankBlueprintItem
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.structures.alignment.constructable.ChannelDataAccessor
import space.impact.structures.item.ItemConstructableTrigger

internal class PlannerState(
    private val networked: Boolean,
    private val world: () -> World?,
    private val onChanged: () -> Unit,
) {

    internal val inventory: ItemStackHandler = object : ItemStackHandler(if (networked) 5 else 3) {
        override fun onContentsChanged(slot: Int) {
            onChanged()
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
        return blueprintToLoad() != null
    }

    private fun blueprintToLoad(): ItemStack? {
        val output = inventory.getStackInSlot(BLUEPRINT)
        if (output?.item === StructureBlueprintItem) {
            return output
        }
        return inventory.getStackInSlot(BLUEPRINT_INPUT)?.takeIf {
            it.item === StructureBlueprintItem
        }
    }

    internal fun loadBlueprint(): Boolean {
        val blueprint = blueprintToLoad() ?: return false
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
        if (selection != expectedSelection || !sameController) {
            return false
        }
        val details = "recipe=$recipe selection=$selection revision=${revision + 1}"
        val valid = tracePlannerStep(
            step = "server.validateEdit",
            details = details,
        ) {
            SceneBlueprint.canStore(snapshot)
        }
        if (!valid) {
            return false
        }
        tracePlannerStep(
            step = "server.applyAndMarkDirty",
            details = details,
        ) {
            setPlan(snapshot)
        }
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
        onChanged()
    }

    internal fun canSaveBlueprint(): Boolean {
        if (plan == null) {
            return false
        }
        val output = inventory.getStackInSlot(BLUEPRINT)
        if (output != null) {
            return output.item === StructureBlueprintItem && output.stackSize == 1
        }
        val input = inventory.getStackInSlot(BLUEPRINT_INPUT) ?: return false
        val writable = input.stackSize > 0 && acceptsBlueprintInput(input)
        return writable
    }

    internal fun saveBlueprint(): Boolean {
        val snapshot = plan ?: return false
        if (!canSaveBlueprint()) {
            return false
        }
        val output = inventory.getStackInSlot(BLUEPRINT)
        val input = output ?: inventory.getStackInSlot(BLUEPRINT_INPUT) ?: return false
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
        if (output == null) {
            inventory.extractItem(
                BLUEPRINT_INPUT,
                1,
                false,
            )
        }
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
        return networked && plan != null && acceptsBlankPattern(inventory.getStackInSlot(BLANK_PATTERN)) &&
            inventory.getStackInSlot(PATTERN) == null
    }

    internal fun savePattern() {
        val snapshot = plan ?: return
        if (!canSavePattern()) {
            return
        }
        val pattern = PlannerPatterns.encode(
            plan = snapshot,
            recipe = recipe,
            world = world() ?: return,
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

    internal fun writeToNBT(tag: NBTTagCompound) {
        writeState(tag)
        tag.setTag(
            "Inventory",
            inventory.serializeNBT(),
        )
    }

    internal fun readFromNBT(tag: NBTTagCompound) {
        readState(tag)
        inventory.deserializeNBT(tag.getCompoundTag("Inventory"))
    }

    internal companion object {

        internal const val PROJECTOR = 0
        internal const val BLUEPRINT_INPUT = 1
        internal const val BLUEPRINT = 2
        internal const val BLANK_PATTERN = 3
        internal const val PATTERN = 4

        internal fun acceptsProjector(stack: ItemStack?): Boolean {
            return Loader.isModLoaded("impactapi") && stack?.item is ItemConstructableTrigger
        }

        internal fun acceptsBlueprintInput(stack: ItemStack): Boolean {
            return stack.item === BlankBlueprintItem || stack.item === StructureBlueprintItem
        }

        internal fun acceptsRecordedBlueprint(stack: ItemStack): Boolean {
            return stack.item === StructureBlueprintItem
        }

        internal fun acceptsBlankPattern(stack: ItemStack?): Boolean {
            return Loader.isModLoaded("appliedenergistics2") && PlannerPatterns.isBlank(stack)
        }
    }
}
