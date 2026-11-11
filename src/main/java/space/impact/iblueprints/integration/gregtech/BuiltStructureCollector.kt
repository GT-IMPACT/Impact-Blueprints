package space.impact.iblueprints.integration.gregtech

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase
import java.util.ArrayDeque
import java.util.function.BooleanSupplier
import net.minecraft.world.World
import space.impact.iblueprints.scene.ScenePosition
import space.impact.structures.StructureEvent

internal class BuiltStructureCollector(
    internal val machine: GT_MetaTileEntity_EnhancedMultiBlockBase<*>,
    private val world: World,
) {

    internal val positions: MutableSet<ScenePosition> = linkedSetOf()
    private val pieces = ArrayDeque<MutableSet<ScenePosition>>()

    @SubscribeEvent
    public fun onElement(event: StructureEvent.StructureElementVisitedEvent) {
        if (event.instrumentIdentifier !== this || event.world !== world) {
            return
        }
        (pieces.peekLast() ?: positions).add(ScenePosition(
            x = event.x,
            y = event.y,
            z = event.z,
        ))
    }

    internal fun checkPiece(check: BooleanSupplier): Boolean {
        val visited = linkedSetOf<ScenePosition>()
        pieces.addLast(visited)
        val valid = try {
            check.asBoolean
        } finally {
            pieces.removeLast()
        }
        if (valid) {
            // Failed optional pieces can touch blocks outside the completed machine.
            (pieces.peekLast() ?: positions).addAll(visited)
        }
        return valid
    }
}
