package space.impact.iblueprints.scene.world

import net.minecraft.block.Block
import net.minecraft.client.multiplayer.ChunkProviderClient
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.profiler.Profiler
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraft.world.WorldProviderSurface
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType
import net.minecraft.world.chunk.IChunkProvider
import net.minecraft.world.storage.SaveHandlerMP
import space.impact.iblueprints.planner.logPlannerTraceFailure
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
    private var visiblePositions: Set<ScenePosition>? = null

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
        val position = ScenePosition(
            x = x,
            y = y,
            z = z,
        )
        return if (visiblePositions?.contains(position) == false) {
            Blocks.air
        } else {
            blocks[position] ?: Blocks.air
        }
    }

    override fun getBlockMetadata(
        x: Int,
        y: Int,
        z: Int,
    ): Int {
        val position = ScenePosition(
            x = x,
            y = y,
            z = z,
        )
        return if (visiblePositions?.contains(position) == false) {
            0
        } else {
            metadata[position] ?: 0
        }
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
        val position = ScenePosition(
            x = x,
            y = y,
            z = z,
        )
        return if (visiblePositions?.contains(position) == false) {
            null
        } else {
            tiles[position]
        }
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
        val tile = tiles.remove(ScenePosition(
            x = x,
            y = y,
            z = z,
        )) ?: return
        invalidatePreviewTile(tile)
    }

    private fun invalidatePreviewTile(tile: TileEntity) {
        // Structure checks can simulate the server side. Cleanup must not run live server hooks.
        val previousSide = isRemote
        isRemote = true
        try {
            tile.invalidate()
        } catch (failure: Throwable) {
            logPlannerTraceFailure(
                message = "scene.invalidateTile failed tile=${tile.javaClass.name} " +
                    "position=${tile.xCoord},${tile.yCoord},${tile.zCoord} previousRemote=$previousSide",
                failure = failure,
            )
            throw failure
        } finally {
            isRemote = previousSide
        }
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

    internal fun withVisiblePositions(
        positions: Set<ScenePosition>,
        draw: () -> Unit,
    ) {
        val previous = visiblePositions
        visiblePositions = positions
        try {
            draw()
        } finally {
            visiblePositions = previous
        }
    }

    internal fun positions(): List<ScenePosition> {
        return blocks.keys.toList()
    }

    internal fun blockState(position: ScenePosition): SceneBlockState? {
        val block = blocks[position] ?: return null
        val tileData = tiles[position]?.let { tile ->
            NBTTagCompound().also(tile::writeToNBT)
        }
        return SceneBlockState(
            block = block,
            metadata = metadata.getValue(position),
            tileData = tileData,
        )
    }

    internal fun setBlockState(
        position: ScenePosition,
        state: SceneBlockState,
    ) {
        removeTileEntity(
            x = position.x,
            y = position.y,
            z = position.z,
        )
        blocks[position] = state.block
        metadata[position] = state.metadata
        val data = state.tileData ?: return
        checkNotNull(loadProbeTile(
            position = position,
            data = data,
        ))
    }

    internal fun copyForProbe(): SceneWorld {
        val copy = SceneWorld()
        copy.blocks.putAll(blocks)
        copy.metadata.putAll(metadata)
        for ((position, tile) in tiles) {
            val data = NBTTagCompound()
            tile.writeToNBT(data)
            copy.loadProbeTile(
                position = position,
                data = data,
            )
        }
        return copy
    }

    internal fun loadProbeTile(
        position: ScenePosition,
        data: NBTTagCompound,
    ): TileEntity? {
        val block = blocks[position] ?: return null
        val tile = block.createTileEntity(
            this,
            metadata[position] ?: 0,
        ) ?: return null
        setTileEntity(
            x = position.x,
            y = position.y,
            z = position.z,
            tile = tile,
        )
        val tag = data.copy() as NBTTagCompound
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
        return tile
    }

    internal fun clear() {
        tiles.values.forEach(::invalidatePreviewTile)
        tiles.clear()
        metadata.clear()
        blocks.clear()
    }
}
