package space.impact.iblueprints.integration.nei

import codechicken.lib.gui.GuiDraw
import codechicken.nei.PositionedStack
import codechicken.nei.api.IOverlayHandler
import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.recipe.IUsageHandler
import codechicken.nei.recipe.TemplateRecipeHandler
import cpw.mods.fml.common.Loader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.StatCollector
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.integration.gregtech.GregTechScenes
import space.impact.iblueprints.integration.modularui.SceneViewer
import space.impact.iblueprints.planner.PlannerContainer
import space.impact.iblueprints.scene.SceneView

/** NEI hosts only a static engine preview. Interaction belongs to the full-screen viewer. */
public class SceneRecipeHandler : TemplateRecipeHandler() {

    private val entries: MutableList<SceneEntry> = mutableListOf()
    private var usageStack: ItemStack? = null
    private var view: SceneView? = null
    private var activeRecipe: Int = -1
    private var choosingForPlanner: Boolean = false

    internal fun controller(recipe: Int): ItemStack? {
        return entries.getOrNull(recipe)?.item()
    }

    internal fun catalysts(recipe: Int): List<PositionedStack> {
        val controller = controller(recipe) ?: return emptyList()
        val gregTechLoaded = Loader.isModLoaded("gregtech")
        val materials = prepare(recipe)?.materials.orEmpty().map {
            it.stack
        }.filterNot {
            val sameController = it.isItemEqual(controller) && ItemStack.areItemStackTagsEqual(
                it,
                controller,
            )
            sameController || gregTechLoaded && GregTechScenes.isHatch(it)
        }
        return (listOf(controller) + materials).map {
            PositionedStack(
                it.copy(),
                0,
                0,
                false,
            )
        }
    }

    override fun getRecipeHeight(recipe: Int): Int {
        return ScenePreviewLayout.HEIGHT
    }

    override fun getRecipeName(): String {
        return StatCollector.translateToLocal("iblueprints.title")
    }

    override fun getGuiTexture(): String {
        return "minecraft:textures/gui/container/crafting_table.png"
    }

    override fun loadCraftingRecipes(result: ItemStack) {
        addEntries(SceneRegistry.matching(result))
    }

    override fun loadUsageRecipes(ingredient: ItemStack) {
        usageStack = ingredient.copy()
        addEntries((SceneRegistry.matching(ingredient) + SceneCatalysts.matching(ingredient)).distinct())
    }

    private fun materialUsage(entry: SceneEntry): SceneMaterialUsage? {
        val stack = usageStack ?: return null
        return SceneCatalysts.usage(
            entry = entry,
            stack = stack,
        )
    }

    override fun getUsageAndCatalystHandler(
        inputId: String,
        vararg ingredients: Any,
    ): IUsageHandler {
        // Match controllers and materials to individual scenes instead of opening the whole catalogue.
        return getUsageHandler(
            inputId,
            *ingredients,
        )
    }

    override fun loadCraftingRecipes(
        outputId: String,
        vararg results: Any,
    ) {
        if (outputId == PlannerRecipeSelection.QUERY) {
            choosingForPlanner = true
            addEntries(SceneRegistry.all())
        } else if (outputId == "iblueprints.scenes" || outputId == "all") {
            addEntries(SceneRegistry.all())
        } else {
            super.loadCraftingRecipes(
                outputId,
                *results,
            )
        }
    }

    override fun hasOverlay(
        gui: GuiContainer,
        container: Container,
        recipe: Int,
    ): Boolean {
        return choosingForPlanner && container is PlannerContainer && recipe in entries.indices
    }

    override fun getOverlayHandler(
        gui: GuiContainer,
        recipe: Int,
    ): IOverlayHandler? {
        if (!hasOverlay(
                gui,
                gui.inventorySlots,
                recipe,
            )
        ) {
            return null
        }
        return PlannerRecipeSelection(entries[recipe])
    }

    private fun addEntries(matches: List<SceneEntry>) {
        for (entry in matches) {
            entries.add(entry)
            arecipes.add(object : CachedRecipe() {

                override fun getResult(): PositionedStack? {
                    if (!choosingForPlanner) {
                        return null
                    }
                    return PositionedStack(
                        entry.item(),
                        3,
                        ScenePreviewLayout.HEIGHT - 16,
                    )
                }
            })
        }
    }

