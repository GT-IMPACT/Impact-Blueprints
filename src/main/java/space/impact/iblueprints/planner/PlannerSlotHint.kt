package space.impact.iblueprints.planner

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.drawable.DynamicDrawable
import com.cleanroommc.modularui.drawable.GuiTextures
import com.cleanroommc.modularui.drawable.UITexture
import com.cleanroommc.modularui.widgets.slot.ItemSlot
import cpw.mods.fml.common.Loader
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import java.util.Locale

internal enum class PlannerSlotHint(
    val index: Int,
    private val texture: String,
    private val tint: Int = 0x70FFFFFF,
    private val requiredMod: String? = null,
) {

    PROJECTOR(
        index = BlueprintPlannerTile.PROJECTOR,
        texture = "impactapi:items/itemConstructableTrigger",
        requiredMod = "impactapi",
    ),
    PAPER(
        index = BlueprintPlannerTile.PAPER,
        texture = "minecraft:items/paper",
    ),
    BLUEPRINT(
        index = BlueprintPlannerTile.BLUEPRINT,
        texture = "minecraft:items/paper",
        tint = 0x90538CF3.toInt(),
    ),
    BLANK_PATTERN(
        index = BlueprintPlannerTile.BLANK_PATTERN,
        texture = "appliedenergistics2:items/ItemMaterial.BlankPattern",
        requiredMod = "appliedenergistics2",
    ),
    PATTERN(
        index = BlueprintPlannerTile.PATTERN,
        texture = "appliedenergistics2:items/ItemEncodedUltimatePattern",
        requiredMod = "appliedenergistics2",
    ),
    ;

    @SideOnly(Side.CLIENT)
    internal fun attach(widget: ItemSlot) {
        val missingMod = requiredMod?.takeUnless(Loader::isModLoaded)
        val texture = if (missingMod == null) {
            UITexture.builder()
                .location(texture)
                .nonOpaque()
                .build()
                .withColorOverride(tint)
        } else {
            GuiTextures.DISABLED
        }
        val icon = texture.asIcon().size(16)
        widget.backgroundOverlay(DynamicDrawable {
            if (widget.slot.stack == null) {
                icon
            } else {
                null
            }
        })
        val prefix = "iblueprints.planner.slot"
        val key = "$prefix.${name.lowercase(Locale.ROOT)}"
        val description = if (missingMod == null) {
            "$key.hint"
        } else {
            "$prefix.requires.$missingMod"
        }
        widget.tooltip {
            it.addLine(IKey.lang("$key.title"))
            it.addLine(IKey.lang(description).color(0xAAAAAA))
        }
    }
}
