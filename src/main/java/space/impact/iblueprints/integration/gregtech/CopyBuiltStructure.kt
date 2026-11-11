package space.impact.iblueprints.integration.gregtech

import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ChatComponentTranslation
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.MODID
import space.impact.iblueprints.integration.structures.BlueprintWorldUse
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.item.BlankBlueprintItem
import space.impact.iblueprints.item.StructureBlueprintItem

internal object CopyBuiltStructure {

    internal fun onUse(event: PlayerInteractEvent) {
        val world = event.world
        val tile = world.getTileEntity(
            event.x,
            event.y,
            event.z,
        ) as? IGregTechTileEntity ?: return
        val machine = tile.metaTileEntity as? GT_MetaTileEntity_EnhancedMultiBlockBase<*> ?: return
        event.isCanceled = true
        if (world.isRemote) {
            BlueprintWorldUse.sendBuildClick(event)
            return
        }
        val player = event.entityPlayer as? EntityPlayerMP ?: return
        if (!machine.mMachine) {
            player.addChatMessage(ChatComponentTranslation("iblueprints.blueprint.not_formed"))
            return
        }
        if (player.heldItem.stackSize > 1 && player.inventory.firstEmptyStack < 0) {
            player.addChatMessage(ChatComponentTranslation("iblueprints.blueprint.inventory_full"))
            return
        }
        val plan = try {
            BuiltStructureCapture.capture(machine)
        } catch (failure: Throwable) {
            if (failure !is Exception && failure !is LinkageError) {
                throw failure
            }
            LogManager.getLogger("ImpactBlueprints").warn(
                "Cannot copy built structure ${machine.javaClass.name}",
                failure,
            )
            null
        }
        if (plan == null || !save(
                player = player,
                plan = plan,
            )
        ) {
            player.addChatMessage(ChatComponentTranslation("iblueprints.blueprint.capture_failed"))
            return
        }
        player.sendContainerToPlayer(player.inventoryContainer)
        player.addChatMessage(ChatComponentTranslation("iblueprints.blueprint.saved"))
    }

    internal fun save(
        player: EntityPlayer,
        plan: NBTTagCompound,
    ): Boolean {
        val blank = player.heldItem ?: return false
        if (blank.item !== BlankBlueprintItem || blank.stackSize <= 0 || !SceneBlueprint.canStore(plan)) {
            return false
        }
        val controller = SceneBlueprint.controller(plan) ?: return false
        val outputSlot = if (blank.stackSize == 1) {
            player.inventory.currentItem
        } else {
            player.inventory.firstEmptyStack
        }
        if (outputSlot < 0) {
            return false
        }
        val blueprint = ItemStack(StructureBlueprintItem).apply {
            tagCompound = NBTTagCompound().apply {
                setTag(
                    StructureBlueprintItem.PLAN_TAG,
                    plan.copy(),
                )
                setString(
                    StructureBlueprintItem.RECIPE_TAG,
                    "$MODID:metatile_${controller.itemDamage}",
                )
            }
        }
        if (blank.stackSize > 1) {
            blank.stackSize--
        }
        player.inventory.setInventorySlotContents(
            outputSlot,
            blueprint,
        )
        player.inventory.markDirty()
        return true
    }
}
