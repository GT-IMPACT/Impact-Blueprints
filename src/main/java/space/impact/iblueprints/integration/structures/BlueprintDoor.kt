package space.impact.iblueprints.integration.structures

import cpw.mods.fml.common.registry.GameData
import net.minecraft.block.Block
import net.minecraft.block.BlockDoor
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection

internal object BlueprintDoor {

    internal const val UPPER_HALF = 8
    internal const val BLOCK_NAME = "IC2:blockDoorAlloy"
    internal const val ITEM_NAME = "IC2:itemDoorAlloy"
    private val directions = listOf(
        ForgeDirection.WEST,
        ForgeDirection.NORTH,
        ForgeDirection.EAST,
        ForgeDirection.SOUTH,
    )

    internal fun isReinforced(block: Block): Boolean {
        return block is BlockDoor && GameData.getBlockRegistry().getNameForObject(block) == BLOCK_NAME
    }

    internal fun isReinforcedBlockItem(item: Item): Boolean {
        return GameData.getItemRegistry().getNameForObject(item) == BLOCK_NAME
    }

    internal fun material(): ItemStack? {
        return GameData.getItemRegistry().getObject(ITEM_NAME)?.let {
            ItemStack(it)
        }
    }

    internal fun rotate(
        metadata: Int,
        from: ForgeDirection,
        to: ForgeDirection,
    ): Int {
        if (metadata and UPPER_HALF != 0 || from.offsetY != 0 || to.offsetY != 0 ||
            from == ForgeDirection.UNKNOWN || to == ForgeDirection.UNKNOWN
        ) {
            return metadata
        }
        val rotation = directions.indexOf(to) - directions.indexOf(from)
        return (metadata and 4) or (((metadata and 3) + rotation) and 3)
    }
}
