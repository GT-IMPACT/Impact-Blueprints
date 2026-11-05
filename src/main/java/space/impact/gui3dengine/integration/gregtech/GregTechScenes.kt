package space.impact.gui3dengine.integration.gregtech

import gregtech.api.GregTech_API
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch
import gregtech.api.util.GTStructureChannels
import java.util.function.Function
import java.util.function.IntUnaryOperator
import java.util.function.Supplier
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import org.apache.logging.log4j.LogManager
import space.impact.gui3dengine.api.SceneChannel
import space.impact.gui3dengine.api.SceneEntry
import space.impact.gui3dengine.api.SceneRegistry
import space.impact.gui3dengine.integration.structures.PreviewItemSource
import space.impact.gui3dengine.integration.structures.PreviewPlayer
import space.impact.gui3dengine.integration.structures.StructurePreviewCollector
import space.impact.gui3dengine.scene.BlockScene
import space.impact.gui3dengine.scene.ScenePosition
import space.impact.structures.ImpactStructures
import space.impact.structures.alignment.constructable.ChannelDataAccessor
import space.impact.structures.alignment.constructable.IConstructable
import space.impact.structures.alignment.constructable.ISurvivalConstructable
import space.impact.structures.structure.ISurvivalBuildEnvironment

/** Loaded only after both optional mods have completed initialization. */
internal object GregTechScenes {

    private val candidates: List<ItemStack> by lazy {
        val stacks = mutableListOf<ItemStack>()
        for (prototype in GregTech_API.METATILEENTITIES) {
            if (prototype != null) {
                stacks.add(prototype.getStackForm(1))
            }
        }
        for (block in Block.blockRegistry) {
            val item = Item.getItemFromBlock(block as Block) ?: continue
            for (metadata in 0..15) {
                stacks.add(ItemStack(
                    item,
                    1,
                    metadata,
                ))
            }
        }
        stacks
    }

    internal fun register() {
        var count = 0
        for ((
            id,
            prototype,
        ) in GregTech_API.METATILEENTITIES.withIndex()) {
            if (prototype !is IConstructable) {
                continue
            }
            val controller = prototype.getStackForm(1)
            SceneRegistry.register(
                SceneEntry(
                    id = "gui3dengine:metatile_$id",
                    item = controller,
                    factory = Supplier {
                        create(
                            controller = controller,
                            values = emptyMap(),
                        )
                    },
                    channels = channels(prototype),
                    configuredFactory = Function {
                        create(
                            controller = controller,
                            values = it,
                        )
                    },
                ),
            )
            count++
        }
        LogManager.getLogger("Gui3dEngine").info("Registered $count multiblock scenes from the metatile registry")
    }

    private fun channels(constructable: IConstructable): List<SceneChannel> {
        val machine = constructable as? GT_MetaTileEntity_EnhancedMultiBlockBase<*> ?: return emptyList()
        return machine.structureDefinition.channelDefinitions.map { channel ->
            SceneChannel(
                id = channel.channel,
                label = GTStructureChannels.getLabel(channel.channel),
                description = GTStructureChannels.getDescription(channel.channel),
                minimum = channel.minimumValue,
                maximum = channel.maximumValue,
                normalizer = IntUnaryOperator(channel::normalizeValue),
            )
        }
    }

    internal fun create(
        controller: ItemStack,
        values: Map<String, Int>,
    ): BlockScene {
        val scene = BlockScene(controller.displayName)
        try {
            // ImpactStructures construction deliberately runs on a server-side world facade.
            scene.world.isRemote = false
            val id = controller.itemDamage
            val prototype = requireNotNull(GregTech_API.METATILEENTITIES[id])
            scene.block(
                position = ScenePosition(
                    x = 0,
                    y = 64,
                    z = 0,
                ),
                block = GregTech_API.sBlockMachines,
                metadata = prototype.tileEntityBaseType.toInt(),
            )
            val tile = scene.world.getTileEntity(
                x = 0,
                y = 64,
                z = 0,
            ) as IGregTechTileEntity
            tile.setInitialValuesAsNBT(
                null,
                id.toShort(),
            )
            // Alignment changes must not broadcast packets from the isolated preview.
            scene.world.isRemote = true
            tile.frontFacing = 3.toByte()
            scene.world.isRemote = false
            val constructable = tile.metaTileEntity as IConstructable
            val trigger = controller.copy()
            trigger.stackSize = 1
            for (channel in channels(constructable)) {
                ChannelDataAccessor.setChannelData(
                    trigger,
                    channel.id,
                    channel.normalize(values[channel.id] ?: channel.minimum),
                )
            }
            // Hatch placement is opt-in in GT even when its only channel value is 1.
            ChannelDataAccessor.setChannelData(
                trigger,
                GTStructureChannels.HATCH,
                1,
            )
            val source = PreviewItemSource(candidates)
            val actor = PreviewPlayer(scene.world)
            val collector = StructurePreviewCollector(
                scene = scene,
                context = constructable,
                trigger = trigger,
                actor = actor,
            )
            MinecraftForge.EVENT_BUS.register(collector)
            try {
                ImpactStructures.enableInstrument(collector)
                try {
                    // Creative construction chooses the casing wherever a casing is allowed.
                    constructable.construct(
                        trigger,
                        false,
                    )
                    // Only positions still requiring a block can request a dedicated hatch.
                    if (constructable is ISurvivalConstructable) {
                        val environment = ISurvivalBuildEnvironment.create(
                            source,
                            actor,
                        )
                        for (pass in 0 until 4096) {
                            if (!collector.hasUnfilledPositions()) {
                                break
                            }
                            val built = constructable.survivalConstruct(
                                trigger,
                                4096,
                                environment,
                            )
                            if (built <= 0) {
                                break
                            }
                        }
                    }
                    if (collector.hasUnfilledPositions()) {
                        scene.diagnostics.add("gui3dengine.structure_incomplete")
                    }
                    collector.finish()
                } finally {
                    ImpactStructures.disableInstrument()
                }
            } finally {
                MinecraftForge.EVENT_BUS.unregister(collector)
            }
            check(scene.positions().size > 1 || scene.elements.isNotEmpty()) {
                "The controller did not produce a preview structure"
            }
            scene.world.isRemote = true
            for (position in scene.positions()) {
                val hatchTile = scene.world.getTileEntity(
                    x = position.x,
                    y = position.y,
                    z = position.z,
                ) as? IGregTechTileEntity ?: continue
                val hatch = hatchTile.metaTileEntity as? GT_MetaTileEntity_Hatch ?: continue
                // GT packs a server-only flag into the texture index for nonzero pages.
                // Apply its client update path after construction, preserving the assigned casing page.
                val textureId = (hatch.texturePage.toInt() shl 7) or (hatch.updateData.toInt() and 127)
                hatch.updateTexture(textureId)
            }
            return scene
        } catch (failure: Exception) {
            scene.close()
            throw failure
        }
    }
}
