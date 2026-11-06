package space.impact.iblueprints.scene.world

import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound

internal class SceneBlockState(
    val block: Block,
    val metadata: Int,
    val tileData: NBTTagCompound?,
)
