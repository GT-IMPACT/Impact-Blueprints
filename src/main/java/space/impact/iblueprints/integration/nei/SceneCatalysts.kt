package space.impact.iblueprints.integration.nei

import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.scroll.ScrollBar
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.yield
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.scene.SceneMaterial
import space.impact.iblueprints.scene.ScenePosition

/** NEI's global catalyst table is per category. This changes only the current screen's native panel. */
internal object SceneCatalysts {

    private val horizontalScroll = ScrollBar.defaultHorizontalBar().setOverflowType(ScrollBar.OverflowType.AUTO)
    private val indexScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var indexedEntries: List<SceneEntry> = emptyList()

    @Volatile
    private var indexTask: Deferred<Unit>? = null

    @Volatile
    private var materialsByScene: Map<SceneEntry, List<SceneMaterialUsage>> = emptyMap()

    @Synchronized
    internal fun startIndexing(): Deferred<Unit> {
        val entries = SceneRegistry.all()
        indexTask?.let {
            if (!it.isCompleted || indexedEntries == entries && !it.isCancelled) {
                return it
            }
        }
        indexedEntries = entries
        val task = indexScope.async {
            try {
                index(entries)
                LogManager.getLogger("ImpactBlueprints").info("Finished indexing NEI uses for ${entries.size} scenes")
            } catch (failure: Throwable) {
                LogManager.getLogger("ImpactBlueprints").error(
                    "Cannot finish the structure usage index",
                    failure,
                )
                throw failure
            }
        }
        indexTask = task
        return task
    }

    private suspend fun index(entries: List<SceneEntry>) {
        // Publish the default blocks for every scene before probing alternatives.
        for (entry in entries) {
            indexConfigurations(
                entry = entry,
                includeAlternatives = false,
            ).count()
            yield()
        }

        // Give each scene a turn before expanding the next channel combination.
        val configurations = entries.map {
            indexConfigurations(
                entry = it,
                includeAlternatives = true,
            ).iterator()
        }.toMutableList()
        while (configurations.isNotEmpty()) {
            val iterator = configurations.iterator()
            while (iterator.hasNext()) {
                val sceneConfigurations = iterator.next()
                if (sceneConfigurations.hasNext()) {
                    sceneConfigurations.next()
                    yield()
                } else {
                    iterator.remove()
                }
            }
        }
    }

    private fun indexConfigurations(
        entry: SceneEntry,
        includeAlternatives: Boolean,
    ): Sequence<Unit> {
        return sequence {
            val controller = entry.item()
            val materials = materialsByScene[entry].orEmpty().toMutableList()
            val values = entry.channels.associateTo(linkedMapOf()) {
                it.id to it.normalize(it.minimum)
            }

            suspend fun SequenceScope<Unit>.indexConfiguration(channelIndex: Int) {
                if (includeAlternatives && channelIndex < entry.channels.size) {
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
                            val stacks = listOfNotNull(scene.item(position)) + if (includeAlternatives) {
                                scene.elements[position]?.inspectionAlternatives().orEmpty()
                            } else {
                                emptyList()
                            }
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
                        publishMaterials(
                            entry = entry,
                            materials = materials,
                        )
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
        }
    }

    internal fun indexPreview(
        entry: SceneEntry,
        materials: List<SceneMaterial>,
        values: Map<String, Int>,
    ) {
        val controller = entry.item()
        val configuration = entry.channels.associate {
            it.id to it.normalize(values[it.id] ?: it.minimum)
        }
        publishMaterials(
            entry = entry,
            materials = materials.filterNot {
                sameStack(
                    first = controller,
                    second = it.stack,
                )
            }.map {
                SceneMaterialUsage(
                    stack = it.stack.copy(),
                    values = configuration,
                    positions = it.positions.toSet(),
                )
            },
        )
    }

    @Synchronized
    private fun publishMaterials(
        entry: SceneEntry,
        materials: List<SceneMaterialUsage>,
    ) {
        val existing = materialsByScene[entry].orEmpty()
        val additions = materials.filter { candidate ->
            existing.none {
                sameStack(
                    first = it.stack,
                    second = candidate.stack,
                )
            }
        }
        materialsByScene = materialsByScene + (entry to existing + additions)
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
        return materialsByScene.filterValues { usages ->
            usages.any {
                sameStack(
                    first = it.stack,
                    second = stack,
                )
            }
        }.keys.toList()
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
        val minecraft = Minecraft.getMinecraft()
        if (indexTask == null && minecraft.theWorld != null) {
            startIndexing()
        }
        val gui = minecraft.currentScreen as? GuiRecipe<*> ?: return
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
