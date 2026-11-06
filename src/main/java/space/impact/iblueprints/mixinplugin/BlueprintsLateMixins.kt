package space.impact.iblueprints.mixinplugin

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader
import com.gtnewhorizon.gtnhmixins.LateMixin
import com.gtnewhorizon.gtnhmixins.builders.IMixins

@LateMixin
public class BlueprintsLateMixins : ILateMixinLoader {

    override fun getMixinConfig(): String {
        return "mixins.iblueprints.late.json"
    }

    override fun getMixins(loadedMods: Set<String>): List<String> {
        return IMixins.getLateMixins(
            Mixins::class.java,
            loadedMods,
        )
    }
}
