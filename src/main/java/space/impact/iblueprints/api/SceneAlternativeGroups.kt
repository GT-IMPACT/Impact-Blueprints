package space.impact.iblueprints.api

import cpw.mods.fml.common.Loader
import java.util.function.Predicate
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
import space.impact.iblueprints.integration.gregtech.GregTechHatchGroups

/** Registers labels for alternatives already accepted by a scene. Register during client initialization. */
public object SceneAlternativeGroups {

    internal const val OTHER = "iblueprints.variants.group.other"

    private val rules = mutableListOf<Pair<String, Predicate<ItemStack>>>()
    private val priorities = mutableMapOf<String, Int>()

    /** Registers a group at Int.MAX_VALUE. The first matching rule overrides the default classification. */
    @JvmStatic
    public fun register(
        translationKey: String,
        matches: Predicate<ItemStack>,
    ) {
        register(
            translationKey = translationKey,
            priority = Int.MAX_VALUE,
            matches = matches,
        )
    }

    /** Lower priorities appear first. Rules sharing a translation key use their lowest registered priority. */
    @JvmStatic
    public fun register(
        translationKey: String,
        priority: Int,
        matches: Predicate<ItemStack>,
    ) {
        require(translationKey.isNotBlank())
        rules.add(translationKey to matches)
        priorities[translationKey] = minOf(
            a = priorities[translationKey] ?: Int.MAX_VALUE,
            b = priority,
        )
    }

    internal fun priority(translationKey: String): Int {
        return priorities[translationKey] ?: if (translationKey == OTHER) {
            Int.MAX_VALUE
        } else {
            1
        }
    }

    internal fun compareGroups(
        first: String,
        second: String,
    ): Int {
        val priorityComparison = priority(first).compareTo(priority(second))
        if (priorityComparison != 0) {
            return priorityComparison
        }
        return StatCollector.translateToLocal(first).compareTo(StatCollector.translateToLocal(second))
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
