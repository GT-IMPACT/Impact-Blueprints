package space.impact.iblueprints.integration.structures

import cpw.mods.fml.common.registry.GameData
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection
import space.impact.iblueprints.item.StructureBlueprintItem.PLACEMENT_SIZE
import space.impact.iblueprints.item.StructureBlueprintItem.PLAN_VERSION
import space.impact.iblueprints.item.StructureBlueprintItem.PLAN_TAG
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.SceneView
import space.impact.structures.alignment.IAlignmentProvider
import space.impact.structures.alignment.enumerable.ExtendedFacing

internal object SceneBlueprint {

    private const val MAX_BLOCKS = 16_384
    private const val MAX_OFFSET = 4096
    internal const val MAX_BYTES = 30_000

    internal fun capture(
        scene: BlockScene,
        channels: Map<String, Int>,
    ): NBTTagCompound? {
        val simulation = scene.simulation ?: return null
        val origin = simulation.origin
        val controller = scene.item(origin)?.let(::itemTag) ?: return null
        val facing = facing(scene, origin) ?: return null
        val positions = scene.positions().filterNot(origin::equals)
        if (positions.size !in 1..MAX_BLOCKS) {
            return null
        }
        val palette = NBTTagList()
        val paletteIndices = hashMapOf<NBTTagCompound, Int>()
        val placements = IntArray(positions.size * PLACEMENT_SIZE)
        val offset = IntArray(3)
        for ((index, position) in positions.withIndex()) {
            val item = scene.item(position)?.let(::itemTag) ?: return null
            val paletteIndex = paletteIndices.getOrPut(item) {
                val entryIndex = palette.tagCount()
                palette.appendTag(item)
                entryIndex
            }
            facing.getOffsetABC(
                intArrayOf(
                    position.x - origin.x,
                    position.y - origin.y,
                    position.z - origin.z,
                ),
                offset,
            )
            val start = index * PLACEMENT_SIZE
            offset.copyInto(
                destination = placements,
                destinationOffset = start,
            )
            placements[start + 3] = paletteIndex
            placements[start + 4] = localSide(
                facing = facing,
                side = simulation.hatchFacing(position),
            )
        }
        val channelTag = NBTTagCompound()
        for ((channel, value) in channels) {
            channelTag.setInteger(
                channel,
                value,
            )
        }
        return NBTTagCompound().apply {
            setInteger(
                "version",
                PLAN_VERSION,
            )
            setTag(
                "controller",
                controller,
            )
            setTag(
                "channels",
                channelTag,
            )
            setTag(
                "palette",
                palette,
            )
            setIntArray(
                "placements",
                placements,
            )
        }.takeIf(::isValid)
    }

    internal fun isValid(plan: NBTTagCompound): Boolean {
        if (plan.func_150296_c().size != 5 || plan.getInteger("version") != PLAN_VERSION ||
            !isItemTagValid(plan.getCompoundTag("controller")) || !plan.hasKey(
                "channels",
                NBT.TAG_COMPOUND,
            )
        ) {
            return false
        }
        val channels = plan.getCompoundTag("channels")
        val channelValues = channels.func_150296_c().associate { key ->
            val channel = key as String
            channel to channels.getInteger(channel)
        }
        if (channelValues.isNotEmpty() && !SaveProjectorChannels.isValid(channelValues)) {
            return false
        }
        val palette = plan.getTagList(
            "palette",
            NBT.TAG_COMPOUND,
        )
        val placements = plan.getIntArray("placements")
        val size = placements.size / PLACEMENT_SIZE
        if (placements.size % PLACEMENT_SIZE != 0 || size !in 1..MAX_BLOCKS || palette.tagCount() !in 1..size) {
            return false
        }
        for (index in 0 until palette.tagCount()) {
            if (!isItemTagValid(palette.getCompoundTagAt(index))) {
                return false
            }
        }
        val occupied = hashSetOf<ScenePosition>()
        for (index in placements.indices step PLACEMENT_SIZE) {
            val a = placements[index]
            val b = placements[index + 1]
            val c = placements[index + 2]
            if (a !in -MAX_OFFSET..MAX_OFFSET || b !in -MAX_OFFSET..MAX_OFFSET || c !in -MAX_OFFSET..MAX_OFFSET ||
                (a == 0 && b == 0 && c == 0) || placements[index + 3] !in 0 until palette.tagCount() ||
                placements[index + 4] !in -1..5
            ) {
                return false
            }
            if (!occupied.add(ScenePosition(
                    x = a,
                    y = b,
                    z = c,
                ))
            ) {
                return false
            }
        }
        return true
    }

    internal fun canStore(plan: NBTTagCompound): Boolean {
        return isValid(plan) && CompressedStreamTools.compress(plan).size <= MAX_BYTES
    }

    internal fun fromItem(stack: ItemStack?): NBTTagCompound? {
        val tag = stack?.tagCompound ?: return null
        if (!tag.hasKey(
                PLAN_TAG,
                NBT.TAG_COMPOUND,
            )
        ) {
            return null
        }
        return tag.getCompoundTag(PLAN_TAG)
    }

    internal fun controller(plan: NBTTagCompound): ItemStack? {
        return stack(plan.getCompoundTag("controller"))
    }

    internal fun channels(plan: NBTTagCompound): Map<String, Int> {
        val data = plan.getCompoundTag("channels")
        return data.func_150296_c().associate { key ->
            val name = key as String
            name to data.getInteger(name)
        }
    }

