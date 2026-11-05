package space.impact.gui3dengine.integration.structures

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import net.minecraft.item.ItemStack
import space.impact.gui3dengine.scene.BlockScene
import space.impact.gui3dengine.scene.SceneElement
import space.impact.gui3dengine.scene.ScenePosition
import space.impact.structures.StructureEvent
import space.impact.structures.structure.IStructureElement
import space.impact.structures.structure.StructureUtility

internal class StructurePreviewCollector(
    private val scene: BlockScene,
    private val context: Any,
    private val trigger: ItemStack,
    private val actor: PreviewPlayer,
) {

    private val elements: MutableMap<ScenePosition, IStructureElement<Any>> = linkedMapOf()

    @SubscribeEvent
    public fun onElement(event: StructureEvent.StructureElementVisitedEvent) {
        if (event.instrumentIdentifier !== this || event.world !== scene.world) {
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

    internal fun finish() {
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
            scene.elements[position] = SceneElement(
                requiredAir = requiredAir,
                unresolved = air && !requiredAir,
                choices = listOfNotNull(scene.item(position)),
                channels = element.channels.toList(),
            )
        }
        scene.diagnostics.addAll(actor.messages)
    }
}