    private fun prepare(recipe: Int): SceneView? {
        if (activeRecipe == recipe) {
            return view
        }
        view?.scene?.close()
        activeRecipe = recipe
        view = try {
            val entry = entries[recipe]
            val usage = materialUsage(entry)
            SceneView(entry.build(usage?.values.orEmpty())).also {
                it.highlight(usage?.positions.orEmpty())
            }
        } catch (failure: Throwable) {
            if (failure !is Exception && failure !is LinkageError) {
                throw failure
            }
            LogManager.getLogger("ImpactBlueprints").error(
                "Cannot create scene ${entries[recipe].id}",
                failure,
            )
            null
        }
        return view
    }

    override fun drawBackground(recipe: Int) {
        val gui = Minecraft.getMinecraft().currentScreen as? GuiRecipe<*> ?: return
        SceneCatalysts.update(
            gui = gui,
            recipe = recipe,
        )
        Gui.drawRect(
            0,
            0,
            ScenePreviewLayout.WIDTH,
            ScenePreviewLayout.HEIGHT,
            0xFFCECECE.toInt(),
        )
        val active = prepare(recipe)
        if (active == null) {
            GuiDraw.drawString(
                StatCollector.translateToLocal("iblueprints.error"),
                3,
                20,
                0xAA0000,
                false,
            )
            return
        }
        val mouse = GuiDraw.getMousePosition()
        val local = gui.getRecipeMousePosition(recipe)
        active.render(
            x = mouse.x - local.x + ScenePreviewLayout.PREVIEW_X,
            y = mouse.y - local.y + ScenePreviewLayout.PREVIEW_Y,
            width = ScenePreviewLayout.PREVIEW_WIDTH,
            height = ScenePreviewLayout.PREVIEW_HEIGHT,
            mouseX = Int.MIN_VALUE,
            mouseY = Int.MIN_VALUE,
        )
    }

    override fun drawForeground(recipe: Int) {
        val title = entries[recipe].item().displayName
        val font = GuiDraw.fontRenderer
        val visibleTitle = if (font.getStringWidth(title) > ScenePreviewLayout.TITLE_WIDTH) {
            val suffix = "${EnumChatFormatting.RESET}..."
            val prefix = font.trimStringToWidth(
                title,
                ScenePreviewLayout.TITLE_WIDTH - font.getStringWidth(suffix),
            ).trimEnd()
            prefix + suffix
        } else {
            title
        }
        GuiDraw.drawString(
            visibleTitle,
            ScenePreviewLayout.TITLE_X,
            ScenePreviewLayout.TITLE_Y,
            0x202020,
            false,
        )
        GuiDraw.drawStringC(
            font.trimStringToWidth(
                viewerHint(),
                ScenePreviewLayout.WIDTH - if (choosingForPlanner) 44 else 6,
            ),
            ScenePreviewLayout.WIDTH / 2,
            ScenePreviewLayout.FOOTER_Y,
            0x404040,
            false,
        )
    }

    override fun mouseClicked(
        gui: GuiRecipe<*>,
        button: Int,
        recipe: Int,
    ): Boolean {
        val local = gui.getRecipeMousePosition(recipe)
        val inside = ScenePreviewLayout.contains(
            x = local.x,
            y = local.y,
        )
        if (button != 0 || !inside ||
            choosingForPlanner && local.y >= ScenePreviewLayout.FOOTER_Y && local.x !in 20..145
        ) {
            return false
        }
        if (Loader.isModLoaded("modularui2")) {
            val entry = entries[recipe]
            val usage = materialUsage(entry)
            try {
                SceneViewer.open(
                    entry = entry,
                    initialValues = usage?.values.orEmpty(),
                    highlighted = usage?.positions.orEmpty(),
                )
            } catch (failure: LinkageError) {
                LogManager.getLogger("ImpactBlueprints").warn("Cannot open scene ${entry.id}: $failure")
            }
        }
        return true
    }

    override fun handleTooltip(
        gui: GuiRecipe<*>,
        currenttip: MutableList<String>,
        recipe: Int,
    ): MutableList<String> {
        val local = gui.getRecipeMousePosition(recipe)
        val inside = ScenePreviewLayout.contains(
            x = local.x,
            y = local.y,
        )
        val overTitle = local.x >= ScenePreviewLayout.TITLE_X &&
            local.x < ScenePreviewLayout.TITLE_X + ScenePreviewLayout.TITLE_WIDTH &&
            local.y in ScenePreviewLayout.TITLE_Y until ScenePreviewLayout.PREVIEW_Y
        if (overTitle || inside) {
            currenttip.add(entries[recipe].item().displayName)
        }
        if (inside) {
            currenttip.add(viewerHint())
        }
        return currenttip
    }

    private fun viewerHint(): String {
        val key = if (Loader.isModLoaded("modularui2")) {
            "open_viewer"
        } else {
            "requires_modularui"
        }
        return StatCollector.translateToLocal("iblueprints.$key")
    }
}
