package space.impact.iblueprints.integration.gregtech

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import gregtech.api.GregTech_API
import gregtech.api.interfaces.tileentity.IGregTechTileEntity
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase
import gregtech.api.util.GTStructureChannels
import gregtech.api.util.GTStructureUtility
import java.util.function.Function
import java.util.function.IntUnaryOperator
import java.util.function.Supplier
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.oredict.OreDictionary
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.api.SceneChannel
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.integration.structures.PreviewItemSource
import space.impact.iblueprints.integration.structures.PreviewPlayer
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.integration.structures.StructurePreviewCollector
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.ScenePosition
import space.impact.structures.StructureEvent
import space.impact.structures.alignment.constructable.ChannelDataAccessor
import space.impact.structures.alignment.constructable.IConstructable
import space.impact.structures.alignment.constructable.ISurvivalConstructable
import space.impact.structures.alignment.enumerable.ExtendedFacing
import space.impact.structures.structure.ISurvivalBuildEnvironment

/** Loaded only after both optional mods have completed initialization. */
internal object GregTechScenes {

    internal fun isHatch(stack: ItemStack): Boolean {
        return GTStructureUtility.getMetaTileEntity(stack) is GT_MetaTileEntity_Hatch
    }

    internal fun refreshHatchTexture(hatch: GT_MetaTileEntity_Hatch) {
        // GT can restore the server flag from NBT even in a client scene.
        val textureId = (hatch.texturePage.toInt() shl 7) or (hatch.updateData.toInt() and 127)
        hatch.updateTexture(textureId)
    }

    private val metatileCandidates: List<ItemStack> by lazy {
        val stacks = mutableListOf<ItemStack>()
        for (prototype in GregTech_API.METATILEENTITIES) {
            if (prototype != null) {
                try {
                    stacks.add(prototype.getStackForm(1))
                } catch (failure: LinkageError) {
                    LogManager.getLogger("ImpactBlueprints").warn("Cannot read metatile item ${prototype.javaClass.name}: $failure")
                }
            }
        }
        stacks
    }

