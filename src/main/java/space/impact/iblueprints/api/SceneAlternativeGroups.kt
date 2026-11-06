package space.impact.iblueprints.api

import cpw.mods.fml.common.Loader
import java.util.function.Predicate
import net.minecraft.item.ItemStack
import space.impact.iblueprints.integration.gregtech.GregTechHatchGroups

/** Registers labels for alternatives already accepted by a scene. Register during client initialization. */
public object SceneAlternativeGroups {

    internal const val OTHER = "iblueprints.variants.group.other"

    private val rules = mutableListOf<Pair<String, Predicate<ItemStack>>>()

    /** The first matching rule takes priority over the defaults. Reuse a translation key to share a group. */
    @JvmStatic
    public fun register(
        translationKey: String,
        matches: Predicate<ItemStack>,
    ) {
        require(translationKey.isNotBlank())
        rules.add(translationKey to matches)
    }

    internal fun group(stack: ItemStack): String {
        val registered = rules.firstOrNull { (_, matches) ->
            matches.test(stack)
        }?.first
        if (registered != null) {
            return registered
        }
        if (Loader.isModLoaded("gregtech")) {
            return GregTechHatchGroups.group(stack) ?: OTHER
        }
        return OTHER
    }
}
