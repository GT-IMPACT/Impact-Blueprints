package space.impact.iblueprints.integration.modularui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.drawable.Rectangle
import com.cleanroommc.modularui.factory.ClientGUI
import com.cleanroommc.modularui.screen.ModularContainer
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.ModularScreen
import com.cleanroommc.modularui.screen.UISettings
import com.cleanroommc.modularui.value.StringValue
import com.cleanroommc.modularui.utils.Alignment
import com.cleanroommc.modularui.widget.sizer.Unit as SizerUnit
import java.util.function.DoubleSupplier
import com.cleanroommc.modularui.widgets.ButtonWidget
import com.cleanroommc.modularui.widgets.layout.Flow
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget
import cpw.mods.fml.common.Loader
import java.util.Locale
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.util.StatCollector
import space.impact.iblueprints.MODID
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.integration.gregtech.GregTechScenes
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.planner.logPlannerTrace
import space.impact.iblueprints.planner.tracePlannerStep
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.SceneView

/** Optional client-only UI, opened explicitly; no server container or world capture. */
public object SceneViewer {

    private const val ALTERNATIVES_WIDTH = 144

    internal fun isCapturedLayout(
        entry: SceneEntry,
        plan: NBTTagCompound,
    ): Boolean {
        return SceneBlueprint.channels(plan).isEmpty() && entry.id.startsWith("$MODID:metatile_") &&
            Loader.isModLoaded("gregtech")
    }

    @JvmStatic
    public fun open(entry: SceneEntry) {
        open(
            entry = entry,
            initialValues = emptyMap(),
            highlighted = emptySet(),
        )
    }

    internal fun open(
        entry: SceneEntry,
        initialValues: Map<String, Int>,
        highlighted: Set<ScenePosition>,
        savedPlan: NBTTagCompound? = null,
        onLayoutChanged: ((NBTTagCompound) -> Unit)? = null,
    ) {
        val parent = Minecraft.getMinecraft().currentScreen
        val panel = createPanel(
            entry = entry,
            initialValues = initialValues,
            highlighted = highlighted,
            savedPlan = savedPlan,
            onLayoutChanged = onLayoutChanged,
        )
        val settings = UISettings()
        settings.recipeViewerSettings.disable()
        val screen = object : ModularScreen(
            MODID,
            panel,
        ) {
            override fun onOpen() {
                super.onOpen()
                context.setParentScreen(parent)
            }
        }
        screen.openParentOnClose(true)
        screen.context.setSettings(settings)
        val container = ModularContainer()
        container.constructClientOnly()
        val gui = SceneViewerGui(
            container = container,
            screen = screen,
            parentContainer = Minecraft.getMinecraft().thePlayer.openContainer,
        )
        tracePlannerStep(
            step = "viewer.open",
            details = "entry=${entry.id} editing=${onLayoutChanged != null}",
        ) {
            ClientGUI.open(gui)
        }
    }

