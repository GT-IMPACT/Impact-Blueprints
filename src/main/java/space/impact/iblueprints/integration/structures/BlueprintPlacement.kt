package space.impact.iblueprints.integration.structures

import net.minecraft.block.Block
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import space.impact.iblueprints.scene.ScenePosition

internal data class BlueprintPlacement(
    val position: ScenePosition,
    val stack: ItemStack?,
    val hatchFacing: ForgeDirection?,
    val doorMetadata: Int? = null,
    val hatchTexture: Int? = null,
    val fluidBlock: Block? = null,
    val fluidMetadata: Int = 0,
) {

    internal val block: Block?
        get() {
            return fluidBlock ?: (stack?.item as? ItemBlock)?.let(Block::getBlockFromItem)
        }

    internal val metadata: Int
        get() {
            if (fluidBlock != null) {
                return fluidMetadata
            }
            return doorMetadata ?: (stack?.item as? ItemBlock)?.getMetadata(stack.itemDamage) ?: 0
        }
}
