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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.integration.modularui.SceneActionButton
import space.impact.iblueprints.integration.modularui.SceneViewer
import space.impact.iblueprints.integration.nei.PlannerRecipeSelection
import space.impact.iblueprints.integration.structures.SceneBlueprint

internal object BlueprintPlannerGui {

    internal fun build(
        state: PlannerState,
        manager: PanelSyncManager,
        settings: UISettings,
        accessible: (EntityPlayer) -> Boolean,
    ): ModularPanel {
        val sync = bind(
            state = state,
            manager = manager,
            settings = settings,
            accessible = accessible,
        )
        val panel = ModularPanel.defaultPanel(
            "blueprint_planner",
            220,
            192,
        )
        val client = manager.isClient
        panel.child(SlotGroupWidget.playerInventory(true))
        panel.child(
            slot(
                state = state,
                hint = PlannerSlotHint.PROJECTOR,
                x = 8,
                y = 60,
                client = client,
                accepts = PlannerState::acceptsProjector,
            ),
        )
        panel.child(
            slot(
                state = state,
                hint = PlannerSlotHint.BLUEPRINT_INPUT,
                x = 8,
                y = 81,
                client = client,
                accepts = PlannerState::acceptsBlueprintInput,
            ),
        )
        panel.child(
            slot(
                state = state,
                hint = PlannerSlotHint.BLUEPRINT,
                x = 32,
                y = 81,
                client = client,
                accepts = PlannerState::acceptsRecordedBlueprint,
            ),
        )
        if (client) {
            addControls(
                panel = panel,
                state = state,
                sync = sync.session,
            )
        }
        return panel
    }

    internal fun bind(
        state: PlannerState,
        manager: PanelSyncManager,
        settings: UISettings,
        accessible: (EntityPlayer) -> Boolean,
    ): PlannerSync {
        val sync = PlannerSync(
            state = state,
            accessible = accessible,
        )
        manager.syncValue(
            "planner",
            0,
            sync.allowC2S(),
        )
        settings.customContainer {
            PlannerContainer(sync)
        }
        settings.canInteractWith(accessible)
        manager.registerSlotGroup(
            "planner",
            1,
        )
        return sync
    }

    internal fun simulator(
        panel: ModularPanel,
        state: PlannerState,
        sync: PlannerSession,
    ): IPanelHandler {
        return IPanelHandler.simple(
            panel,
            { _, _ ->
                val entry = requireNotNull(entry(state))
                val plan = requireNotNull(sync.currentPlan())
                val selection = state.selection
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
    }

    internal fun slot(
        state: PlannerState,
        hint: PlannerSlotHint,
        x: Int,
        y: Int,
        client: Boolean,
        accepts: ((ItemStack) -> Boolean)? = null,
    ): ItemSlot {
        val slot = ModularSlot(
            state.inventory,
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
        state: PlannerState,
        sync: PlannerSession,
    ) {
        val simulator = simulator(
            panel = panel,
            state = state,
            sync = sync,
        )
        panel.child(IKey.lang("iblueprints.planner.title").asWidget().left(8).top(7).color(0x404040))
        panel.child(IKey.dynamic {
            val controller = state.plan?.let(SceneBlueprint::controller)
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
                state.recipe.isEmpty()
            },
        )
        panel.child(
            button(
                key = "reset",
                action = {
                    sync.action(PlannerSession.RESET)
                },
            ).left(8).top(36).width(86).setEnabledIf {
                state.recipe.isNotEmpty()
            },
        )
        panel.child(
            button(
                key = "simulate",
                enabled = {
                    entry(state) != null && sync.currentPlan() != null
                },
                action = {
                    if (!simulator.isPanelOpen) {
                        simulator.deleteCachedPanel()
                        simulator.openPanel()
                    }
                },
            ).left(98).top(36).width(114).setEnabledIf {
                state.recipe.isNotEmpty()
            },
        )
        panel.child(
            button(
                key = "projector",
                enabled = state::canSaveProjector,
                action = {
                    sync.action(PlannerSession.PROJECTOR)
                },
            ).left(58).top(60).width(154),
        )
        panel.child(
            button(
                key = "blueprint",
                enabled = state::canSaveBlueprint,
                action = {
                    sync.action(PlannerSession.BLUEPRINT)
                },
            ).left(58).top(81).width(154).setEnabledIf {
                !state.hasBlueprintInput()
            },
        )
        panel.child(
            button(
                key = "load_blueprint",
                action = {
                    sync.action(PlannerSession.LOAD_BLUEPRINT)
                },
            ).left(58).top(81).width(73).setEnabledIf {
                state.hasBlueprintInput()
            },
        )
        panel.child(
            button(
                key = "rewrite_blueprint",
                enabled = state::canSaveBlueprint,
                action = {
                    sync.action(PlannerSession.BLUEPRINT)
                },
            ).left(135).top(81).width(77).setEnabledIf {
                state.hasBlueprintInput()
            },
        )
    }

    internal fun entry(state: PlannerState): SceneEntry? {
        val controller = state.plan?.let(SceneBlueprint::controller) ?: return null
        return SceneRegistry.find(
            id = state.recipe,
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
