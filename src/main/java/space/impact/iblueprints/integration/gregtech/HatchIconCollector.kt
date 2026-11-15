package space.impact.iblueprints.integration.gregtech

import net.minecraft.block.Block
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess

/** Collects GregTech's face layers without writing vertices to the active tessellator. */
internal class HatchIconCollector(world: IBlockAccess) : RenderBlocks(world) {

    internal val icons: MutableList<IIcon> = mutableListOf()

    override fun renderFaceYNeg(
        block: Block,
        x: Double,
        y: Double,
        z: Double,
        icon: IIcon?,
    ) {
        icon?.let(icons::add)
    }

    override fun renderFaceYPos(
        block: Block,
        x: Double,
        y: Double,
        z: Double,
        icon: IIcon?,
    ) {
        icon?.let(icons::add)
    }

    override fun renderFaceZNeg(
        block: Block,
        x: Double,
        y: Double,
        z: Double,
        icon: IIcon?,
    ) {
        icon?.let(icons::add)
    }

    override fun renderFaceZPos(
        block: Block,
        x: Double,
        y: Double,
        z: Double,
        icon: IIcon?,
    ) {
        icon?.let(icons::add)
    }

    override fun renderFaceXNeg(
        block: Block,
        x: Double,
        y: Double,
        z: Double,
        icon: IIcon?,
    ) {
        icon?.let(icons::add)
    }

    override fun renderFaceXPos(
        block: Block,
        x: Double,
        y: Double,
        z: Double,
        icon: IIcon?,
    ) {
        icon?.let(icons::add)
    }
}
