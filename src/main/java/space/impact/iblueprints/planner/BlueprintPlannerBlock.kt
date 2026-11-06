package space.impact.iblueprints.planner

import com.cleanroommc.modularui.factory.TileEntityGuiFactory
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

internal object BlueprintPlannerBlock : BlockContainer(Material.wood) {

    internal const val REGISTRY_NAME = "blueprint_planner"

    init {
        setBlockName("iblueprints.blueprint_planner")
        setBlockTextureName("minecraft:crafting_table_side")
        setCreativeTab(CreativeTabs.tabMisc)
        setHardness(2.5f)
        setStepSound(soundTypeWood)
    }

    override fun createNewTileEntity(
        world: World,
        metadata: Int,
    ): TileEntity {
        return BlueprintPlannerTile()
    }

    override fun onBlockActivated(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        player: EntityPlayer,
        side: Int,
        hitX: Float,
        hitY: Float,
        hitZ: Float,
    ): Boolean {
        if (!world.isRemote) {
            val tile = world.getTileEntity(
                x,
                y,
                z,
            ) as? BlueprintPlannerTile ?: return false
            TileEntityGuiFactory.INSTANCE.open(
                player,
                tile,
            )
        }
        return true
    }

    override fun breakBlock(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        block: Block,
        metadata: Int,
    ) {
        if (!world.isRemote) {
            val tile = world.getTileEntity(
                x,
                y,
                z,
            ) as? BlueprintPlannerTile
            if (tile != null) {
                for (index in 0 until tile.inventory.slots) {
                    val stack = tile.inventory.getStackInSlot(index) ?: continue
                    dropBlockAsItem(
                        world,
                        x,
                        y,
                        z,
                        stack.copy(),
                    )
                    tile.inventory.setStackInSlot(
                        index,
                        null,
                    )
                }
            }
        }
        super.breakBlock(
            world,
            x,
            y,
            z,
            block,
            metadata,
        )
    }
}
