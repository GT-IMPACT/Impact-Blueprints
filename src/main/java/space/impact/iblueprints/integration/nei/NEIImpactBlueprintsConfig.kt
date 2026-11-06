package space.impact.iblueprints.integration.nei

import codechicken.nei.api.API
import codechicken.nei.api.IConfigureNEI
import codechicken.nei.guihook.GuiContainerManager
import org.lwjgl.input.Keyboard
import space.impact.iblueprints.MODNAME
import space.impact.iblueprints.VERSION

/** NEI discovers this plugin itself. The base mod never loads NEI classes. */
public class NEIImpactBlueprintsConfig : IConfigureNEI {

    override fun loadConfig() {
        val handler = SceneRecipeHandler()
        API.registerRecipeHandler(handler)
        API.registerUsageHandler(handler)
        API.addKeyBind(
            BlueprintViewInput.KEY,
            Keyboard.KEY_P,
        )
        // NEI's potion popup uses P too and stops the late input pass when it handles the key.
        if (!GuiContainerManager.inputHandlers.contains(BlueprintViewInput)) {
            GuiContainerManager.inputHandlers.addFirst(BlueprintViewInput)
        }
    }

    override fun getName(): String {
        return MODNAME
    }

    override fun getVersion(): String {
        return VERSION
    }
}
