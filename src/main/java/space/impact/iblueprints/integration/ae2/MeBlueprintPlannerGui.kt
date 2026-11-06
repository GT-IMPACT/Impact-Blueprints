package space.impact.iblueprints.integration.ae2

import appeng.client.gui.implementations.GuiMEMonitorable
import appeng.client.gui.widgets.GuiSimpleImgButton
import appeng.container.slot.AppEngSlot
import cpw.mods.fml.common.Loader
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.util.StatCollector
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11
import space.impact.iblueprints.integration.modularui.SceneViewer
import space.impact.iblueprints.integration.nei.PlannerRecipeSelection
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.planner.BlueprintPlannerGui
import space.impact.iblueprints.planner.PlannerSession
import space.impact.iblueprints.scene.SceneView
import java.util.Locale

internal class MeBlueprintPlannerGui(
    inventory: InventoryPlayer,
    part: MeBlueprintPlannerPart,
    private val terminal: MeBlueprintPlannerContainer = MeBlueprintPlannerContainer(
        inventory = inventory,
        part = part,
    ),
) : GuiMEMonitorable(
    inventory,
    part,
    terminal,
) {

    private val controls = linkedMapOf<GuiSimpleImgButton, () -> Boolean>()
    private val actions = linkedMapOf<GuiSimpleImgButton, () -> Unit>()
    private var previewPlan: NBTTagCompound? = null
    private var preview: SceneView? = null
    private val plannerTop: Int
        get() {
            return ySize - 99 - reservedSpace
        }

    init {
        reservedSpace = 82
    }

    override fun initGui() {
        super.initGui()
        controls.clear()
        actions.clear()
        val state = terminal.state
        val planner = terminal.planner

        addControl(
            key = "select",
            icon = 36,
            x = 90,
            y = 10,
            enabled = {
                state.recipe.isEmpty() && Loader.isModLoaded("NotEnoughItems")
            },
        ) {
            PlannerRecipeSelection.open()
        }
        addControl(
            key = "simulate",
            icon = 226,
            x = 109,
            y = 10,
            enabled = {
                BlueprintPlannerGui.entry(state) != null && planner.currentPlan() != null
            },
        ) {
            val entry = BlueprintPlannerGui.entry(state) ?: return@addControl
            val plan = planner.currentPlan() ?: return@addControl
            val selection = state.selection
            SceneViewer.open(
                entry = entry,
                initialValues = SceneBlueprint.channels(plan),
                highlighted = emptySet(),
                savedPlan = plan,
                onLayoutChanged = {
                    planner.edit(
                        selection = selection,
                        plan = it,
                    )
                },
            )
        }
        addControl(
            key = "reset",
            icon = 6,
            x = 128,
            y = 10,
            enabled = {
                state.recipe.isNotEmpty()
            },
        ) {
            planner.action(PlannerSession.RESET)
        }
        addControl(
            key = "projector",
            icon = 39,
            x = 109,
            y = 30,
            enabled = state::canSaveProjector,
        ) {
            planner.action(PlannerSession.PROJECTOR)
        }
        addControl(
            key = "blueprint",
            icon = MeEncodeButton.ICON,
            x = 109,
            y = 49,
            enabled = state::canSaveBlueprint,
        ) {
            planner.action(PlannerSession.BLUEPRINT)
        }
        addControl(
            key = "load_blueprint",
            icon = 7,
            x = 147,
            y = 49,
            enabled = state::hasBlueprintInput,
        ) {
            planner.action(PlannerSession.LOAD_BLUEPRINT)
        }
        addControl(
            key = "pattern",
            icon = MeEncodeButton.ICON,
            x = 109,
            y = 68,
            enabled = state::canSavePattern,
        ) {
            planner.action(PlannerSession.PATTERN)
        }
    }

    override fun actionPerformed(button: GuiButton) {
        val action = actions[button]
        if (action != null) {
            if (controls[button]?.invoke() == true) {
                action()
            }
        } else {
            super.actionPerformed(button)
        }
    }

    override fun getMaxRows(): Int {
        return super.getMaxRows().coerceAtMost(6)
    }

    override fun getGuiDisplayName(name: String): String {
        val title = super.getGuiDisplayName(name)
        val availableWidth = searchField.xPos() - guiLeft - 8 - 4
        if (fontRendererObj.getStringWidth(title) <= availableWidth) {
            return title
        }

        val suffix = "..."
        val prefixWidth = availableWidth - fontRendererObj.getStringWidth(suffix)
        if (prefixWidth <= 0) {
            return ""
        }
        return fontRendererObj.trimStringToWidth(
            title,
            prefixWidth,
        ).trimEnd() + suffix
    }

    override fun onGuiClosed() {
        preview?.scene?.close()
        preview = null
        previewPlan = null
        super.onGuiClosed()
    }

    override fun repositionSlot(slot: AppEngSlot) {
        if (slot in terminal.blueprintSlots.values) {
            slot.yDisplayPosition = plannerTop + slot.y
        } else {
            super.repositionSlot(slot)
        }
    }

    override fun drawBG(
        offsetX: Int,
        offsetY: Int,
        mouseX: Int,
        mouseY: Int,
    ) {
        // Draw the stock terminal with its inventory footer shifted down to make room for the planner.
        bindTexture(background)
        drawTexturedModalRect(
            offsetX,
            offsetY,
            0,
            0,
            195,
            18,
        )
        drawTexturedModalRect(
            offsetX + 195,
            offsetY,
            195,
            0,
            47,
            128,
        )
        for (row in 0 until rows) {
            drawTexturedModalRect(
                offsetX,
                offsetY + 18 + row * 18,
                0,
                18,
                195,
                18,
            )
        }

        // Keep the grid's bottom border and the taper to the narrow panel from AE2's texture.
        drawTexturedModalRect(
            offsetX,
            offsetY + plannerTop,
            0,
            70,
            195,
            9,
        )
        Gui.func_152125_a(
            offsetX,
            offsetY + plannerTop + 9,
            0f,
            82f,
            176,
            1,
            176,
            reservedSpace,
            256f,
            256f,
        )
        drawTexturedModalRect(
            offsetX,
            offsetY + plannerTop + reservedSpace + 9,
            0,
            79,
            195,
            90,
        )
        bindTexture("guis/pattern.png")
        for (slot in terminal.blueprintSlots.values) {
            drawTexturedModalRect(
                offsetX + slot.x - 1,
                offsetY + slot.yDisplayPosition - 1,
                7,
                166,
                18,
                18,
            )
        }
        drawPreview(
            x = offsetX + PREVIEW_X,
            y = offsetY + plannerTop + PREVIEW_Y,
        )
        searchField.drawTextBox()
        updateViewCells()
    }

    override fun drawFG(
        offsetX: Int,
        offsetY: Int,
        mouseX: Int,
        mouseY: Int,
    ) {
        super.drawFG(
            offsetX,
            offsetY,
            mouseX,
            mouseY,
        )
        for ((button, enabled) in controls) {
            button.enabled = enabled()
        }

        val localX = mouseX - offsetX
        val localY = mouseY - offsetY
        val previewY = plannerTop + PREVIEW_Y
        if (localX in PREVIEW_X until PREVIEW_X + PREVIEW_WIDTH && localY in previewY until previewY + PREVIEW_HEIGHT) {
            val name = previewPlan?.let(SceneBlueprint::controller)?.displayName ?: label("no_structure")
            drawTooltip(
                localX,
                localY,
                name,
            )
        }

        for ((hint, slot) in terminal.blueprintSlots) {
            if (slot.hasStack) {
                continue
            }
            val x = slot.xDisplayPosition
            val y = slot.yDisplayPosition
            val missing = hint.missingMod()
            if (missing == null) {
                val texture = hint.texture
                val resource = ResourceLocation(
                    texture.substringBefore(':'),
                    "textures/${texture.substringAfter(':')}.png",
                )
                mc.textureManager.bindTexture(resource)
                GL11.glPushAttrib(GL11.GL_ENABLE_BIT or GL11.GL_COLOR_BUFFER_BIT or GL11.GL_CURRENT_BIT)
                try {
                    GL11.glEnable(GL11.GL_BLEND)
                    GL11.glBlendFunc(
                        GL11.GL_SRC_ALPHA,
                        GL11.GL_ONE_MINUS_SRC_ALPHA,
                    )
                    val tint = hint.tint
                    GL11.glColor4f(
                        (tint shr 16 and 255) / 255f,
                        (tint shr 8 and 255) / 255f,
                        (tint and 255) / 255f,
                        (tint ushr 24) / 255f,
                    )
                    Gui.func_146110_a(
                        x,
                        y,
                        0f,
                        0f,
                        16,
                        16,
                        16f,
                        16f,
                    )
                } finally {
                    GL11.glPopAttrib()
                }
            }
            if (localX in x until x + 16 && localY in y until y + 16 && mc.thePlayer.inventory.itemStack == null) {
                val key = "slot.${hint.name.lowercase(Locale.ROOT)}"
                val description = if (missing == null) {
                    "$key.hint"
                } else {
                    "slot.requires.$missing"
                }
                drawTooltip(localX, localY, arrayOf(label("$key.title"), label(description)))
            }
        }
    }

    private fun drawPreview(
        x: Int,
        y: Int,
    ) {
        val plan = terminal.planner.currentPlan()
        if (previewPlan !== plan) {
            preview?.scene?.close()
            preview = null
            previewPlan = plan
            val entry = BlueprintPlannerGui.entry(terminal.state)
            if (plan != null && entry != null) {
                preview = try {
                    SceneView(entry.build(SceneBlueprint.channels(plan))).also {
                        try {
                            SceneBlueprint.restore(
                                view = it,
                                plan = plan,
                            )
                        } catch (failure: Throwable) {
                            it.scene.close()
                            throw failure
                        }
                    }
                } catch (failure: Exception) {
                    LogManager.getLogger("ImpactBlueprints").warn("Cannot preview structure ${entry.id}", failure)
                    null
                }
            }
        }

        val view = preview
        if (view != null) {
            view.render(
                x = x,
                y = y,
                width = PREVIEW_WIDTH,
                height = PREVIEW_HEIGHT,
                mouseX = Int.MIN_VALUE,
                mouseY = Int.MIN_VALUE,
            )
        } else {
            Gui.drawRect(
                x,
                y,
                x + PREVIEW_WIDTH,
                y + PREVIEW_HEIGHT,
                0xFF171A1F.toInt(),
            )
        }
    }

    private fun addControl(
        key: String,
        icon: Int,
        x: Int,
        y: Int,
        enabled: () -> Boolean,
        action: () -> Unit,
    ) {
        val buttonX = guiLeft + x
        val buttonY = guiTop + plannerTop + y
        val tooltip = label(key)
        val button = if (icon == MeEncodeButton.ICON) {
            MeEncodeButton(
                x = buttonX,
                y = buttonY,
                tooltip = tooltip,
            )
        } else {
            GuiSimpleImgButton(
                buttonX,
                buttonY,
                icon,
                tooltip,
            )
        }
        button.enabled = enabled()
        controls[button] = enabled
        actions[button] = action
        buttonList.add(button)
    }

    private fun label(key: String): String {
        return StatCollector.translateToLocal("iblueprints.planner.$key")
    }

    private companion object {

        const val PREVIEW_X = 8
        const val PREVIEW_Y = 10
        const val PREVIEW_WIDTH = 74
        const val PREVIEW_HEIGHT = 74
    }
}
