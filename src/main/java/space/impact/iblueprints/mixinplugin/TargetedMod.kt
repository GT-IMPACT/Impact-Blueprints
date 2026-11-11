package space.impact.iblueprints.mixinplugin

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder

public enum class TargetedMod(private val targetBuilder: TargetModBuilder) : ITargetMod {

    AE2FC(
        TargetModBuilder()
            .setModId("ae2fc"),
    ),
    AE2(
        TargetModBuilder()
            .setModId("appliedenergistics2"),
    ),
    MODULAR_UI(
        TargetModBuilder()
            .setModId("modularui2"),
    ),
    GREGTECH(
        TargetModBuilder()
            .setModId("gregtech"),
    ),
    IMPACT_API(
        TargetModBuilder()
            .setModId("impactapi"),
    );

    override fun getBuilder(): TargetModBuilder {
        return targetBuilder
    }
}
