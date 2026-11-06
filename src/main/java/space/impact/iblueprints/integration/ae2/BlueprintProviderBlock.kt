package space.impact.iblueprints.integration.ae2

import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

internal object BlueprintProviderBlock : BlockContainer(Material.iron) {

    internal const val REGISTRY_NAME = "blueprint_provider"

    init {
        setBlockName("iblueprints.blueprint_provider")
        setBlockTextureName("iblueprints:blueprint_provider")
        setCreativeTab(CreativeTabs.tabMisc)
        setHardness(2.5f)
        setStepSound(soundTypeMetal)
    }

    override fun createNewTileEntity(
        world: World,
        metadata: Int,
    ): TileEntity {
        return BlueprintProviderTile()
    }

    override fun onBlockPlacedBy(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        placer: EntityLivingBase,
        stack: ItemStack,
    ) {
        if (!world.isRemote && placer is EntityPlayer) {
            val tile = world.getTileEntity(
                x,
                y,
                z,
            ) as? BlueprintProviderTile ?: return
            tile.proxy.setOwner(placer)
        }
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
            ) as? BlueprintProviderTile ?: return false
            if (tile.accessible(player)) {
                MePlannerGuiFactory.openProvider(
                    provider = tile,
                    player = player,
                )
            }
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
            ) as? BlueprintProviderTile
            tile?.dropContents()
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
