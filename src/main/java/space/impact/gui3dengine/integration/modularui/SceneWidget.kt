package space.impact.gui3dengine.integration.modularui

import com.cleanroommc.modularui.api.UpOrDown
import com.cleanroommc.modularui.api.widget.Interactable
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext
import com.cleanroommc.modularui.theme.WidgetThemeEntry
import com.cleanroommc.modularui.widget.Widget
import space.impact.gui3dengine.scene.SceneView

/** ModularUI owns layout and events; the engine owns rendering and picking. */
public class SceneWidget(public val view: SceneView) : Widget<SceneWidget>(), Interactable {

    private var shownScene = view.scene

    init {
        tooltipDynamic {
            val position = view.hovered
            if (position != null) {
                val item = view.scene.item(position)
                if (item != null) {
                    it.addLine(item.displayName)
                }
            }
        }
    }

    override fun draw(
        context: ModularGuiContext,
        widgetTheme: WidgetThemeEntry<*>,
    ) {
        val previous = view.hovered
        view.render(
            x = context.absMouseX - context.mouseX,
            y = context.absMouseY - context.mouseY,
            width = area.width,
            height = area.height,
            mouseX = context.absMouseX,
            mouseY = context.absMouseY,
        )
        if (previous != view.hovered || shownScene !== view.scene) {
            shownScene = view.scene
            markTooltipDirty()
        }
    }

    override fun onMousePressed(mouseButton: Int): Interactable.Result {
        val consumed = view.mouseClicked(
            mouseX = context.absMouseX,
            mouseY = context.absMouseY,
            button = mouseButton,
        )
        return if (consumed) {
            Interactable.Result.SUCCESS
        } else {
            Interactable.Result.IGNORE
        }
    }

    override fun onMouseScroll(
        scrollDirection: UpOrDown,
        amount: Int,
    ): Boolean {
        return view.scroll(
            mouseX = context.absMouseX,
            mouseY = context.absMouseY,
            steps = scrollDirection.modifier,
        )
    }
}
