package space.impact.iblueprints.scene.world

import net.minecraft.block.Block
import net.minecraft.client.multiplayer.ChunkProviderClient
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.profiler.Profiler
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraft.world.WorldProviderSurface
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.storage.SaveHandlerMP
import space.impact.iblueprints.scene.ScenePosition

/** In-memory construction sandbox. No neighbor notifications, scheduling, saving or ticking. */
internal class SceneWorld : World(
    SaveHandlerMP(),
    "ImpactBlueprintsPreview",
    WorldSettings(
        0,
        WorldSettings.GameType.CREATIVE,
        false,
        false,
        WorldType.FLAT,
    ),
    WorldProviderSurface(),
    Profiler(),
) {

    private val blocks: MutableMap<ScenePosition, Block> = linkedMapOf()
    private val metadata: MutableMap<ScenePosition, Int> = hashMapOf()
    private val tiles: MutableMap<ScenePosition, TileEntity> = hashMapOf()

    init {
        provider.dimensionId = Int.MAX_VALUE
        isRemote = true
    }

    override fun createChunkProvider(): IChunkProvider {
        return ChunkProviderClient(this)
    }

    override fun func_152379_p(): Int {
        return 0
    }

    override fun getEntityByID(id: Int): Entity? {
        return null
    }

    override fun chunkExists(
        x: Int,
        z: Int,
    ): Boolean {
        return true
    }

    override fun getBlock(
        x: Int,
        y: Int,
        z: Int,
    ): Block {
        return blocks[ScenePosition(
            x = x,
            y = y,
            z = z,
        )] ?: Blocks.air
    }

    override fun getBlockMetadata(
        x: Int,
        y: Int,
        z: Int,
    ): Int {
        return metadata[ScenePosition(
            x = x,
            y = y,
            z = z,
        )] ?: 0
    }

    override fun setBlock(
        x: Int,
        y: Int,
        z: Int,
        block: Block,
        meta: Int,
        flags: Int,
    ): Boolean {
        if (y !in 0..255) {
            return false
        }
        val position = ScenePosition(
            x = x,
            y = y,
            z = z,
        )
        removeTileEntity(
            x = x,
            y = y,
            z = z,
        )
        if (block === Blocks.air) {
            blocks.remove(position)
            metadata.remove(position)
        } else {
            blocks[position] = block
            metadata[position] = meta
            block.createTileEntity(
                this,
                meta,
            )?.let {
                setTileEntity(
                    x = x,
                    y = y,
                    z = z,
                    tile = it,
                )
            }
        }
        return true
    }

    override fun setBlockMetadataWithNotify(
        x: Int,
        y: Int,
        z: Int,
        meta: Int,
        flags: Int,
    ): Boolean {
        val position = ScenePosition(
            x = x,
            y = y,
            z = z,
        )
        if (!blocks.containsKey(position)) {
            return false
        }
        metadata[position] = meta
        tiles[position]?.updateContainingBlockInfo()
        return true
    }

    override fun getTileEntity(
        x: Int,
        y: Int,
        z: Int,
    ): TileEntity? {
        return tiles[ScenePosition(
            x = x,
            y = y,
            z = z,
        )]
    }

    override fun setTileEntity(
        x: Int,
        y: Int,
        z: Int,
        tile: TileEntity,
    ) {
        removeTileEntity(
            x = x,
            y = y,
            z = z,
        )
        tile.setWorldObj(this)
        tile.xCoord = x
        tile.yCoord = y
        tile.zCoord = z
        tile.validate()
        tiles[ScenePosition(
            x = x,
            y = y,
            z = z,
        )] = tile
    }

    override fun removeTileEntity(
        x: Int,
        y: Int,
        z: Int,
    ) {
        tiles.remove(ScenePosition(
            x = x,
            y = y,
            z = z,
        ))?.invalidate()
    }

    override fun getLightBrightnessForSkyBlocks(
        x: Int,
        y: Int,
        z: Int,
        minimum: Int,
    ): Int {
        return 0xF000F0
    }

    override fun updateEntities() {
    }

    internal fun positions(): List<ScenePosition> {
        return blocks.keys.toList()
    }

    internal fun clear() {
        tiles.values.forEach {
            it.invalidate()
        }
        tiles.clear()
        metadata.clear()
        blocks.clear()
    }
}
