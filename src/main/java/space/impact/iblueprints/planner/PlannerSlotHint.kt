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
    internal val texture: String,
    internal val tint: Int = 0x70FFFFFF,
    private val requiredMod: String? = null,
) {

    PROJECTOR(
        index = PlannerState.PROJECTOR,
        texture = "impactapi:items/itemConstructableTrigger",
        requiredMod = "impactapi",
    ),
    BLUEPRINT_INPUT(
        index = PlannerState.BLUEPRINT_INPUT,
        texture = "iblueprints:items/blank_blueprint",
    ),
    BLUEPRINT(
        index = PlannerState.BLUEPRINT,
        texture = "minecraft:items/paper",
        tint = 0x90538CF3.toInt(),
    ),
    BLANK_PATTERN(
        index = PlannerState.BLANK_PATTERN,
        texture = "appliedenergistics2:items/ItemMaterial.BlankPattern",
        requiredMod = "appliedenergistics2",
    ),
    PATTERN(
        index = PlannerState.PATTERN,
        texture = "appliedenergistics2:items/ItemEncodedUltimatePattern",
        tint = 0x706EA9FF,
        requiredMod = "appliedenergistics2",
    ),
    ;

    internal fun missingMod(): String? {
        return requiredMod?.takeUnless(Loader::isModLoaded)
    }

    @SideOnly(Side.CLIENT)
    internal fun attach(widget: ItemSlot) {
        val missingMod = missingMod()
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
        widget.overlay(DynamicDrawable {
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
