package space.impact.iblueprints.integration.structures

import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import space.impact.iblueprints.scene.ScenePosition

internal data class BlueprintPlacement(
    val position: ScenePosition,
    val stack: ItemStack,
    val hatchFacing: ForgeDirection?,
    val doorMetadata: Int? = null,
)
