package space.impact.iblueprints.integration.ae2

import appeng.util.Platform
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.BlockEvent

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
        if (player.isSneaking && Platform.isWrench(player, player.currentEquippedItem, x, y, z)) {
            if (!world.isRemote) {
                dismantle(world = world, x = x, y = y, z = z, player = player)
            }
            return true
        }

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

    private fun dismantle(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) {
        val tile = world.getTileEntity(x, y, z) as? BlueprintProviderTile ?: return
        if (!tile.accessible(player) || !world.canMineBlock(player, x, y, z)) {
            return
        }

        val event = BlockEvent.BreakEvent(x, y, z, world, this, world.getBlockMetadata(x, y, z), player)
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return
        }

        val drops = Platform.getBlockDrops(world, x, y, z)
        if (removedByPlayer(world, player, x, y, z, false)) {
            Platform.spawnDrops(world, x, y, z, drops.asList())
        }
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
