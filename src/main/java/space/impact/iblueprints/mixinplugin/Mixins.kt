package space.impact.iblueprints.mixinplugin

import com.gtnewhorizon.gtnhmixins.builders.IBaseTransformer.Phase
import com.gtnewhorizon.gtnhmixins.builders.IMixins
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder

public enum class Mixins(private val mixinBuilder: MixinBuilder) : IMixins {

    WIRELESS_BLUEPRINT_PLANNER(
        MixinBuilder("Remember the blueprint planner mode in AE2FC wireless terminals")
            .setPhase(Phase.LATE)
            .addRequiredMod(TargetedMod.AE2FC)
            .addRequiredMod(TargetedMod.AE2)
            .addRequiredMod(TargetedMod.MODULAR_UI)
            .addRequiredMod(TargetedMod.IMPACT_API)
            .addCommonMixins(
                "ae2fc.ItemBaseWirelessTerminalMixin",
                "ae2fc.ItemWirelessUltraTerminalMixin",
            )
            .addClientMixins(
                "ae2fc.UltraTerminalButtonsMixin",
                "ae2fc.GuiMEMonitorableMixin",
            )
            .setApplyIf {
                true
            },
    );

    override fun getBuilder(): MixinBuilder {
        return mixinBuilder
    }
}