    internal fun createPanel(
        entry: SceneEntry,
        initialValues: Map<String, Int>,
        highlighted: Set<ScenePosition> = emptySet(),
        savedPlan: NBTTagCompound? = null,
        onLayoutChanged: ((NBTTagCompound) -> Unit)? = null,
    ): ModularPanel {
        val editing = onLayoutChanged != null
        val details = "entry=${entry.id} editing=$editing"
        val capturedLayout = savedPlan != null && isCapturedLayout(
            entry = entry,
            plan = savedPlan,
        )
        val view = tracePlannerStep(
            step = "viewer.build",
            details = details,
        ) {
            val scene = if (capturedLayout) {
                GregTechScenes.create(
                    controller = entry.item(),
                    values = emptyMap(),
                    savedPlan = savedPlan,
                )
            } else {
                entry.build(initialValues)
            }
            SceneView(scene)
        }
        view.highlight(highlighted)
        if (savedPlan != null && !capturedLayout) {
            tracePlannerStep(
                step = "viewer.restore",
                details = details,
            ) {
                SceneBlueprint.restore(
                    view = view,
                    plan = savedPlan,
                )
            }
        }
        val sceneChannels = if (capturedLayout) {
            emptyList()
        } else {
            entry.channels
        }
        val channelValues = sceneChannels.associate {
            it.id to it.normalize(initialValues[it.id] ?: it.minimum)
        }.toMutableMap()
        val saveEdits: () -> Unit = {
            if (onLayoutChanged != null) {
                val captureDetails = "structure=${view.scene.title}"
                val plan = tracePlannerStep(
                    step = "viewer.capture",
                    details = captureDetails,
                ) {
                    SceneBlueprint.capture(
                        scene = view.scene,
                        channels = channelValues,
                    )
                }
                if (plan != null) {
                    onLayoutChanged(plan)
                } else {
                    logPlannerTrace("viewer.capture rejected $captureDetails")
                    Minecraft.getMinecraft().thePlayer?.addChatMessage(
                        ChatComponentTranslation("iblueprints.planner.save_failed"),
                    )
                }
            }
        }
        val panel = object : ModularPanel("scene_viewer") {
            override fun onOpen(screen: ModularScreen) {
                super.onOpen(screen)
                if (!isMainPanel) {
                    screen.context.recipeViewerSettings.disable()
                }
            }

            override fun onClose() {
                if (!isMainPanel) {
                    context.recipeViewerSettings.defaultState()
                }
                super.onClose()
            }

            override fun disablePanelsBelow(): Boolean {
                return true
            }

            override fun isDraggable(): Boolean {
                return false
            }
        }
            .fullScreenInvisible()
            .background(Rectangle().color(0xFF24272D.toInt()))
            .onCloseAction {
                tracePlannerStep(
                    step = "viewer.close",
                    details = details,
                ) {
                    view.scene.close()
                }
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
        val sceneTop = if (editing && view.scene.simulation != null) {
            panel.child(
                IKey.dynamic {
                    when {
                        !view.hasEditedStructure -> text("simulation.preview")
                        view.structureValid == true -> text("simulation.valid")
                        view.structureValid == false -> text("simulation.invalid")
                        else -> text("simulation.unverified")
                    }
                }.asWidget()
                    .left(8).right(8).top(51).height(13)
                    .textAlign(Alignment.Center)
                    .color {
                        when {
                            !view.hasEditedStructure -> 0xB9C0CA
                            view.structureValid == true -> 0x82DB93
                            else -> 0xFFD166
                        }
                    },
            )
            66
        } else {
            52
        }
        val sceneLeft = if (editing) {
            ALTERNATIVES_WIDTH + 16
        } else {
            8
        }
        panel.child(SceneWidget(view).left(sceneLeft).top(sceneTop).right(126).bottom(40))
        if (editing) {
            panel.child(
                createAlternativesPanel(
                    view = view,
                    top = sceneTop,
                    onChanged = saveEdits,
                ),
            )
        }
        val channelCount = sceneChannels.count {
            it.minimum != it.maximum
        }
        val sidebarTop = sceneTop
        val channelsHeight = DoubleSupplier {
            // Reserve the toolbar, footer, materials heading and two material rows.
            val available = (panel.context.screenArea.height - (sidebarTop + 40 + 22 + 48))
                .coerceAtLeast(SceneChannelList.ROW_HEIGHT)
            val visibleRows = (available / SceneChannelList.ROW_HEIGHT).coerceAtMost(channelCount)
            (visibleRows * SceneChannelList.ROW_HEIGHT).toDouble()
        }
        val materialsHeadingSpace = 16.0
        val materialsTop = DoubleSupplier {
            if (channelCount == 0) {
                sidebarTop + materialsHeadingSpace
            } else {
                sidebarTop + channelsHeight.asDouble + 22.0
            }
        }
        if (channelCount > 0) {
            panel.child(
                SceneChannelList(
                    view = view,
                    entry = entry,
                    values = channelValues,
                    onChanged = saveEdits,
                    editable = editing || savedPlan == null,
                )
                    .right(8).top(sidebarTop).width(110)
                    .height(
                        channelsHeight,
                        SizerUnit.Measure.PIXEL,
                    ),
            )
        }
        panel.child(
            IKey.lang("iblueprints.materials").asWidget()
                .right(8)
                .top(
                    {
                        materialsTop.asDouble - materialsHeadingSpace
                    },
                    SizerUnit.Measure.PIXEL,
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
                    SizerUnit.Measure.PIXEL,
                )
                .bottom(40)
                .width(110),
        )

        val controls = listOf(
            IKey.lang("iblueprints.all"),
            IKey.dynamic {
                "XYZ"[view.slice.axis].toString()
            },
            IKey.str("-"),
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
        var controlLeft = 8
        for (index in controls.indices) {
            val controlWidth = if (index >= 4) {
                44
            } else {
                28
            }
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
                    .left(controlLeft)
                    .top(28)
                    .size(
                        controlWidth,
                        20,
                    ),
            )
            controlLeft += controlWidth + 2
        }
        panel.child(
            IKey.dynamic {
                val slice = view.slice
                val comparison = if (slice.cutaway) {
                    "<="
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
        return panel
    }

    private fun createAlternativesPanel(
        view: SceneView,
        top: Int,
        onChanged: () -> Unit,
    ): Flow {
        val search = StringValue("")
        val searchField = object : TextFieldWidget() {
            override fun canScrollHorizontally(): Boolean {
                return false
            }
        }
            .value(search)
            .autoUpdateOnChange(true)
            .setMaxLength(80)
            .hintText(text("variants.search"))
            .widthRel(1f).height(20).marginBottom(6)
        val rotate = SceneActionButton(
            label = IKey.lang("iblueprints.hatch.rotate"),
            action = {
                val position = view.inspectedPosition()
                if (position != null) {
                    val rotated = tracePlannerStep(
                        step = "viewer.rotate",
                        details = "structure=${view.scene.title} position=$position",
                    ) {
                        view.rotateHatch(position)
                    }
                    if (rotated) {
                        onChanged()
                    }
                }
            },
        ).widthRel(1f).height(20).marginBottom(4)
            .disabled()
            .setEnabledIf {
                val position = view.inspectedPosition()
                position != null && view.scene.simulation?.nextHatchFacing(position) != null
            }
            .tooltipDynamic { tooltip ->
                val position = view.inspectedPosition()
                val facing = position?.let {
                    view.scene.simulation?.hatchFacing(it)
                }
                if (facing != null) {
                    tooltip.addLine(text("hatch.side.${facing.name.lowercase(Locale.ROOT)}"))
                }
            }
        rotate.tooltip().setAutoUpdate(true)
        return Flow.column()
            .left(8).top(top).width(ALTERNATIVES_WIDTH)
            .heightRelOffset(
                1f,
                -top - 40,
            )
            .padding(8)
            .collapseDisabledChild()
            .background(Rectangle().color(0xFF171A20.toInt()))
            .child(searchField)
            .child(
                IKey.lang("iblueprints.variants.select").asWidget()
                    .widthRel(1f).height(32).textAlign(Alignment.Center).color(0xB9C0CA)
                    .setEnabledIf {
                        view.inspectedPositions().isEmpty()
                    },
            )
            .child(
                IKey.dynamic {
                    val count = view.inspectedPositions().size
                    if (count > 1) {
                        StatCollector.translateToLocalFormatted("iblueprints.variants.common", count)
                    } else {
                        text("variants")
                    }
                }.asWidget()
                    .widthRel(1f).height(14).color(0xFFFFFF)
                    .disabled()
                    .setEnabledIf {
                        view.inspectedPositions().isNotEmpty()
                    },
            )
            .child(rotate)
            .child(SceneAlternativeList(
                view = view,
                query = search::getStringValue,
                onChanged = onChanged,
            ).widthRel(1f).expanded())
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
