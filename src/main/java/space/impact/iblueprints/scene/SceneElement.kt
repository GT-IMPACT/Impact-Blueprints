package space.impact.iblueprints.scene

import net.minecraft.item.ItemStack

/** Structural positions exist independently of the blocks currently occupying them. */
public class SceneElement(
    public val requiredAir: Boolean,
    public val unresolved: Boolean,
    choices: List<ItemStack>,
    public val channels: List<String>,
) {

    private val choices: List<ItemStack> = choices.map {
        it.copy()
    }
    internal var hatchChoices: (() -> List<ItemStack>)? = null
    internal var placementChoices: (() -> List<ItemStack>)? = null
    internal var hatchChoicesIncomplete: Boolean = false
    private val resolvedChoices: List<ItemStack> by lazy(LazyThreadSafetyMode.NONE) {
        val resolved = mutableListOf<ItemStack>()
        for (stack in choices + placementChoices?.invoke().orEmpty()) {
            if (resolved.none {
                    it.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(
                        it,
                        stack,
                    )
                }
            ) {
                resolved.add(stack)
            }
        }
        resolved
    }
    private val resolvedHatches: List<ItemStack> by lazy(LazyThreadSafetyMode.NONE) {
        hatchChoices?.invoke().orEmpty()
    }

    internal fun inspectionAlternatives(): List<ItemStack> {
        val blocks = alternatives()
        return blocks + resolvedHatches.filterNot { hatch ->
            blocks.any(hatch::isItemEqual)
        }.map {
            it.copy()
        }
    }

    internal fun hasAlternative(stack: ItemStack): Boolean {
        val matches: (ItemStack) -> Boolean = {
            it.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(
                it,
                stack,
            )
        }
        return resolvedChoices.any(matches) || resolvedHatches.any(matches)
    }

    internal fun matchesOriginal(stack: ItemStack): Boolean {
        return choices.firstOrNull()?.isItemEqual(stack) == true
    }

    public fun alternatives(): List<ItemStack> {
        return resolvedChoices.map {
            it.copy()
        }
    }
}
