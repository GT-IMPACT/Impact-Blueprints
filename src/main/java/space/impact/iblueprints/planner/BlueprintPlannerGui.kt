package space.impact.iblueprints.planner

import com.cleanroommc.modularui.api.IPanelHandler
import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.screen.ModularPanel
import com.cleanroommc.modularui.screen.UISettings
import com.cleanroommc.modularui.value.sync.PanelSyncManager
import com.cleanroommc.modularui.widgets.SlotGroupWidget
import com.cleanroommc.modularui.widgets.slot.ItemSlot
import com.cleanroommc.modularui.widgets.slot.ModularSlot
import cpw.mods.fml.common.Loader
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.integration.modularui.SceneActionButton
import space.impact.iblueprints.integration.modularui.SceneViewer
import space.impact.iblueprints.integration.nei.PlannerRecipeSelection
import space.impact.iblueprints.integration.structures.SceneBlueprint
import space.impact.iblueprints.item.StructureBlueprintItem

internal object BlueprintPlannerGui {

    internal fun build(
        tile: BlueprintPlannerTile,
        manager: PanelSyncManager,
        settings: UISettings,
    ): ModularPanel {
        val sync = PlannerSync(tile)
        manager.syncValue(
            "planner",
            0,
            sync.allowC2S(),
        )
        settings.customContainer {
            PlannerContainer(sync)
        }
        manager.registerSlotGroup(
            "planner",
            1,
        )
        val panel = ModularPanel.defaultPanel(
            "blueprint_planner",
            220,
            214,
        )
        val client = manager.isClient
        panel.child(SlotGroupWidget.playerInventory(true))
        panel.child(
            slot(
                tile = tile,
                hint = PlannerSlotHint.PROJECTOR,
                x = 8,
                y = 60,
                client = client,
                accepts = BlueprintPlannerTile::acceptsProjector,
            ),
        )
        panel.child(
            slot(
                tile = tile,
                hint = PlannerSlotHint.PAPER,
                x = 8,
                y = 81,
                client = client,
                accepts = {
                    it.item === Items.paper || it.item === StructureBlueprintItem
                },
            ),
        )
        panel.child(
            slot(
                tile = tile,
                hint = PlannerSlotHint.BLUEPRINT,
                x = 32,
                y = 81,
                client = client,
            ),
        )
        panel.child(
            slot(
                tile = tile,
                hint = PlannerSlotHint.BLANK_PATTERN,
                x = 8,
                y = 102,
                client = client,
                accepts = BlueprintPlannerTile::acceptsBlankPattern,
            ),
        )
        panel.child(
            slot(
                tile = tile,
                hint = PlannerSlotHint.PATTERN,
                x = 32,
                y = 102,
                client = client,
            ),
        )
        if (client) {
            addControls(
                panel = panel,
                tile = tile,
                sync = sync,
            )
        }
        return panel
    }

    private fun slot(
        tile: BlueprintPlannerTile,
        hint: PlannerSlotHint,
        x: Int,
        y: Int,
        client: Boolean,
        accepts: ((ItemStack) -> Boolean)? = null,
    ): ItemSlot {
        val slot = ModularSlot(
            tile.inventory,
            hint.index,
        ).slotGroup("planner")
        if (accepts == null) {
            slot.canPut(false)
        } else {
            slot.filter(accepts)
        }
        val widget = ItemSlot().slot(slot).left(x).top(y)
        if (client) {
            hint.attach(widget)
        }
        return widget
    }

    private fun addControls(
        panel: ModularPanel,
        tile: BlueprintPlannerTile,
        sync: PlannerSync,
    ) {
        val simulator = IPanelHandler.simple(
            panel,
            { _, _ ->
                val entry = requireNotNull(entry(tile))
                val plan = requireNotNull(sync.currentPlan())
                val selection = tile.selection
                SceneViewer.createPanel(
                    entry = entry,
                    initialValues = SceneBlueprint.channels(plan),
                    savedPlan = plan,
                    onLayoutChanged = {
                        sync.edit(
                            selection = selection,
                            plan = it,
                        )
                    },
                )
            },
            true,
        )
        panel.child(IKey.lang("iblueprints.planner.title").asWidget().left(8).top(7).color(0x404040))
        panel.child(IKey.dynamic {
            val controller = tile.plan?.let(SceneBlueprint::controller)
            controller?.displayName ?: StatCollector.translateToLocal("iblueprints.planner.no_structure")
        }.asWidget().left(8).right(8).top(21).height(12).color(0x404040))
        panel.child(
            button(
                key = "select",
                enabled = {
                    Loader.isModLoaded("NotEnoughItems")
                },
                action = {
                    PlannerRecipeSelection.open()
                },
            ).left(8).top(36).width(204).setEnabledIf {
                tile.recipe.isEmpty()
            },
        )
        panel.child(
            button(
                key = "reset",
                action = {
                    sync.action(PlannerSync.RESET)
                },
            ).left(8).top(36).width(86).setEnabledIf {
                tile.recipe.isNotEmpty()
            },
        )
        panel.child(
            button(
                key = "simulate",
                enabled = {
                    entry(tile) != null && sync.currentPlan() != null
                },
                action = {
                    if (!simulator.isPanelOpen) {
                        simulator.deleteCachedPanel()
                        simulator.openPanel()
                    }
                },
            ).left(98).top(36).width(114).setEnabledIf {
                tile.recipe.isNotEmpty()
            },
        )
        panel.child(
            button(
                key = "projector",
                enabled = tile::canSaveProjector,
                action = {
                    sync.action(PlannerSync.PROJECTOR)
                },
            ).left(58).top(60).width(154),
        )
        panel.child(
            button(
                key = "blueprint",
                enabled = tile::canSaveBlueprint,
                action = {
                    sync.action(PlannerSync.BLUEPRINT)
                },
            ).left(58).top(81).width(154).setEnabledIf {
                !tile.hasBlueprintInput()
            },
        )
        panel.child(
            button(
                key = "load_blueprint",
                action = {
                    sync.action(PlannerSync.LOAD_BLUEPRINT)
                },
            ).left(58).top(81).width(73).setEnabledIf {
                tile.hasBlueprintInput()
            },
        )
        panel.child(
            button(
                key = "rewrite_blueprint",
                enabled = tile::canSaveBlueprint,
                action = {
                    sync.action(PlannerSync.BLUEPRINT)
                },
            ).left(135).top(81).width(77).setEnabledIf {
                tile.hasBlueprintInput()
            },
        )
        panel.child(
            button(
                key = "pattern",
                enabled = tile::canSavePattern,
                action = {
                    sync.action(PlannerSync.PATTERN)
                },
            ).left(58).top(102).width(154),
        )
    }

    private fun entry(tile: BlueprintPlannerTile): SceneEntry? {
        val controller = tile.plan?.let(SceneBlueprint::controller) ?: return null
        return SceneRegistry.find(
            id = tile.recipe,
            controller = controller,
        )
    }

    private fun button(
        key: String,
        enabled: () -> Boolean = {
            true
        },
        action: () -> Unit,
    ): SceneActionButton {
        return SceneActionButton(
            label = IKey.lang("iblueprints.planner.$key").color {
                if (enabled()) {
                    0xFFFFFF
                } else {
                    0x909090
                }
            },
            action = {
                if (enabled()) {
                    action()
                }
            },
        ).height(18)
    }
}
