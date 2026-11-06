package space.impact.iblueprints.integration.ae2

import appeng.api.AEApi
import appeng.api.implementations.ICraftingPatternItem
import appeng.api.networking.crafting.ICraftingPatternDetails
import appeng.util.item.AEItemStack
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.world.World
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.item.StructureBlueprintItem

internal object PlannerPatterns {

    internal const val ORDER_TAG = "MaterialOrder"

    internal fun isOrder(stack: ItemStack): Boolean {
        return stack.tagCompound?.getBoolean(ORDER_TAG) == true
    }

    internal fun isBlank(stack: ItemStack?): Boolean {
        return stack != null && AEApi.instance().definitions().materials().blankPattern().isSameAs(stack)
    }

    internal fun encode(
        plan: NBTTagCompound,
        recipe: String,
        world: World,
    ): ItemStack? {
        val pattern = structureStack(
            item = StructurePatternItem,
            plan = plan,
            recipe = recipe,
        ) ?: return null
        return pattern.takeIf {
            details(
                pattern = it,
                world = world,
            ) != null
        }
    }

    internal fun details(
        pattern: ItemStack,
        world: World,
    ): ICraftingPatternDetails? {
        if (pattern.item !== StructurePatternItem || isOrder(pattern)) {
            return null
        }
        val plan = SceneBlueprint.fromItem(pattern) ?: return null
        val ingredients = SceneBlueprint.materials(plan) ?: return null
        val order = pattern.copy()
        order.tagCompound.setBoolean(ORDER_TAG, true)
        val encoded = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).orNull()
            ?: return null
        val inputs = NBTTagList()
        for (ingredient in ingredients) {
            val stack = AEItemStack.create(ingredient) ?: return null
            inputs.appendTag(stack.toNBTGeneric())
        }
        val output = AEItemStack.create(order) ?: return null
        val outputs = NBTTagList()
        outputs.appendTag(output.toNBTGeneric())
        val tag = NBTTagCompound().apply {
            setTag(
                "in",
                inputs,
            )
            setTag(
                "out",
                outputs,
            )
        }
        if (CompressedStreamTools.compress(tag).size > Short.MAX_VALUE) {
            return null
        }
        encoded.tagCompound = tag
        val item = encoded.item as? ICraftingPatternItem ?: return null
        return item.getPatternForItem(
            encoded,
            world,
        )
    }

    internal fun structureStack(
        item: Item,
        plan: NBTTagCompound,
        recipe: String,
    ): ItemStack? {
        if (recipe.length !in 1..128 || !SceneBlueprint.canStore(plan)) {
            return null
        }
        return ItemStack(item).apply {
            tagCompound = NBTTagCompound().apply {
                setString(
                    StructureBlueprintItem.RECIPE_TAG,
                    recipe,
                )
                setTag(
                    StructureBlueprintItem.PLAN_TAG,
                    plan.copy(),
                )
            }
        }
    }
}
