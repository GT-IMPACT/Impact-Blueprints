package space.impact.iblueprints.integration.nei

import net.minecraft.item.ItemStack
import space.impact.iblueprints.scene.ScenePosition

internal class SceneMaterialUsage(
    internal val stack: ItemStack,
    internal val values: Map<String, Int>,
    internal val positions: Set<ScenePosition>,
)
