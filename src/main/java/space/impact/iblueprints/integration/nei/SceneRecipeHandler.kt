package space.impact.iblueprints.integration.nei

import codechicken.lib.gui.GuiDraw
import codechicken.nei.PositionedStack
import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.recipe.IUsageHandler
import codechicken.nei.recipe.TemplateRecipeHandler
import cpw.mods.fml.common.Loader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
import org.apache.logging.log4j.LogManager
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.integration.modularui.SceneViewer
import space.impact.iblueprints.scene.SceneView

/** NEI hosts only a static engine preview. Interaction belongs to the full-screen viewer. */
public class SceneRecipeHandler : TemplateRecipeHandler() {

    private val entries: MutableList<SceneEntry> = mutableListOf()
    private var view: SceneView? = null
    private var activeRecipe: Int = -1

    internal fun controller(recipe: Int): ItemStack? {
        return entries.getOrNull(recipe)?.item()
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
        addEntries(SceneRegistry.matching(ingredient))
    }

    override fun getUsageAndCatalystHandler(
        inputId: String,
        vararg ingredients: Any,
    ): IUsageHandler {
        // A controller selects its own structure; NEI's default catalyst path would open the whole catalogue.
        return getUsageHandler(
            inputId,
            *ingredients,
        )
    }

    override fun loadCraftingRecipes(
        outputId: String,
        vararg results: Any,
    ) {
        if (outputId == "iblueprints.scenes" || outputId == "all") {
            addEntries(SceneRegistry.all())
        } else {
            super.loadCraftingRecipes(
                outputId,
                *results,
            )
        }
    }

    private fun addEntries(matches: List<SceneEntry>) {
        for (entry in matches) {
            entries.add(entry)
            arecipes.add(object : CachedRecipe() {

                override fun getResult(): PositionedStack? {
                    return null
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
            SceneView(entries[recipe].build())
        } catch (failure: Exception) {
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
        val font = Minecraft.getMinecraft().fontRenderer
        GuiDraw.drawString(
            font.trimStringToWidth(
                title,
                ScenePreviewLayout.WIDTH - 6,
            ),
            3,
            3,
            0x202020,
            false,
        )
        GuiDraw.drawStringC(
            font.trimStringToWidth(
                viewerHint(),
                ScenePreviewLayout.WIDTH - 6,
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
        if (button != 0 || !inside) {
            return false
        }
        if (Loader.isModLoaded("modularui2")) {
            SceneViewer.open(entries[recipe])
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
        if (inside) {
            currenttip.add(entries[recipe].item().displayName)
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
