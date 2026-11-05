package space.impact.gui3dengine.integration.modularui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.api.widget.IWidget
import com.cleanroommc.modularui.drawable.ItemDrawable
import com.cleanroommc.modularui.api.drawable.IDrawable
import com.cleanroommc.modularui.drawable.Rectangle
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.widgets.ListWidget
import space.impact.gui3dengine.scene.SceneView

internal class SceneMaterialList(private val view: SceneView) : ListWidget<IWidget, SceneMaterialList>() {

    private var shownScene = view.scene

    init {
        rebuild()
    }

    override fun onUpdate() {
        super.onUpdate()
        if (shownScene !== view.scene) {
            shownScene = view.scene
            removeAll()
            rebuild()
        }
    }

    private fun rebuild() {
        for (material in view.materials) {
            val positions = material.positions.toSet()
            child(
                SceneMaterialRow()
                    .height(24)
                    .widthRel(1f)
                    .background(IDrawable.NONE)
                    .hoverBackground(Rectangle().color(0x30FFFFFF))
                    .onMousePressed {
                        if (it == 0) {
                            view.toggleMaterial(material)
                            true
                        } else {
                            false
                        }
                    }
                    .tooltip {
                        it.addLine(material.stack.displayName)
                        it.addLine("× ${positions.size}")
                    }
                    .child(
                        ItemDrawable(material.stack).asWidget()
                            .size(18)
                            .left(3)
                            .top(3),
                    )
                    .overlay(
                        IKey.str("× ${positions.size}").color {
                            if (view.isMaterialSelected(material)) {
                                0xFFD166
                            } else {
                                0xFFFFFF
                            }
                        }.asIcon().alignment(Alignment.CenterRight),
                    ),
            )
        }
    }
}
