package space.impact.gui3dengine.api

import net.minecraft.item.ItemStack

/** Register on the client during post-init; NEI queries only inspect immutable entry snapshots. */
public object SceneRegistry {

    private val entries: MutableMap<String, SceneEntry> = linkedMapOf()

    @JvmStatic
    @Synchronized
    public fun register(entry: SceneEntry) {
        require(!entries.containsKey(entry.id)) {
            "Scene already registered: ${entry.id}"
        }
        entries[entry.id] = entry
    }

    @JvmStatic
    @Synchronized
    public fun all(): List<SceneEntry> {
        return entries.values.toList()
    }

    @JvmStatic
    public fun matching(stack: ItemStack): List<SceneEntry> {
        return all().filter {
            val controller = it.item()
            controller.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(
                controller,
                stack,
            )
        }
    }
}
