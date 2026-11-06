package space.impact.iblueprints.integration.gregtech

import gregtech.api.GregTech_API
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase
import gregtech.api.util.GTStructureUtility
import java.util.IdentityHashMap
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.integration.structures.PreviewPlayer
import space.impact.iblueprints.planner.tracePlannerStep
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.ISceneSimulation
import space.impact.iblueprints.scene.SceneElement
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.world.SceneBlockState
import space.impact.iblueprints.scene.world.SceneWorld
import space.impact.structures.structure.IStructureElement

internal class GregTechHatchChoices(
    private val scene: BlockScene,
    override val origin: ScenePosition,
    private val trigger: ItemStack,
) : ISceneSimulation {

    private val elements = hashMapOf<ScenePosition, IStructureElement<Any>>()
    private val originals = hashMapOf<ScenePosition, SceneBlockState>()
    private val hatches by lazy(LazyThreadSafetyMode.NONE) {
        GregTech_API.METATILEENTITIES.filterIsInstance<GT_MetaTileEntity_Hatch>()
    }
    private val unavailableHatches = IdentityHashMap<IStructureElement<Any>, MutableSet<GT_MetaTileEntity_Hatch>>()

    internal fun track(
        position: ScenePosition,
        element: IStructureElement<Any>,
        snapshot: SceneElement,
    ): () -> List<ItemStack> {
        elements[position] = element
        return {
            find(
                position = position,
                element = element,
                snapshot = snapshot,
            )
        }
    }

    private fun find(
        position: ScenePosition,
        element: IStructureElement<Any>,
        snapshot: SceneElement,
    ): List<ItemStack> {
        val choices = mutableListOf<ItemStack>()
        try {
            val controller = scene.world.getTileEntity(
                x = origin.x,
                y = origin.y,
                z = origin.z,
            ) ?: return choices
            val controllerData = NBTTagCompound()
            controller.writeToNBT(controllerData)
            val probe = scene.world.copyForProbe()
            try {
                for (hatch in hatches) {
                    if (unavailableHatches[element]?.contains(hatch) == true) {
                        snapshot.hatchChoicesIncomplete = true
                        continue
                    }
                    try {
                        val stack = hatch.getStackForm(1)
                        if (accepts(
                                probe = probe,
                                controllerData = controllerData,
                                position = position,
                                element = element,
                                id = stack.itemDamage,
                                metadata = hatch.tileEntityBaseType.toInt(),
                            )
                        ) {
                            choices.add(stack)
                        }
                    } catch (failure: Throwable) {
                        if (failure is LinkageError) {
                            unavailableHatches.getOrPut(element) {
                                hashSetOf()
                            }.add(hatch)
                        }
                        recordFailure(
                            position = position,
                            snapshot = snapshot,
                            failure = failure,
                        )
                    }
                }
            } finally {
                probe.clear()
            }
        } catch (failure: Throwable) {
            recordFailure(
                position = position,
                snapshot = snapshot,
                failure = failure,
            )
        }
        return choices
    }

    override fun canReplace(
        position: ScenePosition,
        stack: ItemStack,
    ): Boolean {
        return position != origin && position in elements && stack.item is ItemBlock
    }

    override fun replaceAll(
        targets: Map<ScenePosition, ForgeDirection?>,
        stack: ItemStack,
    ): Boolean {
        var position = targets.keys.firstOrNull() ?: return false
        val previous = linkedMapOf<ScenePosition, SceneBlockState>()
        val staged = linkedMapOf<ScenePosition, SceneBlockState>()
        val probe = lazy {
            scene.world.copyForProbe()
        }
        try {
            val hatch = GTStructureUtility.getMetaTileEntity(stack) as? GT_MetaTileEntity_Hatch
            for ((target, facing) in targets) {
                position = target
                val snapshot = scene.elements[position] ?: return false
                if (!canReplace(
                        position = position,
                        stack = stack,
                    )
                ) {
                    return false
                }
                val replacement = if (snapshot.matchesOriginal(stack)) {
                    originals[position] ?: continue
                } else {
                    if (hatch == null && snapshot.alternatives().none(stack::isItemEqual)) {
                        return false
                    }
                    prepareReplacement(
                        probe = probe.value,
                        position = position,
                        stack = stack,
                        hatch = hatch,
                        preferredFacing = facing,
                    ) ?: return false
                }
                previous[position] = scene.world.blockState(position) ?: return false
                staged[position] = replacement
            }
            val applied = mutableListOf<ScenePosition>()
            try {
                for ((target, replacement) in staged) {
                    position = target
                    applied.add(position)
                    setSceneState(
                        position = position,
                        state = replacement,
                    )
                    val facing = targets[position]
                    if (facing != null && scene.elements.getValue(position).matchesOriginal(stack)) {
                        setHatchFacing(
                            position = position,
                            facing = facing,
                        )
                    }
                }
            } catch (failure: Throwable) {
                for (target in applied.asReversed()) {
                    try {
                        setSceneState(
                            position = target,
                            state = previous.getValue(target),
                        )
                    } catch (rollbackFailure: Throwable) {
                        if (rollbackFailure !== failure) {
                            failure.addSuppressed(rollbackFailure)
                        }
                    }
                }
                throw failure
            }
            for ((target, original) in previous) {
                if (scene.elements.getValue(target).matchesOriginal(stack)) {
                    originals.remove(target)
                } else {
                    originals.putIfAbsent(
                        target,
                        original,
                    )
                }
            }
            return true
        } catch (failure: Throwable) {
            recordFailure(
                position = position,
                snapshot = scene.elements.getValue(position),
                failure = failure,
            )
            return false
        } finally {
            if (probe.isInitialized()) {
                probe.value.clear()
            }
        }
    }

    private fun setSceneState(
        position: ScenePosition,
        state: SceneBlockState,
    ) {
        scene.world.setBlockState(
            position = position,
            state = state,
        )
        val hatch = hatchTile(position)?.metaTileEntity as? GT_MetaTileEntity_Hatch
        hatch?.let(GregTechScenes::refreshHatchTexture)
    }

    private fun prepareReplacement(
        probe: SceneWorld,
        position: ScenePosition,
        stack: ItemStack,
        hatch: GT_MetaTileEntity_Hatch?,
        preferredFacing: ForgeDirection?,
    ): SceneBlockState? {
        val element = elements[position] ?: return null
        val controller = probe.getTileEntity(
            x = origin.x,
            y = origin.y,
            z = origin.z,
        ) as? IGregTechTileEntity ?: return null
        val data = NBTTagCompound()
        (controller as TileEntity).writeToNBT(data)
        try {
            val accepted = if (hatch != null) {
                accepts(
                    probe = probe,
                    controllerData = data,
                    position = position,
                    element = element,
                    id = stack.itemDamage,
                    metadata = hatch.tileEntityBaseType.toInt(),
                    directions = ForgeDirection.VALID_DIRECTIONS.sortedBy { direction ->
                        if (direction == preferredFacing) {
                            return@sortedBy -1
                        }
                        val neighbor = ScenePosition(
                            x = position.x + direction.offsetX,
                            y = position.y + direction.offsetY,
                            z = position.z + direction.offsetZ,
                        )
                        val sidePriority = when (direction) {
                            ForgeDirection.DOWN -> 2
                            ForgeDirection.UP -> 1
                            else -> 0
                        }
                        val exposed = neighbor !in scene.elements && probe.isAirBlock(
                            neighbor.x,
                            neighbor.y,
                            neighbor.z,
                        )
                        if (!exposed) {
                            sidePriority + 3
                        } else {
                            sidePriority
                        }
                    },
                )
            } else {
                val item = stack.item as ItemBlock
                item.placeBlockAt(
                    stack.copy(),
                    PreviewPlayer(probe),
                    probe,
                    position.x,
                    position.y,
                    position.z,
                    ForgeDirection.UP.ordinal,
                    0.5f,
                    0.5f,
                    0.5f,
                    item.getMetadata(stack.itemDamage),
                ) && element.check(
                    controller.metaTileEntity,
                    probe,
                    position.x,
                    position.y,
                    position.z,
                )
            }
            return if (accepted) {
                probe.blockState(position)
            } else {
                null
            }
        } finally {
            // Each candidate sees the same controller and neighboring blocks as before the batch.
            probe.setBlockState(
                position = position,
                state = checkNotNull(scene.world.blockState(position)),
            )
            probe.loadProbeTile(
                position = origin,
                data = data,
            )
        }
    }

    override fun hatchFacing(position: ScenePosition): ForgeDirection? {
        val tile = hatchTile(position) ?: return null
        return ForgeDirection.getOrientation(tile.frontFacing.toInt())
    }

    override fun nextHatchFacing(position: ScenePosition): ForgeDirection? {
        val tile = hatchTile(position) ?: return null
        val sides = ForgeDirection.VALID_DIRECTIONS
        val current = tile.frontFacing.toInt()
        for (offset in 1..sides.size) {
            val next = sides[(current + offset).mod(sides.size)]
            if (next.ordinal != current && tile.isValidFacing(next.ordinal.toByte())) {
                return next
            }
        }
        return null
    }

    override fun setHatchFacing(
        position: ScenePosition,
        facing: ForgeDirection,
    ): Boolean {
        val tile = hatchTile(position) ?: return false
        val side = facing.ordinal.toByte()
        if (facing == ForgeDirection.UNKNOWN || !tile.isValidFacing(side)) {
            return false
        }
        tile.frontFacing = side
        return tile.frontFacing == side
    }

    private fun hatchTile(position: ScenePosition): IGregTechTileEntity? {
        val tile = scene.world.getTileEntity(
            x = position.x,
            y = position.y,
            z = position.z,
        ) as? IGregTechTileEntity ?: return null
        return tile.takeIf {
            it.metaTileEntity is GT_MetaTileEntity_Hatch
        }
    }

    override fun validate(): Boolean? {
        val details = "structure=${scene.title}"
        try {
            val probe = tracePlannerStep(
                step = "viewer.copyValidationProbe",
                details = details,
            ) {
                scene.world.copyForProbe()
            }
            try {
                val tile = probe.getTileEntity(
                    x = origin.x,
                    y = origin.y,
                    z = origin.z,
                ) as? IGregTechTileEntity ?: return null
                val machine = tile.metaTileEntity as? GT_MetaTileEntity_MultiBlockBase ?: return null
                probe.isRemote = false
                return tracePlannerStep(
                    step = "viewer.checkMachine",
                    details = details,
                ) {
                    machine.checkMachine(
                        tile,
                        tile.getStackInSlot(1),
                    )
                }
            } finally {
                tracePlannerStep(
                    step = "viewer.clearValidationProbe",
                    details = details,
                ) {
                    probe.clear()
                }
            }
        } catch (failure: Throwable) {
            if (failure !is Exception && failure !is LinkageError) {
                throw failure
            }
            if (reportedFailures.add(failure.toString())) {
                LogManager.getLogger("ImpactBlueprints").warn("Cannot validate ${scene.title}", failure)
            }
            return null
        }
    }

    private fun accepts(
        probe: SceneWorld,
        controllerData: NBTTagCompound,
        position: ScenePosition,
        element: IStructureElement<Any>,
        id: Int,
        metadata: Int,
        directions: Iterable<ForgeDirection> = ForgeDirection.VALID_DIRECTIONS.asIterable(),
    ): Boolean {
        probe.setBlock(
            x = position.x,
            y = position.y,
            z = position.z,
            block = GregTech_API.sBlockMachines,
            meta = metadata,
            flags = 0,
        )
        val tile = probe.getTileEntity(
            x = position.x,
            y = position.y,
            z = position.z,
        ) as? IGregTechTileEntity ?: return false
        tile.setInitialValuesAsNBT(
            null,
            id.toShort(),
        )
        for (direction in directions) {
            val facing = direction.ordinal.toByte()
            if (!tile.isValidFacing(facing)) {
                continue
            }
            tile.frontFacing = facing
            val controller = probe.getTileEntity(
                x = origin.x,
                y = origin.y,
                z = origin.z,
            ) as? IGregTechTileEntity ?: return false
            val machine = controller.metaTileEntity ?: return false
            if (!element.couldBeValid(
                    machine,
                    probe,
                    position.x,
                    position.y,
                    position.z,
                    trigger,
                )
            ) {
                continue
            }
            try {
                if (element.check(
                        machine,
                        probe,
                        position.x,
                        position.y,
                        position.z,
                    )
                ) {
                    return true
                }
            } finally {
                // check() can register hatches on the controller even when a later check fails.
                probe.loadProbeTile(
                    position = origin,
                    data = controllerData,
                )
            }
        }
        return false
    }

    private fun recordFailure(
        position: ScenePosition,
        snapshot: SceneElement,
        failure: Throwable,
    ) {
        if (failure !is Exception && failure !is LinkageError) {
            throw failure
        }
        if (!snapshot.hatchChoicesIncomplete) {
            snapshot.hatchChoicesIncomplete = true
            val reason = failure.toString()
            if (reportedFailures.add(reason)) {
                LogManager.getLogger("ImpactBlueprints").warn(
                    "Some hatch choices could not be checked for ${scene.title} at $position: $reason",
                )
            }
        }
    }

    private companion object {
        val reportedFailures = hashSetOf<String>()
    }
}