    private val constructionCandidates: List<ItemStack> by lazy {
        val stacks = metatileCandidates.toMutableList()
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

    private val alternativeCandidates: List<ItemStack> by lazy {
        val stacks = metatileCandidates.toMutableList()
        for (block in Block.blockRegistry) {
            if (block === GregTech_API.sBlockMachines) {
                continue
            }
            val item = Item.getItemFromBlock(block as Block) ?: continue
            val variants = mutableListOf<ItemStack>()
            try {
                item.getSubItems(
                    item,
                    item.creativeTab ?: CreativeTabs.tabAllSearch,
                    variants,
                )
            } catch (failure: Throwable) {
                if (failure !is Exception && failure !is LinkageError) {
                    throw failure
                }
                LogManager.getLogger("ImpactBlueprints").warn(
                    "Cannot read block variants for ${block.unlocalizedName}: $failure",
                )
            }
            val concrete = variants.filter {
                it.item === item && it.itemDamage != OreDictionary.WILDCARD_VALUE
            }
            stacks.addAll(concrete.ifEmpty {
                listOf(ItemStack(item))
            })
        }
        stacks
    }

    internal fun register() {
        // Forge 1.7.10 does not synchronize listener registration with event dispatch.
        MinecraftForge.EVENT_BUS.register(this)
        var count = 0
        for ((
            id,
            prototype,
        ) in GregTech_API.METATILEENTITIES.withIndex()) {
            if (prototype !is IConstructable) {
                continue
            }
            val controller: ItemStack
            val sceneChannels: List<SceneChannel>
            try {
                controller = prototype.getStackForm(1)
                sceneChannels = channels(prototype)
            } catch (failure: LinkageError) {
                LogManager.getLogger("ImpactBlueprints").warn("Cannot register metatile $id: $failure")
                continue
            }
            SceneRegistry.register(
                SceneEntry(
                    id = "iblueprints:metatile_$id",
                    item = controller,
                    factory = Supplier {
                        create(
                            controller = controller,
                            values = emptyMap(),
                        )
                    },
                    channels = sceneChannels,
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
        LogManager.getLogger("ImpactBlueprints").info("Registered $count multiblock scenes from the metatile registry")
    }

    @SubscribeEvent
    public fun onPreviewElement(event: StructureEvent.StructureElementVisitedEvent) {
        (event.instrumentIdentifier as? StructurePreviewCollector)?.onElement(event)
    }

    private fun channels(constructable: IConstructable): List<SceneChannel> {
        val machine = constructable as? GT_MetaTileEntity_EnhancedMultiBlockBase<*>
        val definitions = machine?.structureDefinition?.channelDefinitions.orEmpty()
        // Keep the preview's hatch opt-in in the channel values saved to the projector.
        return definitions.filterNot {
            it.channel == GTStructureChannels.HATCH
        }.map { channel ->
            SceneChannel(
                id = channel.channel,
                label = GTStructureChannels.getLabel(channel.channel),
                description = GTStructureChannels.getDescription(channel.channel),
                minimum = channel.minimumValue,
                maximum = channel.maximumValue,
                normalizer = IntUnaryOperator(channel::normalizeValue),
            )
        } + SceneChannel(
            id = GTStructureChannels.HATCH,
            label = GTStructureChannels.getLabel(GTStructureChannels.HATCH),
            description = GTStructureChannels.getDescription(GTStructureChannels.HATCH),
            minimum = 1,
            maximum = 1,
            normalizer = IntUnaryOperator {
                1
            },
        )
    }

    internal fun create(
        controller: ItemStack,
        values: Map<String, Int>,
        savedPlan: NBTTagCompound? = null,
    ): BlockScene {
        val scene = BlockScene(controller.displayName)
        try {
            // ImpactStructures construction deliberately runs on a server-side world facade.
            scene.world.isRemote = false
            val id = controller.itemDamage
            val prototype = requireNotNull(GregTech_API.METATILEENTITIES[id])
            val origin = ScenePosition(
                x = 0,
                y = 64,
                z = 0,
            )
            scene.block(
                position = origin,
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
            if (savedPlan != null) {
                restoreBlocks(
                    scene = scene,
                    origin = origin,
                    plan = savedPlan,
                )
            }
            val source = PreviewItemSource(constructionCandidates)
            val actor = PreviewPlayer(scene.world)
            val collector = StructurePreviewCollector(
                scene = scene,
                context = constructable,
                trigger = trigger,
                actor = actor,
                candidates = alternativeCandidates,
            )
            collector.collect(constructing = savedPlan == null) {
                if (savedPlan == null) {
                    // Creative construction chooses the casing wherever a casing is allowed.
                    constructable.construct(
                        trigger,
                        false,
                    )
                } else {
                    // The live checker detects variable dimensions from the saved blocks themselves.
                    (constructable as GT_MetaTileEntity_MultiBlockBase).checkMachine(
                        tile,
                        tile.getStackInSlot(1),
                    )
                }
                // Only positions still requiring a block can request a dedicated hatch.
                if (savedPlan == null && constructable is ISurvivalConstructable) {
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
                    scene.diagnostics.add("iblueprints.structure_incomplete")
                }
                val hatchChoices = GregTechHatchChoices(
                    scene = scene,
                    origin = origin,
                    trigger = trigger,
                )
                collector.finish(hatchChoices::track)
                hatchChoices.initializeHatchTextures()
                scene.simulation = hatchChoices
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
                refreshHatchTexture(hatch)
            }
            return scene
        } catch (failure: Throwable) {
            scene.close()
            throw failure
        }
    }

    private fun restoreBlocks(
        scene: BlockScene,
        origin: ScenePosition,
        plan: NBTTagCompound,
    ) {
        require(SceneBlueprint.isValid(plan))
        for (placement in SceneBlueprint.placements(
            plan = plan,
            origin = origin,
            facing = ExtendedFacing.of(ForgeDirection.SOUTH),
        )) {
            val (position, stack, direction) = placement
            val item = stack.item as? ItemBlock ?: error("Blueprint contains a non-block item")
            val block = Block.getBlockFromItem(item)
            val prototype = if (block === GregTech_API.sBlockMachines) {
                GregTech_API.METATILEENTITIES.getOrNull(stack.itemDamage)
                    ?: error("Unknown metatile ${stack.itemDamage}")
            } else {
                null
            }
            scene.block(
                position = position,
                block = block,
                metadata = prototype?.tileEntityBaseType?.toInt() ?: item.getMetadata(stack.itemDamage),
            )
            if (prototype != null) {
                val tile = scene.world.getTileEntity(
                    x = position.x,
                    y = position.y,
                    z = position.z,
                ) as IGregTechTileEntity
                tile.setInitialValuesAsNBT(
                    null,
                    stack.itemDamage.toShort(),
                )
                if (direction != null) {
                    scene.world.isRemote = true
                    try {
                        BlueprintHatch.orient(
                            tile = tile as TileEntity,
                            direction = direction,
                        )
                    } finally {
                        scene.world.isRemote = false
                    }
                }
            }
        }
    }
}