    internal fun materials(plan: NBTTagCompound): List<ItemStack>? {
        val controller = controller(plan) ?: return null
        val palette = plan.getTagList("palette", NBT.TAG_COMPOUND)
        val placements = plan.getIntArray("placements")
        if (placements.size % PLACEMENT_SIZE != 0) {
            return null
        }
        val stacks = mutableListOf(controller)
        val counts = IntArray(palette.tagCount())
        for (index in placements.indices step PLACEMENT_SIZE) {
            val paletteIndex = placements[index + 3]
            if (paletteIndex !in counts.indices) {
                return null
            }
            counts[paletteIndex]++
        }
        for ((index, count) in counts.withIndex()) {
            if (count == 0) {
                continue
            }
            val item = stack(palette.getCompoundTagAt(index)) ?: return null
            val existing = stacks.find {
                item.isItemEqual(it) && ItemStack.areItemStackTagsEqual(
                    item,
                    it,
                )
            }
            if (existing == null) {
                item.stackSize = count
                stacks.add(item)
            } else {
                existing.stackSize += count
            }
        }
        return stacks
    }

    internal fun restore(
        view: SceneView,
        plan: NBTTagCompound,
    ) {
        val simulation = view.scene.simulation ?: return
        val origin = simulation.origin
        val facing = facing(view.scene, origin) ?: return
        for (placement in placements(
            plan = plan,
            origin = origin,
            facing = facing,
        )) {
            val (position, item, side) = placement
            view.replaceBlock(
                position = position,
                stack = item,
                refresh = false,
            )
            if (side != null && view.scene.item(position)?.isItemEqual(item) == true) {
                view.setHatchFacing(
                    position = position,
                    facing = side,
                    refresh = false,
                )
            }
        }
        view.refreshLayout()
    }

    internal fun placements(
        plan: NBTTagCompound,
        origin: ScenePosition,
        facing: ExtendedFacing,
    ): List<BlueprintPlacement> {
        val palette = plan.getTagList("palette", NBT.TAG_COMPOUND)
        val placements = plan.getIntArray("placements")
        val items = List(palette.tagCount()) {
            stack(palette.getCompoundTagAt(it))
        }
        val result = ArrayList<BlueprintPlacement>(placements.size / PLACEMENT_SIZE)
        val offset = IntArray(3)
        for (index in placements.indices step PLACEMENT_SIZE) {
            val item = items.getOrNull(placements[index + 3]) ?: continue
            facing.getWorldOffset(
                intArrayOf(
                    placements[index],
                    placements[index + 1],
                    placements[index + 2],
                ),
                offset,
            )
            val position = ScenePosition(
                x = origin.x + offset[0],
                y = origin.y + offset[1],
                z = origin.z + offset[2],
            )
            val side = placements[index + 4]
            val worldSide = if (side in ForgeDirection.VALID_DIRECTIONS.indices) {
                val local = ForgeDirection.getOrientation(side)
                facing.getWorldOffset(
                    intArrayOf(
                        local.offsetX,
                        local.offsetY,
                        local.offsetZ,
                    ),
                    offset,
                )
                ForgeDirection.VALID_DIRECTIONS.first {
                    it.offsetX == offset[0] && it.offsetY == offset[1] && it.offsetZ == offset[2]
                }
            } else {
                null
            }
            result.add(
                BlueprintPlacement(
                    position = position,
                    stack = item,
                    hatchFacing = worldSide,
                ),
            )
        }
        return result
    }

    private fun facing(
        scene: BlockScene,
        origin: ScenePosition,
    ): ExtendedFacing? {
        val provider = scene.world.getTileEntity(
            x = origin.x,
            y = origin.y,
            z = origin.z,
        ) as? IAlignmentProvider ?: return null
        return provider.alignment?.extendedFacing
    }

    private fun stack(tag: NBTTagCompound): ItemStack? {
        val item = GameData.getItemRegistry().getObject(tag.getString("item")) ?: return null
        return ItemStack(
            item,
            1,
            tag.getInteger("damage"),
        )
    }

    private fun itemTag(stack: ItemStack): NBTTagCompound? {
        val name = GameData.getItemRegistry().getNameForObject(stack.item) ?: return null
        return NBTTagCompound().apply {
            setString(
                "item",
                name,
            )
            setInteger(
                "damage",
                stack.itemDamage,
            )
        }
    }

    private fun isItemTagValid(tag: NBTTagCompound): Boolean {
        val name = tag.getString("item")
        return tag.func_150296_c().size == 2 && name.isNotEmpty() &&
            GameData.getItemRegistry().containsKey(name) && tag.hasKey(
                "damage",
                NBT.TAG_INT,
            ) && tag.getInteger("damage") in 0..Short.MAX_VALUE
    }

    private fun localSide(
        facing: ExtendedFacing,
        side: ForgeDirection?,
    ): Int {
        if (side == null || side == ForgeDirection.UNKNOWN) {
            return -1
        }
        val offset = IntArray(3)
        facing.getOffsetABC(
            intArrayOf(
                side.offsetX,
                side.offsetY,
                side.offsetZ,
            ),
            offset,
        )
        return ForgeDirection.VALID_DIRECTIONS.indexOfFirst {
            it.offsetX == offset[0] && it.offsetY == offset[1] && it.offsetZ == offset[2]
        }
    }
}
