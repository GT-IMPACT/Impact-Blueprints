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

    public fun alternatives(): List<ItemStack> {
        return choices.map {
            it.copy()
        }
    }
}
