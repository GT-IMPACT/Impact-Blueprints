package space.impact.iblueprints.scene

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import space.impact.iblueprints.integration.structures.BlueprintDoor
import space.impact.iblueprints.scene.world.SceneWorld

/** Client-side scene built through API. It never ticks or accesses the player's world. */
public class BlockScene(public val title: String) : AutoCloseable {

    internal val world: SceneWorld = SceneWorld()
    public val elements: MutableMap<ScenePosition, SceneElement> = linkedMapOf()
    public val diagnostics: MutableList<String> = mutableListOf()
    internal var simulation: ISceneSimulation? = null

    private val descriptions: MutableMap<ScenePosition, List<String>> = linkedMapOf()

    public fun block(
        position: ScenePosition,
        block: Block,
        metadata: Int = 0,
        tileData: NBTTagCompound? = null,
    ): BlockScene {
        require(metadata in 0..15)
        require(position.y in 0..255)
        world.setBlock(
            x = position.x,
            y = position.y,
            z = position.z,
            block = block,
            meta = metadata,
            flags = 0,
        )
        if (tileData != null) {
            val tile = world.getTileEntity(
                x = position.x,
                y = position.y,
                z = position.z,
            )
            requireNotNull(tile) {
                "Block at $position has no TileEntity"
            }
            val tag = tileData.copy() as NBTTagCompound
            tag.setInteger(
                "x",
                position.x,
            )
            tag.setInteger(
                "y",
                position.y,
            )
            tag.setInteger(
                "z",
                position.z,
            )
            tile.readFromNBT(tag)
        }
        return this
    }

    public fun describe(
        position: ScenePosition,
        lines: List<String>,
    ): BlockScene {
        descriptions[position] = lines.toList()
        return this
    }

    public fun description(position: ScenePosition): List<String> {
        return descriptions[position].orEmpty()
    }

    public fun positions(): List<ScenePosition> {
        return world.positions()
    }

    public fun bounds(): SceneBounds {
        return SceneBounds.of(positions())
    }

    public fun item(position: ScenePosition): ItemStack? {
        val block = world.getBlock(
            x = position.x,
            y = position.y,
            z = position.z,
        )
        if (BlueprintDoor.isReinforced(block)) {
            return BlueprintDoor.material()
        }
        val item = Item.getItemFromBlock(block) ?: return null
        val damage = block.getDamageValue(
            world,
            position.x,
            position.y,
            position.z,
        )
        return ItemStack(
            item,
            1,
            damage,
        )
    }

    internal fun isUpperDoorHalf(position: ScenePosition): Boolean {
        val (x, y, z) = position
        return BlueprintDoor.isReinforced(world.getBlock(
            x = x,
            y = y,
            z = z,
        )) && world.getBlockMetadata(
            x = x,
            y = y,
            z = z,
        ) and BlueprintDoor.UPPER_HALF != 0
    }

    override fun close() {
        simulation = null
        world.clear()
        descriptions.clear()
        elements.clear()
        diagnostics.clear()
    }
}
