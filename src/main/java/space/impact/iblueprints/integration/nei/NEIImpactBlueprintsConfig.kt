package space.impact.iblueprints.integration.nei

import codechicken.nei.api.API
import codechicken.nei.api.IConfigureNEI
import space.impact.iblueprints.MODNAME
import space.impact.iblueprints.VERSION

/** NEI discovers this plugin itself. The base mod never loads NEI classes. */
public class NEIImpactBlueprintsConfig : IConfigureNEI {

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
