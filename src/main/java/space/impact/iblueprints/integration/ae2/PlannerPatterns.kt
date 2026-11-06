package space.impact.iblueprints.integration.ae2

import appeng.api.AEApi
import appeng.api.implementations.ICraftingPatternItem
import appeng.util.item.AEItemStack
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.world.World
import space.impact.iblueprints.integration.structures.SceneBlueprint

internal object PlannerPatterns {

    internal fun isBlank(stack: ItemStack?): Boolean {
        return stack != null && AEApi.instance().definitions().materials().blankPattern().isSameAs(stack)
    }

    internal fun encode(
        plan: NBTTagCompound,
        world: World,
    ): ItemStack? {
        val ingredients = SceneBlueprint.materials(plan) ?: return null
        val pattern = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).orNull()
            ?: return null
        val inputs = NBTTagList()
        for (ingredient in ingredients) {
            val stack = AEItemStack.create(ingredient) ?: return null
            inputs.appendTag(stack.toNBTGeneric())
        }
        val marker = ItemStack(Items.paper)
        marker.setStackDisplayName(ingredients.first().displayName)
        val output = AEItemStack.create(marker) ?: return null
        val outputs = NBTTagList()
        outputs.appendTag(output.toNBTGeneric())
        pattern.tagCompound = NBTTagCompound().apply {
            setTag(
                "in",
                inputs,
            )
            setTag(
                "out",
                outputs,
            )
        }
        val item = pattern.item as? ICraftingPatternItem ?: return null
        return pattern.takeIf {
            item.getPatternForItem(
                it,
                world,
            ) != null
        }
    }
}
