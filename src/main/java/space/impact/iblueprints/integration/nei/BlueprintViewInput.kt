package space.impact.iblueprints.integration.nei

import codechicken.nei.NEIClientConfig
import codechicken.nei.guihook.GuiContainerManager
import codechicken.nei.guihook.IContainerInputHandler
import cpw.mods.fml.common.Loader
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.util.ChatComponentTranslation
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.integration.modularui.SceneViewer
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.item.StructureBlueprintItem

internal object BlueprintViewInput : IContainerInputHandler {

    internal const val KEY = "gui.pattern_view"

    internal fun keyName(): String? {
        return NEIClientConfig.getKeyName(KEY)
    }

    override fun lastKeyTyped(
        gui: GuiContainer,
        keyChar: Char,
        keyID: Int,
    ): Boolean {
        if (!NEIClientConfig.isKeyHashDown(KEY) || !Loader.isModLoaded("modularui2")) {
            return false
        }
        val stack = GuiContainerManager.getStackMouseOver(gui) ?: return false
        if (stack.item !== StructureBlueprintItem) {
            return false
        }
        val plan = SceneBlueprint.fromItem(stack) ?: return false
        val controller = SceneBlueprint.controller(plan)
        val entry = controller?.let {
            SceneRegistry.find(
                id = stack.tagCompound.getString(StructureBlueprintItem.RECIPE_TAG),
                controller = it,
            )
        }
        if (entry == null || !SceneBlueprint.isValid(plan)) {
            gui.mc.thePlayer.addChatMessage(ChatComponentTranslation("iblueprints.blueprint.preview_unavailable"))
            return true
        }
        try {
            SceneViewer.open(
                entry = entry,
                initialValues = SceneBlueprint.channels(plan),
                highlighted = emptySet(),
                savedPlan = plan,
            )
        } catch (failure: Throwable) {
            if (failure !is Exception && failure !is LinkageError) {
                throw failure
            }
            LogManager.getLogger("ImpactBlueprints").warn(
                "Cannot preview blueprint ${entry.id}",
                failure,
            )
            gui.mc.thePlayer.addChatMessage(ChatComponentTranslation("iblueprints.blueprint.preview_unavailable"))
        }
        return true
    }

    override fun keyTyped(
        gui: GuiContainer,
        keyChar: Char,
        keyCode: Int,
    ): Boolean {
        return false
    }

    override fun onKeyTyped(
        gui: GuiContainer,
        keyChar: Char,
        keyID: Int,
    ) {
    }

    override fun mouseClicked(
        gui: GuiContainer,
        mousex: Int,
        mousey: Int,
        button: Int,
    ): Boolean {
        return false
    }

    override fun onMouseClicked(
        gui: GuiContainer,
        mousex: Int,
        mousey: Int,
        button: Int,
    ) {
    }

    override fun onMouseUp(
        gui: GuiContainer,
        mousex: Int,
        mousey: Int,
        button: Int,
    ) {
    }

    override fun mouseScrolled(
        gui: GuiContainer,
        mousex: Int,
        mousey: Int,
        scrolled: Int,
    ): Boolean {
        return false
    }

    override fun onMouseScrolled(
        gui: GuiContainer,
        mousex: Int,
        mousey: Int,
        scrolled: Int,
    ) {
    }

    override fun onMouseDragged(
        gui: GuiContainer,
        mousex: Int,
        mousey: Int,
        button: Int,
        heldTime: Long,
    ) {
    }
}
