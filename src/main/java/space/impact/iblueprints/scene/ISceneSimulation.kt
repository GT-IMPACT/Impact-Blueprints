package space.impact.iblueprints.scene

import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection

internal interface ISceneSimulation {

    val origin: ScenePosition

    fun canReplace(
        position: ScenePosition,
        stack: ItemStack,
    ): Boolean

    fun replace(
        position: ScenePosition,
        stack: ItemStack,
        preferredFacing: ForgeDirection? = null,
    ): Boolean {
        return replaceAll(
            targets = mapOf(position to preferredFacing),
            stack = stack,
        )
    }

    fun replaceAll(
        targets: Map<ScenePosition, ForgeDirection?>,
        stack: ItemStack,
    ): Boolean

    fun hatchFacing(position: ScenePosition): ForgeDirection?

    fun hatchTexture(position: ScenePosition): Int?

    fun setHatchTexture(
        position: ScenePosition,
        texture: Int,
    )

    fun nextHatchFacing(position: ScenePosition): ForgeDirection?

    fun setHatchFacing(
        position: ScenePosition,
        facing: ForgeDirection,
    ): Boolean

    fun validate(): Boolean?
}
