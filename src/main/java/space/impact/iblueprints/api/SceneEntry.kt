package space.impact.iblueprints.api

import java.util.function.Function
import java.util.function.Supplier
import net.minecraft.item.ItemStack
import space.impact.iblueprints.scene.BlockScene

public class SceneEntry @JvmOverloads constructor(
    public val id: String,
    item: ItemStack,
    public val factory: Supplier<BlockScene>,
    public val channels: List<SceneChannel> = emptyList(),
    private val configuredFactory: Function<Map<String, Int>, BlockScene>? = null,
) {

    private val controller: ItemStack = item.copy()

    public fun build(values: Map<String, Int> = emptyMap()): BlockScene {
        val normalized = channels.associate {
            it.id to it.normalize(values[it.id] ?: it.minimum)
        }
        return configuredFactory?.apply(normalized) ?: factory.get()
    }

    public fun item(): ItemStack {
        return controller.copy()
    }
}
