package space.impact.iblueprints.integration.modularui

import com.cleanroommc.modularui.api.drawable.IKey
import com.cleanroommc.modularui.api.layout.IViewport
import com.cleanroommc.modularui.api.layout.IViewportStack
import com.cleanroommc.modularui.api.widget.IWidget
import com.cleanroommc.modularui.utils.HoveredWidgetList
import com.cleanroommc.modularui.widgets.ListWidget
import cpw.mods.fml.common.Loader
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.StatCollector
import space.impact.iblueprints.api.SceneAlternativeGroups
import space.impact.iblueprints.integration.gregtech.GregTechHatchGroups
import space.impact.iblueprints.planner.logPlannerTrace
import space.impact.iblueprints.planner.tracePlannerStep
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.SceneView

internal class SceneAlternativeList(
    private val view: SceneView,
    private val query: () -> String,
    private val onChanged: () -> Unit,
) : ListWidget<IWidget, SceneAlternativeList>() {

    private var lastScene = view.scene
    private var lastPositions = view.inspectedPositions()
    private var lastMaterials = view.materials
    private var lastQuery = query().trim()

    init {
        rebuild()
    }

    override fun onUpdate() {
        super.onUpdate()
        val positions = view.inspectedPositions()
        val search = query().trim()
        val selectionChanged = lastScene !== view.scene || lastPositions != positions
        val searchChanged = lastQuery != search
        if (selectionChanged || searchChanged || lastMaterials !== view.materials) {
            lastScene = view.scene
            lastPositions = positions
            lastMaterials = view.materials
            lastQuery = search
            if (selectionChanged || searchChanged) {
                scrollData.scrollTo(
                    scrollArea,
                    0,
                )
            }
            removeAll()
            rebuild()
        }
    }

    override fun layoutWidgets(): Boolean {
        if (!super.layoutWidgets()) {
            return false
        }
        if (!hasChildren()) {
            // ListWidget skips updating the content height when the last row is removed.
            scrollData.scrollSize = 0
        }
        scrollData.clamp(scrollArea)
        return true
    }

    override fun getWidgetsAt(
        stack: IViewportStack,
        widgets: HoveredWidgetList,
        x: Int,
        y: Int,
    ) {
        if (widgets.peek() !== this) {
            return
        }
        // The scrollbar is fixed to the viewport and does not follow the scrolled rows.
        val localX = stack.unTransformX(
            x.toFloat(),
            y.toFloat(),
        )
        val localY = stack.unTransformY(
            x.toFloat(),
            y.toFloat(),
        ) - scrollY
        if (!scrollData.isInsideScrollbarArea(
                scrollArea,
                localX,
                localY,
            )
        ) {
            IViewport.getChildrenAt(
                this,
                stack,
                widgets,
                x,
                y,
            )
        }
    }

    private fun rebuild() {
        val positions = lastPositions
        if (positions.isEmpty()) {
            return
        }
        val stacks = tracePlannerStep(
            step = "viewer.alternatives",
            details = "structure=${view.scene.title} positions=${positions.size}",
        ) {
            view.commonAlternatives(positions)
        }
        val sorted = if (Loader.isModLoaded("gregtech")) {
            stacks.sortedBy(GregTechHatchGroups::tier)
        } else {
            stacks
        }
        val groups = sorted.groupBy(SceneAlternativeGroups::group)
        val words = lastQuery.split(WHITESPACE).filter(String::isNotEmpty)
        var hasResults = false
        for ((key, alternatives) in groups.entries.sortedWith(
            compareBy<Map.Entry<String, List<ItemStack>>> { it.key == SceneAlternativeGroups.OTHER }.thenBy {
                StatCollector.translateToLocal(it.key)
            },
        )) {
            val visible = if (words.isEmpty()) {
                alternatives
            } else {
                val title = StatCollector.translateToLocal(key)
                alternatives.filter { stack ->
                    val name = EnumChatFormatting.getTextWithoutFormattingCodes(stack.displayName)
                    val searchText = "$name $title"
                    words.all {
                        searchText.contains(
                            it,
                            ignoreCase = true,
                        )
                    }
                }
            }
            if (visible.isEmpty()) {
                continue
            }
            hasResults = true
            child(
                IKey.lang(key).asWidget()
                    .widthRelOffset(
                        1f,
                        -7,
                    )
                    .paddingBottom(3)
                    .color(0xB9C0CA),
            )
            addGroup(
                positions = positions,
                stacks = visible,
            )
        }
        if (!hasResults && (words.isNotEmpty() || positions.size > 1)) {
            val key = if (stacks.isEmpty() && positions.size > 1) {
                "iblueprints.variants.no_common"
            } else {
                "iblueprints.variants.no_results"
            }
            child(IKey.lang(key).asWidget().widthRel(1f).color(0xB9C0CA))
        }
        if (positions.any { view.scene.elements[it]?.hatchChoicesIncomplete == true }) {
            child(IKey.lang("iblueprints.variants.incomplete").asWidget().widthRel(1f).height(28).color(0xFFD166))
        }
    }

    private fun addGroup(
        positions: Set<ScenePosition>,
        stacks: List<ItemStack>,
    ) {
        val scene = view.scene
        val singlePosition = positions.singleOrNull()
        val rows = stacks.chunked(5)
        for ((rowIndex, rowStacks) in rows.withIndex()) {
            val height = if (rowIndex == rows.lastIndex) {
                28
            } else {
                22
            }
            val row = SceneMaterialRow().height(height).widthRel(1f)
            for ((index, stack) in rowStacks.withIndex()) {
                val canReplace = singlePosition == null || scene.simulation?.canReplace(
                    position = singlePosition,
                    stack = stack,
                ) == true
                val replace: (() -> Boolean)? = if (canReplace) {
                    replace@{
                        if (view.scene !== scene) {
                            return@replace false
                        }
                        val item = Item.itemRegistry.getNameForObject(stack.item)
                        val details = "structure=${scene.title} positions=${positions.size} " +
                            "item=$item:${stack.itemDamage}"
                        val changed = tracePlannerStep(
                            step = "viewer.replace",
                            details = details,
                        ) {
                            view.replaceSelection(
                                positions = positions,
                                stack = stack,
                            )
                        }
                        logPlannerTrace("viewer.replace result changed=$changed $details")
                        if (changed) {
                            onChanged()
                        }
                        changed
                    }
                } else {
                    null
                }
                row.child(
                    SceneMaterialSlot(
                        stack = stack,
                        replace = replace,
                    ).left(index * 22).top(0).size(20),
                )
            }
            child(row)
        }
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
