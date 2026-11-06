package space.impact.iblueprints.integration.structures

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.eventhandler.Event
import cpw.mods.fml.common.eventhandler.EventPriority
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import java.util.WeakHashMap
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityClientPlayerMP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.World
import net.minecraftforge.common.util.BlockSnapshot
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import space.impact.iblueprints.integration.gregtech.BlueprintHatch
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.scene.ScenePosition
import space.impact.structures.ConfigurationHandler
import space.impact.structures.ImpactStructures
import space.impact.structures.alignment.IAlignmentProvider
import space.impact.structures.item.ItemConstructableTrigger
import space.impact.structures.item.ItemDebugConstructableTrigger
import space.impact.structures.structure.IItemSource

internal object BlueprintWorldUse {

    private val lastBuild = WeakHashMap<EntityPlayerMP, Long>()

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public fun onUse(event: PlayerInteractEvent) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.useItem == Event.Result.DENY) {
            return
        }
        val player = event.entityPlayer
        if (player is FakePlayer) {
            return
        }
        val held = player.heldItem ?: return
        if (held.item !== StructureBlueprintItem && held.item !is ItemConstructableTrigger) {
            return
        }
        val plan = SceneBlueprint.fromItem(held) ?: return
        val world = event.world
        val tile = world.getTileEntity(
            event.x,
            event.y,
            event.z,
        ) as? IAlignmentProvider ?: return
        val facing = tile.alignment?.extendedFacing ?: return
        event.isCanceled = true

        val controller = SceneBlueprint.controller(plan)
        val matches = controller != null && matchesBlock(
            world = world,
            x = event.x,
            y = event.y,
            z = event.z,
            stack = controller,
        )
        if (!matches || !SceneBlueprint.isValid(plan)) {
            player.addChatMessage(ChatComponentTranslation("iblueprints.blueprint.wrong_controller"))
            return
        }
        if (world.isRemote && player.isSneaking) {
            sendBuildClick(event)
            return
        }
        if (!world.isRemote && (player !is EntityPlayerMP || !player.isSneaking)) {
            return
        }

        val placements = SceneBlueprint.placements(
            plan = plan,
            origin = ScenePosition(
                x = event.x,
                y = event.y,
                z = event.z,
            ),
            facing = facing,
        )
        if (world.isRemote) {
            showHints(
                world = world,
                placements = placements,
            )
        } else if (player is EntityPlayerMP) {
            build(
                player = player,
                world = world,
                placements = placements,
                creative = player.capabilities.isCreativeMode || held.item is ItemDebugConstructableTrigger,
            )
        }
    }

    @SideOnly(Side.CLIENT)
    private fun sendBuildClick(event: PlayerInteractEvent) {
        val client = Minecraft.getMinecraft()
        val player = event.entityPlayer as? EntityClientPlayerMP ?: return
        if (player !== client.thePlayer) {
            return
        }
        val target = client.objectMouseOver ?: return
        if (target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || target.blockX != event.x ||
            target.blockY != event.y || target.blockZ != event.z
        ) {
            return
        }
        val hit = target.hitVec
        // Forward the normal block interaction after suppressing the projector's default client preview.
        player.sendQueue.addToSendQueue(C09PacketHeldItemChange(player.inventory.currentItem))
        player.sendQueue.addToSendQueue(
            C08PacketPlayerBlockPlacement(
                event.x,
                event.y,
                event.z,
                event.face,
                player.heldItem,
                (hit.xCoord - event.x).toFloat(),
                (hit.yCoord - event.y).toFloat(),
                (hit.zCoord - event.z).toFloat(),
            ),
        )
    }

    @SideOnly(Side.CLIENT)
    private fun showHints(
        world: World,
        placements: List<BlueprintPlacement>,
    ) {
        ImpactStructures.startHinting(world)
        try {
            for ((position, stack, _) in placements) {
                val item = stack.item as? ItemBlock ?: continue
                val (x, y, z) = position
                if (!world.blockExists(
                        x,
                        y,
                        z,
                    )
                ) {
                    continue
                }
                val block = Block.getBlockFromItem(item)
                val icons = Array(ForgeDirection.VALID_DIRECTIONS.size) {
                    block.getIcon(
                        it,
                        item.getMetadata(stack.itemDamage),
                    )
                }
                ImpactStructures.hintParticle(
                    world,
                    x,
                    y,
                    z,
                    icons,
                    stack,
                )
            }
        } finally {
            ImpactStructures.endHinting(world)
        }
    }

    private fun build(
        player: EntityPlayerMP,
        world: World,
        placements: List<BlueprintPlacement>,
        creative: Boolean,
    ) {
        val configuration = ConfigurationHandler.INSTANCE
        val now = System.currentTimeMillis()
        val wait = configuration.autoPlaceInterval - (now - (lastBuild[player] ?: 0L))
        if (wait > 0) {
            return
        }
        lastBuild[player] = now
        val source = if (creative) {
            null
        } else {
            IItemSource.fromPlayer(player)
        }
        val budget = if (creative) {
            placements.size
        } else {
            configuration.autoPlaceBudget
        }
        val gregTech = Loader.isModLoaded("gregtech")
        var built = 0
        var pending = 0
        for (placement in placements) {
            val (position, stack, direction) = placement
            val (x, y, z) = position
            if (!world.blockExists(
                    x,
                    y,
                    z,
                )
            ) {
                pending++
                continue
            }
            if (matchesBlock(
                    world = world,
                    x = x,
                    y = y,
                    z = z,
                    stack = stack,
                )
            ) {
                val hatch = if (direction != null && gregTech) {
                    world.getTileEntity(
                        x,
                        y,
                        z,
                    )
                } else {
                    null
                }
                if (direction != null && gregTech && BlueprintHatch.facing(hatch) != direction) {
                    pending++
                }
                continue
            }
            if (built < budget && place(
                    placement = placement,
                    world = world,
                    player = player,
                    source = source,
                )
            ) {
                built++
                if (direction != null && gregTech) {
                    val hatch = world.getTileEntity(
                        x,
                        y,
                        z,
                    )
                    BlueprintHatch.orient(
                        tile = hatch,
                        direction = direction,
                    )
                    if (BlueprintHatch.facing(hatch) != direction) {
                        pending++
                    }
                }
            } else {
                pending++
            }
        }
        player.addChatMessage(
            ChatComponentTranslation(
                "iblueprints.blueprint.build_result",
                built,
                pending,
            ),
        )
    }

    internal fun place(
        placement: BlueprintPlacement,
        world: World,
        player: EntityPlayer,
        source: IItemSource?,
    ): Boolean {
        val (position, stack) = placement
        val (x, y, z) = position
        if (stack.item !is ItemBlock || !world.canMineBlock(
                player,
                x,
                y,
                z,
            )
        ) {
            return false
        }
        // ItemBlock redirects the placement to the adjacent cell if the target isn't replaceable.
        val block = world.getBlock(
            x,
            y,
            z,
        )
        if (!block.isReplaceable(
                world,
                x,
                y,
                z,
            ) || world.getTileEntity(
                x,
                y,
                z,
            ) != null || source?.takeOne(
                stack,
                true,
            ) == false
        ) {
            return false
        }
        val previous = BlockSnapshot.getBlockSnapshot(
            world,
            x,
            y,
            z,
        )
        val attempt = stack.copy()
        if (!attempt.tryPlaceItemIntoWorld(
                player,
                world,
                x,
                y,
                z,
                ForgeDirection.UP.ordinal,
                0.5f,
                0.5f,
                0.5f,
            ) || attempt.stackSize == stack.stackSize
        ) {
            return false
        }
        if (!matchesBlock(
                world = world,
                x = x,
                y = y,
                z = z,
                stack = stack,
            ) || source?.takeOne(
                stack,
                false,
            ) == false
        ) {
            previous.restore(
                true,
                false,
            )
            return false
        }
        return true
    }

    private fun matchesBlock(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        stack: ItemStack,
    ): Boolean {
        val block = world.getBlock(
            x,
            y,
            z,
        )
        return stack.item === Item.getItemFromBlock(block) && stack.itemDamage == block.getDamageValue(
            world,
            x,
            y,
            z,
        )
    }
}
