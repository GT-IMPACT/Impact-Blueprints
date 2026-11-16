package space.impact.iblueprints.integration.structures

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.eventhandler.Event
import cpw.mods.fml.common.eventhandler.EventPriority
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import java.util.WeakHashMap
import java.util.function.Consumer
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
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.FluidStack
import space.impact.iblueprints.integration.gregtech.BlueprintHatch
import space.impact.iblueprints.integration.gregtech.CopyBuiltStructure
import space.impact.iblueprints.integration.gregtech.HatchHintIcons
import space.impact.iblueprints.item.BlankBlueprintItem
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.planner.logPlannerTrace
import space.impact.iblueprints.scene.FluidMaterial
import space.impact.iblueprints.scene.ScenePosition
import space.impact.structures.ConfigurationHandler
import space.impact.structures.ImpactStructures
import space.impact.structures.alignment.IAlignmentProvider
import space.impact.structures.item.ItemConstructableTrigger
import space.impact.structures.item.ItemDebugConstructableTrigger
import space.impact.structures.structure.IItemSource
import space.impact.structures.util.FluidUtility

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
        if (held.item === BlankBlueprintItem && Loader.isModLoaded("gregtech")) {
            CopyBuiltStructure.onUse(event)
            return
        }
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
    internal fun sendBuildClick(event: PlayerInteractEvent) {
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
            val gregTech = Loader.isModLoaded("gregtech")
            val doorIcons = DoorHintIcons.resolve(placements)
            for (placement in placements) {
                val (position, stack, _, doorMetadata) = placement
                val block = placement.block ?: continue
                val (x, y, z) = position
                if (!world.blockExists(
                        x,
                        y,
                        z,
                    )
                ) {
                    continue
                }
                val hintStack = if (doorMetadata != null) {
                    BlueprintDoor.material() ?: stack
                } else if (placement.fluidBlock != null) {
                    FluidMaterial.stack(block)
                } else {
                    stack
                }
                val hatchIcons = if (gregTech) {
                    HatchHintIcons.resolve(placement)
                } else {
                    null
                }
                val icons = doorIcons[position] ?: hatchIcons ?: Array(ForgeDirection.VALID_DIRECTIONS.size) {
                    block.getIcon(
                        it,
                        placement.metadata,
                    )
                }
                ImpactStructures.hintParticle(
                    world,
                    x,
                    y,
                    z,
                    icons,
                    hintStack,
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
        val failures = linkedMapOf<BlueprintPlacementResult, BlueprintPlacement>()
        var missing: Any? = null
        var built = 0
        var pending = 0
        val byPosition = placements.associateBy(BlueprintPlacement::position)
        for (placement in placements) {
            val (position, stack, direction, doorMetadata) = placement
            val (x, y, z) = position
            if (doorMetadata != null && doorMetadata and BlueprintDoor.UPPER_HALF != 0) {
                continue
            }
            if (!world.blockExists(
                    x,
                    y,
                    z,
                )
            ) {
                pending++
                continue
            }
            val upperDoor = if (doorMetadata != null) {
                byPosition[position.copy(y = y + 1)]
            } else {
                null
            }
            val upperMetadata = upperDoor?.doorMetadata
            val hasDoorPair = upperDoor != null && upperMetadata != null &&
                upperMetadata and BlueprintDoor.UPPER_HALF != 0 && upperDoor.stack?.item === stack?.item
            val matches = if (placement.fluidBlock != null) {
                matchesFluid(
                    world = world,
                    placement = placement,
                )
            } else if (doorMetadata == null && stack != null) {
                matchesBlock(
                    world = world,
                    x = x,
                    y = y,
                    z = z,
                    stack = stack,
                )
            } else {
                hasDoorPair && matchesDoorHalf(
                    world = world,
                    placement = placement,
                ) && matchesDoorHalf(
                    world = world,
                    placement = upperDoor,
                )
            }
            if (matches) {
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
            if (built >= budget) {
                pending++
                continue
            }
            val result = if (doorMetadata == null) {
                place(
                    placement = placement,
                    world = world,
                    player = player,
                    source = source,
                )
            } else if (hasDoorPair) {
                placeDoor(
                    lower = placement,
                    upper = upperDoor,
                    world = world,
                    player = player,
                    source = source,
                )
            } else {
                BlueprintPlacementResult.REJECTED
            }
            if (result == BlueprintPlacementResult.PLACED) {
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
                failures.putIfAbsent(
                    result,
                    placement,
                )
                if (result == BlueprintPlacementResult.MISSING_ITEM && missing == null) {
                    missing = if (doorMetadata != null) {
                        BlueprintDoor.material()?.func_151000_E()
                    } else if (placement.fluidBlock != null) {
                        val fluid = FluidUtility.getFluid(placement.fluidBlock)
                        fluid.getLocalizedName(FluidStack(
                            fluid,
                            FluidContainerRegistry.BUCKET_VOLUME,
                        ))
                    } else {
                        stack?.func_151000_E()
                    }
                }
            }
        }
        val rejected = failures.entries.joinToString {
            val item = it.value.stack
            val name = if (item != null) {
                "${Item.itemRegistry.getNameForObject(item.item)}:${item.itemDamage}"
            } else {
                "${Block.blockRegistry.getNameForObject(it.value.fluidBlock)}:${it.value.fluidMetadata}"
            }
            "${it.key}=$name@${it.value.position}"
        }
        logPlannerTrace("world.build placed=$built remaining=$pending creative=$creative rejected=[$rejected]")
        if (built == 0 && missing != null) {
            player.addChatMessage(ChatComponentTranslation(
                "impactapi.autoplace.missing_block",
                missing,
            ))
        }
        player.addChatMessage(
            ChatComponentTranslation(
                "iblueprints.blueprint.build_result",
                built,
                pending,
            ),
        )
    }

    internal fun placeDoor(
        lower: BlueprintPlacement,
        upper: BlueprintPlacement,
        world: World,
        player: EntityPlayer,
        source: IItemSource?,
    ): BlueprintPlacementResult {
        val material = BlueprintDoor.material() ?: return BlueprintPlacementResult.UNSUPPORTED_ITEM
        val halves = listOf(
            lower,
            upper,
        )
        for ((position) in halves) {
            val (x, y, z) = position
            if (!world.blockExists(
                    x,
                    y,
                    z,
                )
            ) {
                return BlueprintPlacementResult.REJECTED
            }
            placementObstruction(
                world = world,
                player = player,
                position = position,
            )?.let {
                return it
            }
        }
        if (source?.takeOne(
                material,
                true,
            ) == false
        ) {
            return BlueprintPlacementResult.MISSING_ITEM
        }
        val snapshots = halves.map { (position) ->
            BlockSnapshot.getBlockSnapshot(
                world,
                position.x,
                position.y,
                position.z,
            )
        }
        val (x, y, z) = lower.position
        // The real item places both halves in one Forge placement event.
        if (!material.copy().tryPlaceItemIntoWorld(
                player,
                world,
                x,
                y - 1,
                z,
                ForgeDirection.UP.ordinal,
                0.5f,
                1f,
                0.5f,
            )
        ) {
            return BlueprintPlacementResult.REJECTED
        }
        val result = if (halves.any {
                !matchesDoorHalf(
                    world = world,
                    placement = it,
                )
            }
        ) {
            BlueprintPlacementResult.WRONG_BLOCK
        } else if (source?.takeOne(
                material,
                false,
            ) == false
        ) {
            BlueprintPlacementResult.EXTRACTION_FAILED
        } else {
            BlueprintPlacementResult.PLACED
        }
        if (result != BlueprintPlacementResult.PLACED) {
            for (snapshot in snapshots.asReversed()) {
                snapshot.restore(
                    true,
                    false,
                )
            }
            return result
        }
        for ((position, _, _, metadata) in halves) {
            world.setBlockMetadataWithNotify(
                position.x,
                position.y,
                position.z,
                requireNotNull(metadata),
                2,
            )
        }
        return BlueprintPlacementResult.PLACED
    }

    private fun matchesDoorHalf(
        world: World,
        placement: BlueprintPlacement,
    ): Boolean {
        val (x, y, z) = placement.position
        val metadata = requireNotNull(placement.doorMetadata)
        return world.getBlock(
            x,
            y,
            z,
        ) === placement.block && world.getBlockMetadata(
            x,
            y,
            z,
        ) and BlueprintDoor.UPPER_HALF == metadata and BlueprintDoor.UPPER_HALF
    }

    internal fun place(
        placement: BlueprintPlacement,
        world: World,
        player: EntityPlayer,
        source: IItemSource?,
    ): BlueprintPlacementResult {
        val (position, stack) = placement
        val (x, y, z) = position
        if (placement.fluidBlock != null) {
            return placeFluid(
                placement = placement,
                world = world,
                player = player,
                source = source,
            )
        }
        if (stack?.item !is ItemBlock) {
            return BlueprintPlacementResult.UNSUPPORTED_ITEM
        }
        val block = Block.getBlockFromItem(stack.item)
        if (FluidUtility.getFluid(block) != null) {
            return placeFluid(
                placement = placement.copy(
                    fluidBlock = block,
                    fluidMetadata = placement.metadata,
                ),
                world = world,
                player = player,
                source = source,
            )
        }
        placementObstruction(
            world = world,
            player = player,
            position = position,
        )?.let {
            return it
        }
        if (source?.takeOne(
                stack,
                true,
            ) == false
        ) {
            return BlueprintPlacementResult.MISSING_ITEM
        }
        val previous = BlockSnapshot.getBlockSnapshot(
            world,
            x,
            y,
            z,
        )
        if (!stack.copy().tryPlaceItemIntoWorld(
                player,
                world,
                x,
                y,
                z,
                ForgeDirection.UP.ordinal,
                0.5f,
                0.5f,
                0.5f,
            )
        ) {
            return BlueprintPlacementResult.REJECTED
        }
        val result = if (!matchesBlock(
                world = world,
                x = x,
                y = y,
                z = z,
                stack = stack,
            )
        ) {
            BlueprintPlacementResult.WRONG_BLOCK
        } else if (source?.takeOne(
                stack,
                false,
            ) == false
        ) {
            BlueprintPlacementResult.EXTRACTION_FAILED
        } else {
            BlueprintPlacementResult.PLACED
        }
        if (result != BlueprintPlacementResult.PLACED) {
            previous.restore(
                true,
                false,
            )
        }
        return result
    }

    private fun matchesFluid(
        world: World,
        placement: BlueprintPlacement,
    ): Boolean {
        val (x, y, z) = placement.position
        return world.getBlock(
            x,
            y,
            z,
        ) === placement.fluidBlock && world.getBlockMetadata(
            x,
            y,
            z,
        ) == placement.fluidMetadata
    }

    private fun placeFluid(
        placement: BlueprintPlacement,
        world: World,
        player: EntityPlayer,
        source: IItemSource?,
    ): BlueprintPlacementResult {
        val block = placement.fluidBlock ?: return BlueprintPlacementResult.UNSUPPORTED_ITEM
        val fluid = FluidUtility.getFluid(block) ?: return BlueprintPlacementResult.UNSUPPORTED_ITEM
        placementObstruction(
            world = world,
            player = player,
            position = placement.position,
        )?.let {
            return it
        }
        val request = FluidStack(
            fluid,
            FluidContainerRegistry.BUCKET_VOLUME,
        )
        val returnContainer = Consumer<ItemStack> { container ->
            if (!player.inventory.addItemStackToInventory(container)) {
                player.dropPlayerItemWithRandomChoice(
                    container,
                    false,
                )
            }
        }
        if (source != null && !FluidUtility.takeFluid(
                source,
                request,
                true,
                returnContainer,
            )
        ) {
            return BlueprintPlacementResult.MISSING_ITEM
        }
        val (x, y, z) = placement.position
        val previous = BlockSnapshot.getBlockSnapshot(
            world,
            x,
            y,
            z,
        )
        if (!world.setBlock(
                x,
                y,
                z,
                block,
                placement.fluidMetadata,
                2,
            )
        ) {
            return BlueprintPlacementResult.REJECTED
        }
        if (source != null && !FluidUtility.takeFluid(
                source,
                request,
                false,
                returnContainer,
            )
        ) {
            previous.restore(
                true,
                false,
            )
            return BlueprintPlacementResult.EXTRACTION_FAILED
        }
        world.notifyBlockChange(
            x,
            y,
            z,
            block,
        )
        player.inventoryContainer.detectAndSendChanges()
        return BlueprintPlacementResult.PLACED
    }

    private fun placementObstruction(
        world: World,
        player: EntityPlayer,
        position: ScenePosition,
    ): BlueprintPlacementResult? {
        val (x, y, z) = position
        if (!world.canMineBlock(
                player,
                x,
                y,
                z,
            )
        ) {
            return BlueprintPlacementResult.PROTECTED
        }
        // ItemBlock redirects the placement to the adjacent cell if the target isn't replaceable.
        if (!ImpactStructures.isBlockTriviallyReplaceable(
                world,
                x,
                y,
                z,
                player,
            ) || world.getTileEntity(
                x,
                y,
                z,
            ) != null
        ) {
            return BlueprintPlacementResult.OCCUPIED
        }
        return null
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
