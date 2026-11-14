package space.impact.iblueprints.integration.structures

import java.util.concurrent.ConcurrentHashMap
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.SceneElement
import space.impact.iblueprints.scene.ScenePosition
import space.impact.structures.StructureEvent
import space.impact.structures.structure.AutoPlaceEnvironment
import space.impact.structures.structure.IStructureElement
import space.impact.structures.structure.IStructureElementChain
import space.impact.structures.structure.StructureUtility

internal class StructurePreviewCollector(
    private val scene: BlockScene,
    private val context: Any,
    private val trigger: ItemStack,
    private val actor: PreviewPlayer,
    private val candidates: List<ItemStack>,
) {

    private val elements: MutableMap<ScenePosition, IStructureElement<Any>> = linkedMapOf()
    // With no hatches in stock, hatch-or-casing elements expose their casing alternatives.
    private val placementEnvironment = AutoPlaceEnvironment.fromLegacy(
        PreviewItemSource(emptyList()),
        actor,
        actor::addChatMessage,
    )

    internal fun onElement(event: StructureEvent.StructureElementVisitedEvent) {
        if (event.world !== scene.world) {
            return
        }
        val position = ScenePosition(
            x = event.x,
            y = event.y,
            z = event.z,
        )
        if (elements.containsKey(position)) {
            return
        }
        @Suppress("UNCHECKED_CAST")
        val element = event.element as IStructureElement<Any>
        elements[position] = element
    }

    internal fun hasUnfilledPositions(): Boolean {
        return elements.any { (position, element) ->
            scene.world.isAirBlock(
                position.x,
                position.y,
                position.z,
            ) && !element.couldBeValid(
                context,
                scene.world,
                position.x,
                position.y,
                position.z,
                trigger,
            )
        }
    }

    internal fun finish(hatchChoices: (ScenePosition, IStructureElement<Any>, SceneElement) -> (() -> List<ItemStack>)) {
        for ((
            position,
            element,
        ) in elements) {
            val air = scene.world.isAirBlock(
                position.x,
                position.y,
                position.z,
            )
            val requiredAir = element === StructureUtility.isAir<Any>()
            val snapshot = SceneElement(
                requiredAir = requiredAir,
                unresolved = air && !requiredAir,
                choices = listOfNotNull(scene.item(position)),
                channels = element.channels.toList(),
            )
            if (!requiredAir) {
                snapshot.placementChoices = {
                    try {
                        placementChoices(
                            position = position,
                            element = element,
                        )
                    } catch (failure: LinkageError) {
                        snapshot.hatchChoicesIncomplete = true
                        if (unavailablePlacementClasses.add(failure.toString())) {
                            LogManager.getLogger("ImpactBlueprints").warn("Cannot read some placement alternatives: $failure")
                        }
                        emptyList()
                    }
                }
                snapshot.hatchChoices = hatchChoices(
                    position,
                    element,
                    snapshot,
                )
            }
            scene.elements[position] = snapshot
        }
        scene.diagnostics.addAll(actor.messages)
    }

    private fun placementChoices(
        position: ScenePosition,
        element: IStructureElement<Any>,
    ): List<ItemStack> {
        if (element is IStructureElementChain<Any>) {
            return element.fallbacks().flatMap {
                placementChoices(
                    position = position,
                    element = it,
                )
            }
        }
        val placement = element.getBlocksToPlace(
            context,
            scene.world,
            position.x,
            position.y,
            position.z,
            trigger,
            placementEnvironment,
        ) ?: return emptyList()
        val stacks = placement.stacks?.filter {
            it.item != null
        } ?: return candidates.filter(placement.predicate::test)
        return stacks.flatMap { stack ->
            if (stack.itemDamage != OreDictionary.WILDCARD_VALUE) {
                listOf(stack)
            } else {
                // Impact Structures' explicit-stack predicate compares the raw damage value.
                candidates.filter {
                    it.item === stack.item
                }.map { variant ->
                    if (stack.hasTagCompound()) {
                        stack.copy().apply {
                            itemDamage = variant.itemDamage
                        }
                    } else {
                        variant
                    }
                }
            }
        }
    }

    private companion object {
        val unavailablePlacementClasses = ConcurrentHashMap.newKeySet<String>()
    }
}
