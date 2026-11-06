package space.impact.iblueprints.integration.modularui

import codechicken.nei.recipe.GuiCraftingRecipe
import com.cleanroommc.modularui.api.drawable.IDrawable
import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.api.widget.IWidget
import com.cleanroommc.modularui.drawable.Rectangle
import com.cleanroommc.modularui.widgets.ListWidget
import cpw.mods.fml.common.Loader
import space.impact.iblueprints.scene.SceneView

internal class SceneMaterialList(private val view: SceneView) : ListWidget<IWidget, SceneMaterialList>() {

    private var shownMaterials = view.materials

    init {
        rebuild()
    }

    override fun onUpdate() {
        super.onUpdate()
        if (shownMaterials !== view.materials) {
            shownMaterials = view.materials
            removeAll()
            rebuild()
        }
    }

    private fun rebuild() {
        for (material in view.materials) {
            val quantity = "x ${material.positions.size}"
            val label = IKey.str(quantity).color {
                if (view.isMaterialSelected(material)) {
                    0xFFD166
                } else {
                    0xFFFFFF
                }
            }
            child(
                SceneMaterialRow()
                    .height(24)
                    .widthRel(1f)
                    .child(
                        SceneMaterialSlot(material.stack).left(2).top(2).size(20)
                            .onMouseTapped { button ->
                                if (button != 0 || !Loader.isModLoaded("NotEnoughItems")) {
                                    return@onMouseTapped false
                                }
                                GuiCraftingRecipe.openRecipeGui(
                                    "item",
                                    material.stack.copy(),
                                )
                            },
                    )
                    .child(
                        SceneActionButton(
                            label = label,
                            action = {
                                view.toggleMaterial(material)
                            },
                        )
                            .left(26)
                            .right(0)
                            .height(24)
                            .background(IDrawable.NONE)
                            .hoverBackground(Rectangle().color(0x30FFFFFF)),
                    ),
            )
        }
    }
}
