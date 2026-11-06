package space.impact.iblueprints.planner

import com.cleanroommc.modularui.screen.ModularContainer

internal class PlannerContainer(sync: PlannerSync) : ModularContainer() {

    internal val planner = sync.session
}
