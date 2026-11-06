package space.impact.iblueprints.integration.modularui

import com.cleanroommc.modularui.screen.GuiContainerWrapper
import com.cleanroommc.modularui.screen.ModularContainer
import com.cleanroommc.modularui.screen.ModularScreen
import net.minecraft.inventory.Container

internal class SceneViewerGui(
    container: ModularContainer,
    screen: ModularScreen,
    private val parentContainer: Container,
) : GuiContainerWrapper(
    container,
    screen,
) {

    override fun initGui() {
        val player = mc.thePlayer
        super.initGui()
        // NEI recipes replace openContainer with EmptyContainer; restore the original planner on return.
        player.openContainer = parentContainer
    }
}
