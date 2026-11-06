package space.impact.iblueprints.integration.modularui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.drawable.Rectangle
import com.cleanroommc.modularui.factory.ClientGUI
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.ModularScreen
import com.cleanroommc.modularui.screen.UISettings
import com.cleanroommc.modularui.widget.sizer.Unit
import java.util.function.DoubleSupplier
import com.cleanroommc.modularui.widgets.ButtonWidget
import net.minecraft.util.StatCollector
import space.impact.iblueprints.MODID
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.SceneView

/** Optional client-only UI, opened explicitly; no server container or world capture. */
public object SceneViewer {

    @JvmStatic
    public fun open(entry: SceneEntry) {
        val view = SceneView(entry.build())
        val panel = ModularPanel("scene_viewer")
            .fullScreenInvisible()
            .background(Rectangle().color(0xFF24272D.toInt()))
            .onCloseAction {
                view.scene.close()
            }

        panel.child(
            IKey.str(view.scene.title).asWidget()
                .left(8)
                .top(8)
                .right(30)
                .height(14)
                .color(0xFFFFFF),
        )
        panel.child(ButtonWidget.panelCloseButton())
        panel.child(
            SceneWidget(view)
                .left(8)
                .top(52)
                .right(126)
                .bottom(40),
        )
        val channelCount = entry.channels.count {
            it.minimum != it.maximum
        }
        val channelsHeight = DoubleSupplier {
            // Reserve the toolbar, footer, materials heading and two material rows.
            val available = (panel.context.screenArea.height - (52 + 40 + 22 + 48)).coerceAtLeast(SceneChannelList.ROW_HEIGHT)
            val visibleRows = (available / SceneChannelList.ROW_HEIGHT).coerceAtMost(channelCount)
            (visibleRows * SceneChannelList.ROW_HEIGHT).toDouble()
        }
        val materialsTop = DoubleSupplier {
            if (channelCount == 0) {
                52.0
            } else {
                52.0 + channelsHeight.asDouble + 22.0
            }
        }
        if (channelCount > 0) {
            panel.child(
                SceneChannelList(
                    view = view,
                    entry = entry,
                )
                    .right(8).top(52).width(110)
                    .height(
                        channelsHeight,
                        Unit.Measure.PIXEL,
                    ),
            )
        }
        panel.child(
            IKey.lang("iblueprints.materials").asWidget()
                .right(8)
                .top(
                    {
                        materialsTop.asDouble - 16.0
                    },
                    Unit.Measure.PIXEL,
                )
                .width(110)
                .height(14)
                .color(0xFFFFFF),
        )
        panel.child(
            SceneMaterialList(view)
                .right(8)
                .top(
                    materialsTop,
                    Unit.Measure.PIXEL,
                )
                .bottom(40)
                .width(110),
        )

        val controls = listOf(
            IKey.lang("iblueprints.all"),
            IKey.dynamic {
                "XYZ"[view.slice.axis].toString()
            },
            IKey.str("−"),
            IKey.str("+"),
            IKey.dynamic {
                val enabled = if (view.slice.cutaway && view.slice.enabled) {
                    " *"
                } else {
                    ""
                }
                text("cut") + enabled
            },
            IKey.lang("iblueprints.home"),
        )
        for (index in controls.indices) {
            panel.child(
                SceneActionButton(
                    label = controls[index],
                    action = {
                        control(
                            view = view,
                            index = index,
                        )
                    },
                )
                    .left(8 + index * 30)
                    .top(28)
                    .size(
                        28,
                        20,
                    ),
            )
        }
        panel.child(
            IKey.dynamic {
                val slice = view.slice
                val comparison = if (slice.cutaway) {
                    "≤"
                } else {
                    "="
                }
                val layer = if (slice.enabled) {
                    "${"XYZ"[slice.axis]} $comparison ${slice.level}"
                } else {
                    text("all")
                }
                val selected = view.selected
                val block = selected?.let {
                    view.scene.item(it)?.displayName
                }.orEmpty()
                "$layer   $block"
            }.asWidget()
                .left(8)
                .right(8)
                .bottom(24)
                .height(12)
                .color(0xFFFFFF),
        )
        panel.child(
            IKey.lang("iblueprints.controls").asWidget()
                .left(8)
                .right(8)
                .bottom(4)
                .height(18)
                .color(0xB9C0CA),
        )
        val settings = UISettings()
        settings.recipeViewerSettings.disable()
        ClientGUI.open(
            ModularScreen(
                MODID,
                panel,
            ).openParentOnClose(true),
            settings,
        )
    }

    private fun control(
        view: SceneView,
        index: Int,
    ) {
        val slice = view.slice
        val axis = if (index == 1) {
            (slice.axis + 1) % 3
        } else {
            slice.axis
        }
        val bounds = view.scene.bounds()
        val minimum = coordinate(
            position = bounds.min,
            axis = axis,
        )
        val maximum = coordinate(
            position = bounds.max,
            axis = axis,
        )
        when (index) {
            0 -> {
                slice.showAll()
            }
            1 -> {
                slice.select(
                    axis = axis,
                    level = minimum,
                    cutaway = slice.cutaway,
                )
            }
            2, 3 -> {
                val initial = if (slice.enabled) {
                    slice.level
                } else {
                    minimum
                }
                val delta = if (index == 2) {
                    -1
                } else {
                    1
                }
                slice.select(
                    axis = axis,
                    level = (initial + delta).coerceIn(
                        minimumValue = minimum,
                        maximumValue = maximum,
                    ),
                    cutaway = slice.cutaway,
                )
            }
            4 -> {
                slice.select(
                    axis = axis,
                    level = slice.level.coerceIn(
                        minimumValue = minimum,
                        maximumValue = maximum,
                    ),
                    cutaway = !slice.cutaway,
                )
            }
            5 -> {
                view.resetCamera()
            }
        }
    }

    private fun coordinate(
        position: ScenePosition,
        axis: Int,
    ): Int {
        return when (axis) {
            0 -> {
                position.x
            }
            1 -> {
                position.y
            }
            else -> {
                position.z
            }
        }
    }

    private fun text(key: String): String {
        return StatCollector.translateToLocal("iblueprints.$key")
    }
}
