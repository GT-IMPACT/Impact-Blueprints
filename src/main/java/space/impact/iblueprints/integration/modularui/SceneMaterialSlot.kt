package space.impact.iblueprints.integration.modularui

import com.cleanroommc.modularui.api.IThemeApi
import com.cleanroommc.modularui.drawable.ItemDrawable
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widgets.ButtonWidget
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector

internal class SceneMaterialSlot(
    stack: ItemStack,
    replace: (() -> Boolean)? = null,
) : ButtonWidget<SceneMaterialSlot>() {

    private val icon = ItemDrawable(stack)

    init {
        widgetTheme(IThemeApi.ITEM_SLOT)
        tooltipDynamic {
            it.addFromItem(stack)
            if (replace != null) {
                it.addLine(StatCollector.translateToLocal("iblueprints.variants.replace"))
            }
        }
        if (replace != null) {
            onMousePressed { button ->
                button == 0 && replace()
            }
        }
    }

    override fun draw(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
    ) {
        super.draw(
            context,
            widgetTheme,
        )
        icon.draw(
            context,
            1,
            1,
            18,
            18,
            widgetTheme.theme,
        )
    }
}
