package space.impact.iblueprints.planner

import com.cleanroommc.modularui.factory.TileEntityGuiFactory
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.IIcon
import net.minecraft.world.World

internal object BlueprintPlannerBlock : BlockContainer(Material.wood) {

    internal const val REGISTRY_NAME = "blueprint_planner"
    private var blueprintTop: IIcon? = null

    init {
        setBlockName("iblueprints.blueprint_planner")
        setBlockTextureName("minecraft:crafting_table_side")
        setCreativeTab(CreativeTabs.tabMisc)
        setHardness(2.5f)
        setStepSound(soundTypeWood)
    }

    @SideOnly(Side.CLIENT)
    override fun registerBlockIcons(register: IIconRegister) {
        super.registerBlockIcons(register)
        blueprintTop = register.registerIcon("iblueprints:blueprint_planner_top")
    }

    @SideOnly(Side.CLIENT)
    override fun getIcon(
        side: Int,
        metadata: Int,
    ): IIcon? {
        if (side == 1) {
            blueprintTop?.let {
                return it
            }
        }
        return super.getIcon(
            side,
            metadata,
        )
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
                for (index in 0 until tile.planner.inventory.slots) {
                    val stack = tile.planner.inventory.getStackInSlot(index) ?: continue
                    dropBlockAsItem(
                        world,
                        x,
                        y,
                        z,
                        stack.copy(),
                    )
                    tile.planner.inventory.setStackInSlot(
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
