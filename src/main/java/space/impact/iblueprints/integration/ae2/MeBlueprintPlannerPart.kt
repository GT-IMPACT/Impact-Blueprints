package space.impact.iblueprints.integration.ae2

import appeng.api.config.SecurityPermissions
import appeng.api.parts.IPartRenderHelper
import appeng.client.texture.CableBusTextures
import appeng.me.GridAccessException
import appeng.parts.reporting.AbstractPartTerminal
import appeng.util.Platform
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.util.Vec3
import space.impact.iblueprints.planner.PlannerState
import space.impact.iblueprints.planner.logPlannerTrace
import space.impact.iblueprints.planner.tracePlannerStep

internal class MeBlueprintPlannerPart(stack: ItemStack) : AbstractPartTerminal(stack) {

    internal val planner = PlannerState(
        networked = true,
        world = {
            tile?.worldObj
        },
        onChanged = {
            host?.markForSave()
        },
    )
    private var lastTracedSaveRevision = 0L

    override fun onPartActivate(
        player: EntityPlayer,
        pos: Vec3,
    ): Boolean {
        if (player.isSneaking) {
            return false
        }
        if (player is EntityPlayerMP) {
            if (accessible(player)) {
                tracePlannerStep(
                    step = "me.openTerminal",
                    details = "recipe=${planner.recipe} selection=${planner.selection}",
                ) {
                    MePlannerGuiFactory.open(
                        part = this,
                        player = player,
                    )
                }
            } else if (!proxy.isActive) {
                player.addChatMessage(ChatComponentTranslation("iblueprints.planner.me_offline"))
            }
        }
        return true
    }

    internal fun accessible(player: EntityPlayer): Boolean {
        val hostTile = tile ?: return false
        if (hostTile.isInvalid || hostTile.worldObj !== player.worldObj || host?.getPart(side) !== this) {
            return false
        }
        val distance = player.getDistanceSq(
            hostTile.xCoord + 0.5,
            hostTile.yCoord + 0.5,
            hostTile.zCoord + 0.5,
        )
        if (distance > 64.0) {
            return false
        }
        if (player.worldObj.isRemote) {
            return true
        }
        return proxy.isActive && try {
            proxy.security.hasPermission(
                player,
                SecurityPermissions.CRAFT,
            )
        } catch (_: GridAccessException) {
            false
        }
    }

    override fun getPrimaryGuiIcon(): ItemStack {
        return itemStack.copy()
    }

    override fun writeToNBT(data: NBTTagCompound) {
        val revision = planner.revision
        val trace = revision != lastTracedSaveRevision
        val details = "recipe=${planner.recipe} selection=${planner.selection} revision=$revision"
        if (trace) {
            logPlannerTrace("me.writeNBT begin $details")
        }
        super.writeToNBT(data)
        planner.writeToNBT(data)
        if (trace) {
            lastTracedSaveRevision = revision
            logPlannerTrace("me.writeNBT end $details")
        }
    }

    override fun readFromNBT(data: NBTTagCompound) {
        tracePlannerStep(
            step = "me.readNBT",
            details = "hasPlan=${data.hasKey("Plan")}",
        ) {
            super.readFromNBT(data)
            planner.readFromNBT(data)
        }
    }

    override fun getDrops(
        drops: MutableList<ItemStack>,
        wrenched: Boolean,
    ) {
        super.getDrops(
            drops,
            wrenched,
        )
        for (slot in 0 until planner.inventory.slots) {
            val stack = planner.inventory.getStackInSlot(slot) ?: continue
            drops.add(stack.copy())
        }
    }

    override fun securityBreak() {
        val hostTile = tile ?: return
        if (itemStack.stackSize <= 0) {
            return
        }
        val contents = mutableListOf<ItemStack>()
        getDrops(
            drops = contents,
            wrenched = false,
        )
        super.securityBreak()
        Platform.spawnDrops(
            hostTile.worldObj,
            hostTile.xCoord,
            hostTile.yCoord,
            hostTile.zCoord,
            contents,
        )
    }

    @SideOnly(Side.CLIENT)
    override fun renderStatic(
        x: Int,
        y: Int,
        z: Int,
        helper: IPartRenderHelper,
        renderer: RenderBlocks,
    ) {
        super.renderStatic(
            x,
            y,
            z,
            MeBlueprintTerminalRenderHelper(helper),
            renderer,
        )
    }

    @SideOnly(Side.CLIENT)
    override fun renderInventory(
        helper: IPartRenderHelper,
        renderer: RenderBlocks,
    ) {
        super.renderInventory(
            MeBlueprintTerminalRenderHelper(helper),
            renderer,
        )
    }

    override fun getFrontBright(): CableBusTextures {
        return CableBusTextures.PartPatternTerm_Bright
    }

    override fun getFrontDark(): CableBusTextures {
        return CableBusTextures.PartPatternTerm_Dark
    }

    override fun getFrontColored(): CableBusTextures {
        return CableBusTextures.PartPatternTerm_Colored
    }
}
