package space.impact.iblueprints.integration.modularui

import com.cleanroommc.modularui.ModularUI
import com.cleanroommc.modularui.api.widget.Interactable
import com.cleanroommc.modularui.api.widget.Interactable.Result
import com.cleanroommc.modularui.drawable.GuiDraw
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.SlotTheme
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widgets.ItemDisplayWidget
import com.cleanroommc.neverenoughanimations.NEAConfig
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector

internal class SceneMaterialSlot(
    stack: ItemStack,
    private val replace: (() -> Boolean)? = null,
    private val onTap: ((Int) -> Boolean)? = null,
) : ItemDisplayWidget(), Interactable {

    private val displayedStack = stack.copy()

    init {
        // Inventory slots create sync handlers, which client-only secondary panels cannot contain.
        item(displayedStack)
        displayAmount(true)
        tooltip().setAutoUpdate(true).tooltipBuilder { tooltip ->
            tooltip.addFromItem(displayedStack)
            if (replace != null) {
                tooltip.addLine(StatCollector.translateToLocal("iblueprints.variants.replace"))
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
        if (isHovering && (!ModularUI.Mods.NEA.isLoaded || NEAConfig.itemHoverOverlay)) {
            val theme = getWidgetTheme(
                panel.theme,
                SlotTheme::class.java,
            )
            GuiDraw.drawRect(
                1f,
                1f,
                16f,
                16f,
                theme.theme.slotHoverColor,
            )
        }
    }

    override fun drawForeground(context: ModularGuiContext) {
        val tooltip = tooltip
        if (tooltip != null && isHoveringFor(tooltip.showUpTimer) && !context.hasDraggable()) {
            context.queueForegroundRender {
                tooltip.draw(
                    context,
                    displayedStack,
                )
            }
        }
    }

    override fun onMousePressed(mouseButton: Int): Result {
        if (mouseButton == 0 && replace?.invoke() == true) {
            return Result.SUCCESS
        }
        return Result.ACCEPT
    }

    override fun onMouseRelease(mouseButton: Int): Boolean {
        return true
    }

    override fun onMouseTapped(mouseButton: Int): Result {
        return if (onTap?.invoke(mouseButton) == true) {
            Result.SUCCESS
        } else {
            Result.IGNORE
        }
    }

    override fun onMouseDrag(
        mouseButton: Int,
        timeSinceClick: Long,
    ) {
    }
}
