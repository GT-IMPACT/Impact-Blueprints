package space.impact.iblueprints.integration.nei

import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.scroll.ScrollBar
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.scene.ScenePosition

/** NEI's global catalyst table is per category. This changes only the current screen's native panel. */
internal object SceneCatalysts {

    private val horizontalScroll = ScrollBar.defaultHorizontalBar().setOverflowType(ScrollBar.OverflowType.AUTO)
    private const val INDEX_BUDGET_NANOS = 2_000_000L
    private var pendingIndex: Iterator<Unit>? = null

    @Volatile
    private var materialsByScene: Map<SceneEntry, List<SceneMaterialUsage>> = emptyMap()

    internal fun index() {
        if (pendingIndex != null) {
            return
        }
        val entries = SceneRegistry.all()
        materialsByScene = emptyMap()
        pendingIndex = sequence {
            for (entry in entries) {
                indexMaterials(entry)
                yield(Unit)
            }
        }.iterator()
    }

    internal fun advanceIndex(): Boolean {
        val pending = pendingIndex ?: return false
        val start = System.nanoTime()
        do {
            if (!pending.hasNext()) {
                pendingIndex = null
                return false
            }
            pending.next()
        } while (System.nanoTime() - start < INDEX_BUDGET_NANOS)
        return true
    }

    // Resume on the client thread and publish immutable results for NEI's query workers.
    private suspend fun SequenceScope<Unit>.indexMaterials(entry: SceneEntry) {
        val controller = entry.item()
        val materials = mutableListOf<SceneMaterialUsage>()
        val values = linkedMapOf<String, Int>()

        suspend fun SequenceScope<Unit>.indexConfiguration(channelIndex: Int) {
            if (channelIndex < entry.channels.size) {
                val channel = entry.channels[channelIndex]
                for (value in (channel.minimum..channel.maximum).asSequence().map(channel::normalize).distinct()) {
                    values[channel.id] = value
                    indexConfiguration(channelIndex + 1)
                }
                return
            }
            try {
                entry.build(values).use { scene ->
                    val positionsByStack = linkedMapOf<ItemStack, MutableSet<ScenePosition>>()
                    for (position in scene.positions().toSet() + scene.elements.keys) {
                        yield(Unit)
                        val stacks = listOfNotNull(scene.item(position)) +
                            scene.elements[position]?.inspectionAlternatives().orEmpty()
                        for (stack in stacks) {
                            if (sameStack(
                                    first = controller,
                                    second = stack,
                                ) || materials.any {
                                    sameStack(
                                        first = it.stack,
                                        second = stack,
                                    )
                                }
                            ) {
                                continue
                            }
                            val key = positionsByStack.keys.firstOrNull {
                                sameStack(
                                    first = it,
                                    second = stack,
                                )
                            } ?: stack
                            positionsByStack.getOrPut(key) {
                                linkedSetOf()
                            }.add(position)
                        }
                    }
                    val configuration = values.toMap()
                    for ((stack, positions) in positionsByStack) {
                        materials.add(SceneMaterialUsage(
                            stack = stack,
                            values = configuration,
                            positions = positions.toSet(),
                        ))
                    }
                }
            } catch (failure: Throwable) {
                if (failure !is Exception && failure !is LinkageError) {
                    throw failure
                }
                LogManager.getLogger("ImpactBlueprints").warn(
                    "Cannot index structural blocks for ${entry.id} with $values: $failure",
                )
            }
            yield(Unit)
        }
        try {
            indexConfiguration(0)
        } catch (failure: LinkageError) {
            LogManager.getLogger("ImpactBlueprints").warn("Cannot enumerate channels for ${entry.id}: $failure")
        }
        materialsByScene = materialsByScene + (entry to materials.toList())
    }

    private fun sameStack(
        first: ItemStack,
        second: ItemStack,
    ): Boolean {
        return first.isItemEqual(second) && ItemStack.areItemStackTagsEqual(
            first,
            second,
        )
    }

    internal fun matching(stack: ItemStack): List<SceneEntry> {
        return materialsByScene.keys.filter {
            usage(
                entry = it,
                stack = stack,
            ) != null
        }
    }

    internal fun usage(
        entry: SceneEntry,
        stack: ItemStack,
    ): SceneMaterialUsage? {
        return materialsByScene[entry]?.firstOrNull {
            sameStack(
                first = it.stack,
                second = stack,
            )
        }
    }

    @SubscribeEvent
    public fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) {
            return
        }
        advanceIndex()
        val gui = Minecraft.getMinecraft().currentScreen as? GuiRecipe<*> ?: return
        if (gui.handler !is SceneRecipeHandler) {
            return
        }
        // GuiRecipe.updateScreen first resets this panel from its category-wide table.
        // Restore the visible recipe after that reset, before the next input/render cycle.
        update(
            gui = gui,
            recipe = gui.recipeIndices.firstOrNull(),
        )
    }

    internal fun update(
        gui: GuiRecipe<*>,
        recipe: Int?,
    ) {
        val handler = gui.handler as? SceneRecipeHandler ?: return
        val catalysts = recipe?.let(handler::catalysts).orEmpty()
        val panel = gui.recipeCatalystWidget
        val right = panel.x + panel.w
        val wasScrollable = panel.horizontalScroll === horizontalScroll
        val previousScroll = panel.horizontalScrollOffset
        // NEI keeps the old child widgets when setCatalysts receives an empty list.
        if (catalysts.isEmpty()) {
            panel.clear()
        }
        panel.setPaddingBlock(
            7,
            7,
        )
        panel.setCatalysts(catalysts)
        val gridOverflows = panel.columnCount > 1 && panel.w > right && right >= 26
        if (gridOverflows) {
            val fullWidth = panel.w
            panel.w = ((right - 10) / 16) * 16 + 10
            panel.h += horizontalScroll.trackWidth
            panel.setPaddingBlock(
                7,
                7 + horizontalScroll.trackWidth,
            )
            panel.setHorizontalScroll(horizontalScroll)
            panel.x = right - panel.w
            panel.update()
            panel.horizontalScrollOffset = if (wasScrollable) {
                previousScroll
            } else {
                fullWidth - panel.w
            }
            return
        }
        panel.setHorizontalScroll(null)
        panel.x = right - panel.w
        panel.update()
    }
}
