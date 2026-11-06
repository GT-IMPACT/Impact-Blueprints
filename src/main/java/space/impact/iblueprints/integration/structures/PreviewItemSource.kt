package space.impact.iblueprints.integration.structures

import net.minecraft.item.ItemStack
import space.impact.structures.structure.IItemSource
import java.util.function.Predicate

/** An unlimited scene-local supply. Never reads or consumes the player's inventory. */
internal class PreviewItemSource(private val candidates: List<ItemStack>) : IItemSource {

    override fun take(
        predicate: Predicate<ItemStack>,
        simulate: Boolean,
        count: Int,
    ): Map<ItemStack, Int> {
        require(count >= 0)
        if (count == 0) {
            return emptyMap()
        }
        val item = candidates.firstOrNull {
            predicate.test(it)
        } ?: return emptyMap()
        return mapOf(item.copy() to count)
    }

    override fun takeOne(
        stack: ItemStack,
        simulate: Boolean,
    ): Boolean {
        return stack.item != null && stack.stackSize == 1
    }

    override fun takeAll(
        stack: ItemStack,
        simulate: Boolean,
    ): Boolean {
        return stack.item != null && stack.stackSize >= 0
    }

}
