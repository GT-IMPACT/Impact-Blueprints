package space.impact.iblueprints.integration.ae2

import appeng.api.storage.ITerminalHost
import appeng.container.ContainerOpenContext
import appeng.container.PrimaryGui
import net.minecraft.entity.player.EntityPlayer
import space.impact.iblueprints.planner.PlannerState

internal interface IMeBlueprintPlannerHost : ITerminalHost {

    val planner: PlannerState

    fun markPlannerDirty()

    fun accessible(player: EntityPlayer): Boolean

    fun plannerContext(): ContainerOpenContext

    fun plannerPrimaryGui(): PrimaryGui
}
