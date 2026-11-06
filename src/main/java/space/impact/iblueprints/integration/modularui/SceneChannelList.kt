package space.impact.iblueprints.integration.modularui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.api.widget.IWidget
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.widgets.ListWidget
import net.minecraft.client.Minecraft
import net.minecraft.util.StatCollector
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.planner.tracePlannerStep
import space.impact.iblueprints.scene.SceneView

internal class SceneChannelList(
    private val view: SceneView,
    private val entry: SceneEntry,
    private val values: MutableMap<String, Int>,
    onChanged: () -> Unit,
    editable: Boolean,
) : ListWidget<IWidget, SceneChannelList>() {

    internal companion object {

        internal const val ROW_HEIGHT: Int = 36
    }

    init {
        for (channel in entry.channels) {
            if (channel.minimum == channel.maximum) {
                continue
            }
            val row = SceneChannelRow().height(ROW_HEIGHT).widthRel(1f)
            row.child(
                IKey.dynamic {
                    Minecraft.getMinecraft().fontRenderer.trimStringToWidth(
                        channel.label,
                        row.area.width.coerceAtLeast(1),
                    )
                }.asWidget()
                    .left(0).top(0).right(0).height(12).color(0xFFFFFF)
                    .tooltip {
                        it.addLine(channel.label)
                        it.addLine(channel.description)
                        it.addLine("${channel.minimum} - ${channel.maximum}")
                    },
            )
            row.child(
                IKey.dynamic {
                    values.getValue(channel.id).toString()
                }.asWidget()
                    .left(22)
                    .widthRelOffset(
                        1f,
                        -44,
                    )
                    .top(14)
                    .height(20)
                    .color(0xFFFFFF)
                    .textAlign(Alignment.Center),
            )
            child(row)
            if (!editable) {
                continue
            }
            for (delta in listOf(
                -1,
                1,
            )) {
                val button = SceneActionButton(
                    label = IKey.str(if (delta < 0) {
                        "-"
                    } else {
                        "+"
                    }).color {
                        val value = values.getValue(channel.id)
                        if ((delta < 0 && value <= channel.minimum) || (delta > 0 && value >= channel.maximum)) {
                            0x777777
                        } else {
                            0xFFFFFF
                        }
                    },
                    action = {
                        val value = values.getValue(channel.id).toLong() + delta
                        val next = channel.normalize(value.coerceIn(
                            minimumValue = 1L,
                            maximumValue = Int.MAX_VALUE.toLong(),
                        ).toInt())
                        if (next != values[channel.id]) {
                            val requested = values.toMutableMap()
                            requested[channel.id] = next
                            try {
                                tracePlannerStep(
                                    step = "viewer.changeChannel",
                                    details = "entry=${entry.id} channel=${channel.id} value=$next",
                                ) {
                                    val replacement = entry.build(requested)
                                    view.replaceConfiguredScene(replacement)
                                }
                                values[channel.id] = next
                                onChanged()
                            } catch (failure: Throwable) {
                                if (failure !is Exception && failure !is LinkageError) {
                                    throw failure
                                }
                                LogManager.getLogger("ImpactBlueprints").error(
                                    "Cannot change scene channel",
                                    failure,
                                )
                                view.scene.diagnostics.add(StatCollector.translateToLocal("iblueprints.error"))
                            }
                        }
                    },
                ).top(14).size(20)
                if (delta < 0) {
                    button.left(0)
                } else {
                    button.right(0)
                }
                row.child(button)
            }
        }
    }
}
