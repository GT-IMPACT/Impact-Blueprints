package space.impact.iblueprints.planner

import com.cleanroommc.modularui.api.IGuiHolder
import com.cleanroommc.modularui.factory.PosGuiData
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.ModularScreen
import com.cleanroommc.modularui.screen.UISettings
import com.cleanroommc.modularui.value.sync.PanelSyncManager
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import space.impact.iblueprints.MODID

internal class BlueprintPlannerTile : TileEntity(), IGuiHolder<PosGuiData> {

    internal val planner = PlannerState(
        networked = false,
        world = ::getWorldObj,
        onChanged = ::markDirty,
    )

    override fun buildUI(
        data: PosGuiData,
        manager: PanelSyncManager,
        settings: UISettings,
    ): ModularPanel {
        return BlueprintPlannerGui.build(
            state = planner,
            manager = manager,
            settings = settings,
            accessible = ::accessible,
        )
    }

    @SideOnly(Side.CLIENT)
    override fun createScreen(
        data: PosGuiData,
        mainPanel: ModularPanel,
    ): ModularScreen {
        return ModularScreen(
            MODID,
            mainPanel,
        )
    }

    internal fun accessible(player: EntityPlayer): Boolean {
        if (worldObj !== player.worldObj || isInvalid) {
            return false
        }
        return worldObj.getTileEntity(
            xCoord,
            yCoord,
            zCoord,
        ) === this && player.getDistanceSq(
            xCoord + 0.5,
            yCoord + 0.5,
            zCoord + 0.5,
        ) <= 64.0
    }

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        planner.writeToNBT(tag)
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        planner.readFromNBT(tag)
    }
}