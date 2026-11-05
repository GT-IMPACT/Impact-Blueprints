package space.impact.gui3dengine.integration.nei

import codechicken.nei.api.API
import codechicken.nei.api.IConfigureNEI
import space.impact.gui3dengine.MODNAME
import space.impact.gui3dengine.VERSION

/** NEI discovers this plugin itself. The base mod never loads NEI classes. */
public class NEIGui3DEngineConfig : IConfigureNEI {

    override fun loadConfig() {
        val handler = SceneRecipeHandler()
        API.registerRecipeHandler(handler)
        API.registerUsageHandler(handler)
    }

    override fun getName(): String {
        return MODNAME
    }

    override fun getVersion(): String {
        return VERSION
    }
}
