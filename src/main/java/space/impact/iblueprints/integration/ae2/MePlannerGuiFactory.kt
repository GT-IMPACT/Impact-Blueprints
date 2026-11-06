package space.impact.iblueprints.integration.ae2

import appeng.api.parts.IPartHost
import cpw.mods.fml.common.network.IGuiHandler
import cpw.mods.fml.common.Loader
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import space.impact.iblueprints.ImpactBlueprintsMod

internal object MePlannerGuiFactory : IGuiHandler {

    private const val PROVIDER_GUI = 7

    internal fun open(
        part: MeBlueprintPlannerPart,
        player: EntityPlayer,
    ) {
        val tile = part.tile ?: return
        player.openGui(
            ImpactBlueprintsMod,
            part.side.ordinal,
            tile.worldObj,
            tile.xCoord,
            tile.yCoord,
            tile.zCoord,
        )
    }

    internal fun openProvider(
        provider: BlueprintProviderTile,
        player: EntityPlayer,
    ) {
        player.openGui(
            ImpactBlueprintsMod,
            PROVIDER_GUI,
            provider.worldObj,
            provider.xCoord,
            provider.yCoord,
            provider.zCoord,
        )
    }

    override fun getServerGuiElement(
        id: Int,
        player: EntityPlayer,
        world: World,
        x: Int,
        y: Int,
        z: Int,
    ): Any? {
        if (id == WirelessPlannerIntegration.GUI_ID && Loader.isModLoaded("ae2fc")) {
            val host = WirelessPlannerIntegration.createHost(
                player = player,
                slot = x,
            ) ?: return null
            WirelessPlannerMode.select(host.itemStack)
            player.inventory.markDirty()
            player.inventoryContainer.detectAndSendChanges()
            return MeBlueprintPlannerContainer(
                inventory = player.inventory,
                part = host,
            )
        }
        if (id == PROVIDER_GUI) {
            val provider = findProvider(
                player = player,
                world = world,
                x = x,
                y = y,
                z = z,
            ) ?: return null
            return BlueprintProviderContainer(
                playerInventory = player.inventory,
                provider = provider,
            )
        }
        val part = find(
            id = id,
            world = world,
            x = x,
            y = y,
            z = z,
        ) ?: return null
        if (!part.accessible(player)) {
            return null
        }
        return MeBlueprintPlannerContainer(
            inventory = player.inventory,
            part = part,
        )
    }

    override fun getClientGuiElement(
        id: Int,
        player: EntityPlayer,
        world: World,
        x: Int,
        y: Int,
        z: Int,
    ): Any? {
        if (id == WirelessPlannerIntegration.GUI_ID && Loader.isModLoaded("ae2fc")) {
            val host = WirelessPlannerIntegration.createHost(
                player = player,
                slot = x,
            ) ?: return null
            return MeBlueprintPlannerGui(
                inventory = player.inventory,
                part = host,
            )
        }
        if (id == PROVIDER_GUI) {
            val provider = findProvider(
                player = player,
                world = world,
                x = x,
                y = y,
                z = z,
            ) ?: return null
            return BlueprintProviderGui(BlueprintProviderContainer(
                playerInventory = player.inventory,
                provider = provider,
            ))
        }
        val part = find(
            id = id,
            world = world,
            x = x,
            y = y,
            z = z,
        ) ?: return null
        return MeBlueprintPlannerGui(
            inventory = player.inventory,
            part = part,
        )
    }

    private fun findProvider(
        player: EntityPlayer,
        world: World,
        x: Int,
        y: Int,
        z: Int,
    ): BlueprintProviderTile? {
        val provider = world.getTileEntity(
            x,
            y,
            z,
        ) as? BlueprintProviderTile ?: return null
        return provider.takeIf {
            it.accessible(player)
        }
    }

    private fun find(
        id: Int,
        world: World,
        x: Int,
        y: Int,
        z: Int,
    ): MeBlueprintPlannerPart? {
        val side = ForgeDirection.getOrientation(id)
        if (side == ForgeDirection.UNKNOWN || !world.blockExists(
                x,
                y,
                z,
            )
        ) {
            return null
        }
        val host = world.getTileEntity(
            x,
            y,
            z,
        ) as? IPartHost ?: return null
        return host.getPart(side) as? MeBlueprintPlannerPart
    }
}
