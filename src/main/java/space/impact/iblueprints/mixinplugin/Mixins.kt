package space.impact.iblueprints.mixinplugin

import com.gtnewhorizon.gtnhmixins.builders.IBaseTransformer.Phase
import com.gtnewhorizon.gtnhmixins.builders.IMixins
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder

public enum class Mixins(private val mixinBuilder: MixinBuilder) : IMixins {

    PROJECTOR_LAYOUT_RESET(
        MixinBuilder("Clear the saved projector layout on sneak-right-click in air")
            .setPhase(Phase.LATE)
            .addRequiredMod(TargetedMod.IMPACT_API)
            .addCommonMixins("structures.ItemConstructableTriggerMixin")
            .setApplyIf {
                true
            },
    ),
    BUILT_STRUCTURE_BLUEPRINT(
        MixinBuilder("Copy a successfully checked GregTech structure into a blueprint")
            .setPhase(Phase.LATE)
            .addRequiredMod(TargetedMod.GREGTECH)
            .addRequiredMod(TargetedMod.IMPACT_API)
            .addCommonMixins(
                "gregtech.IMultiblockSetupInvoker",
                "gregtech.MultiblockPieceMixin",
            )
            .setApplyIf {
                true
            },
    ),
    BLUEPRINT_INTERFACE_TERMINAL(
        MixinBuilder("Allow structure patterns through the interface terminal item check")
            .setPhase(Phase.LATE)
            .addRequiredMod(TargetedMod.AE2)
            .addRequiredMod(TargetedMod.MODULAR_UI)
            .addCommonMixins(
                "ae2.InterfaceTerminalTrackerAccessor",
                "ae2.ContainerInterfaceTerminalMixin",
            )
            .addClientMixins("ae2.GuiInterfaceTerminalMixin")
            .setApplyIf {
                true
            },
    ),
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
