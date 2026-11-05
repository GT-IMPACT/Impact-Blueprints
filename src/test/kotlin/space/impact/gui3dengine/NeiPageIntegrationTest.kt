package space.impact.gui3dengine

import codechicken.nei.event.NEIRegisterHandlerInfosEvent
import codechicken.nei.PositionedStack
import codechicken.nei.recipe.IRecipeHandler
import codechicken.nei.recipe.RecipePageManager
import codechicken.nei.scroll.ScrollContainer
import codechicken.nei.recipe.GuiRecipeTab
import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.recipe.TemplateRecipeHandler
import codechicken.nei.recipe.RecipeCatalysts
import codechicken.nei.recipe.GuiCraftingRecipe
import codechicken.nei.recipe.widget.RecipeWidget
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.DummyModContainer
import cpw.mods.fml.common.LoadController
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.ModMetadata
import cpw.mods.fml.common.discovery.ASMDataTable
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.relauncher.FMLInjectionData
import cpw.mods.fml.relauncher.FMLRelaunchLog
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Bootstrap
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.launchwrapper.Launch
import net.minecraft.launchwrapper.LaunchClassLoader
import net.minecraftforge.common.MinecraftForge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.impact.gui3dengine.integration.nei.SceneCatalysts
import space.impact.gui3dengine.integration.nei.SceneHandlerInfo
import space.impact.gui3dengine.integration.nei.ScenePreviewLayout
import space.impact.gui3dengine.integration.nei.SceneRecipeHandler
import space.impact.gui3dengine.integration.nei.NEIGui3DEngineConfig
import space.impact.gui3dengine.api.SceneEntry
import space.impact.gui3dengine.api.SceneRegistry
import space.impact.gui3dengine.proxy.ClientProxy
import java.awt.Point
import java.io.File
import java.lang.reflect.Constructor
import java.net.URLClassLoader
import java.util.function.Supplier

public class NeiPageIntegrationTest {

    @Test
    public fun firstRegistrationAndReloadCoverTheWholePreview() {
        val thread = Thread.currentThread()
        val previousContext = thread.contextClassLoader
        val previousLaunch = Launch.classLoader
        val previousBlackboard = Launch.blackboard
        LaunchClassLoader((javaClass.classLoader as URLClassLoader).getURLs()).use { loader ->
            try {
                Launch.classLoader = loader
                Launch.blackboard = hashMapOf()
                thread.contextClassLoader = loader
                loader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer")
                loader.loadClass("cpw.mods.fml.common.Loader")
                    .getMethod(
                        "injectData",
                        Array<Any>::class.java,
                    )
                    .invoke(
                        null,
                        arrayOf(
                            "7",
                            "99",
                            "40",
                            "1614",
                            "1.7.10",
                            "9.05",
                            File("."),
                            arrayListOf<String>(),
                        ),
                    )
                loader.loadClass(javaClass.name).getMethod("checkWidgetBounds").invoke(null)
            } finally {
                thread.contextClassLoader = previousContext
                Launch.classLoader = previousLaunch
                Launch.blackboard = previousBlackboard
            }
        }
    }

    public companion object {

        @JvmStatic
        public fun checkWidgetBounds() {
            val side = FMLRelaunchLog::class.java.getDeclaredField("side")
            side.isAccessible = true
            side.set(
                null,
                Side.CLIENT,
            )
            val home = FMLInjectionData::class.java.getDeclaredField("minecraftHome")
            home.isAccessible = true
            home.set(
                null,
                File("."),
            )
            Bootstrap.func_151354_b()

            // NEI initializes text-field options even for layout-only tests. No display or renderer is started.
            val factoryClass = Class.forName("sun.reflect.ReflectionFactory")
            val factory = factoryClass.getMethod("getReflectionFactory").invoke(null)
            val constructor = factoryClass.getMethod(
                "newConstructorForSerialization",
                Class::class.java,
                Constructor::class.java,
            ).invoke(
                factory,
                Minecraft::class.java,
                Any::class.java.getDeclaredConstructor(),
            ) as Constructor<*>
            constructor.isAccessible = true
            val minecraft = Minecraft::class.java.getDeclaredField("theMinecraft")
            minecraft.isAccessible = true
            val client = constructor.newInstance() as Minecraft
            client.gameSettings = GameSettings()
            minecraft.set(
                null,
                client,
            )
            val controller = ItemStack(Blocks.glass)
            SceneRegistry.register(
                SceneEntry(
                    id = "test:nei_page",
                    item = controller,
                    factory = Supplier {
                        error("Layout and tooltip checks must not construct a scene")
                    },
                ),
            )

            val metadata = ModMetadata()
            metadata.modId = "NotEnoughItems"
            val namedMods = Loader::class.java.getDeclaredField("namedMods")
            namedMods.isAccessible = true
            namedMods.set(
                Loader.instance(),
                hashMapOf("NotEnoughItems" to DummyModContainer(metadata)),
            )
            val controllerField = Loader::class.java.getDeclaredField("modController")
            controllerField.isAccessible = true
            val loadController = LoadController(Loader.instance())
            controllerField.set(
                Loader.instance(),
                loadController,
            )
            val engineMetadata = ModMetadata()
            engineMetadata.modId = "gui3dengine"
            val activeContainer = LoadController::class.java.getDeclaredField("activeContainer")
            activeContainer.isAccessible = true
            activeContainer.set(
                loadController,
                DummyModContainer(engineMetadata),
            )
            // Exercise the real subscription point, before any IConfigureNEI plugin is loaded.
            ClientProxy().preInit(FMLPreInitializationEvent(
                ASMDataTable(),
                File("build/test-config"),
            ))
            repeat(2) {
                GuiRecipeTab.handlerMap.clear()
                MinecraftForge.EVENT_BUS.post(NEIRegisterHandlerInfosEvent())
                val handler = SceneRecipeHandler()
                handler.loadCraftingRecipes(controller)
                val info = GuiRecipeTab.getHandlerInfo(handler)
                assertEquals(
                    146,
                    info.height,
                )
                assertEquals(
                    166,
                    info.width,
                )
                assertFalse(info.showOverlayButton)
                assertFalse(info.showFavoritesButton)

                val widget = RecipeWidget(
                    handler,
                    listOf(0),
                )
                widget.setLocation(
                    37,
                    51,
                )
                for (y in 18 until 146) {
                    for (x in 2 until 164) {
                        assertTrue(ScenePreviewLayout.contains(
                            x = x,
                            y = y,
                        ))
                        assertTrue(widget.contains(
                            37 + x,
                            51 + y,
                        ))
                    }
                }
                assertFalse(widget.contains(
                    37,
                    51 + 146,
                ))

                var mouse = Point(
                    83,
                    100,
                )
                val gui = object : GuiRecipe<TemplateRecipeHandler>(null) {

                    override fun getCurrentRecipeHandlers(): ArrayList<TemplateRecipeHandler> {
                        return arrayListOf(handler)
                    }

                    override fun getRecipeMousePosition(recipeIndex: Int): Point {
                        return mouse
                    }
                }
                for (y in listOf(
                    18,
                    65,
                    100,
                    129,
                    134,
                    145,
                )) {
                    mouse = Point(
                        83,
                        y,
                    )
                    assertTrue(handler.mouseClicked(
                        gui = gui,
                        button = 0,
                        recipe = 0,
                    ))
                    assertEquals(
                        2,
                        handler.handleTooltip(
                            gui = gui,
                            currenttip = mutableListOf(),
                            recipe = 0,
                        ).size,
                    )
                }
                mouse = Point(
                    83,
                    146,
                )
                assertFalse(handler.mouseClicked(
                    gui = gui,
                    button = 0,
                    recipe = 0,
                ))
                assertTrue(handler.handleTooltip(
                    gui = gui,
                    currenttip = mutableListOf(),
                    recipe = 0,
                ).isEmpty())
            }
            MinecraftForge.EVENT_BUS.unregister(SceneHandlerInfo)
            checkCatalogueAndCatalysts(controller)
            FMLCommonHandler.instance().bus().unregister(SceneCatalysts)
            GregTechSceneChecks.run()
        }

        private fun checkCatalogueAndCatalysts(controller: ItemStack) {
            val otherController = ItemStack(Blocks.stone)
            SceneRegistry.register(
                SceneEntry(
                    id = "test:other_controller",
                    item = otherController,
                    factory = Supplier {
                        error("Catalogue and catalyst queries must not construct scenes")
                    },
                ),
            )
            NEIGui3DEngineConfig().loadConfig()
            RecipeCatalysts.loadCatalystInfo()
            SceneHandlerInfo.register(NEIRegisterHandlerInfosEvent())
            val handlers = GuiCraftingRecipe.craftinghandlers.filterIsInstance<SceneRecipeHandler>()
            assertEquals(
                1,
                handlers.size,
            )
            val handler = handlers.single()
            assertTrue(RecipeCatalysts.getRecipeCatalysts(handler).isEmpty())
            val catalogue = handler.allRecipeHandler as SceneRecipeHandler
            assertEquals(
                2,
                catalogue.numRecipes(),
            )
            for (item in listOf(
                controller,
                otherController,
            )) {
                val usage = handler.getUsageAndCatalystHandler(
                    inputId = "item",
                    ingredients = arrayOf(item),
                ) as SceneRecipeHandler
                val crafting = handler.getRecipeHandler(
                    "item",
                    item,
                ) as SceneRecipeHandler
                for (selected in listOf(
                    usage,
                    crafting,
                )) {
                    assertEquals(
                        1,
                        selected.numRecipes(),
                    )
                    assertEquals(
                        handler.handlerId,
                        selected.handlerId,
                    )
                    assertTrue(selected.controller(0)?.isItemEqual(item) == true)
                }
            }
            assertEquals(
                0,
                handler.getUsageAndCatalystHandler(
                    inputId = "item",
                    ingredients = arrayOf(ItemStack(Blocks.dirt)),
                ).numRecipes(),
            )
            checkPageCatalysts(catalogue)
        }

        private fun checkPageCatalysts(catalogue: SceneRecipeHandler) {
            // Exercise NEI's actual pagination; its search wrapper is package-private.
            val searchType = Class.forName("codechicken.nei.recipe.SearchRecipeHandler")
            val constructor = searchType.getDeclaredConstructor(IRecipeHandler::class.java)
            constructor.isAccessible = true
            val container = ScrollContainer()
            container.w = 166
            container.h = 600
            val pages = RecipePageManager::class.java.getConstructor(
                searchType,
                codechicken.nei.recipe.HandlerInfo::class.java,
                ScrollContainer::class.java,
            ).newInstance(
                constructor.newInstance(catalogue),
                GuiRecipeTab.getHandlerInfo(catalogue),
                container,
            )
            assertEquals(
                2,
                pages.numPages,
            )
            var noResults = false
            var currentHandler: TemplateRecipeHandler = catalogue
            val gui = object : GuiRecipe<TemplateRecipeHandler>(null) {

                override fun getCurrentRecipeHandlers(): ArrayList<TemplateRecipeHandler> {
                    return arrayListOf(catalogue)
                }

                override fun getHandler(): TemplateRecipeHandler {
                    return currentHandler
                }

                override fun getRecipeIndices(): List<Int> {
                    if (noResults) {
                        return emptyList()
                    }
                    return pages.recipeIndices
                }
            }
            val client = Minecraft.getMinecraft()
            client.currentScreen = gui
            val panel = gui.recipeCatalystWidget
            panel.setAvailableHeight(600)
            panel.x = 100 - panel.w
            panel.y = 30
            try {
                repeat(3) {
                    // GuiRecipe.refreshContainer resets the panel from its global category table every tick.
                    panel.setCatalysts(RecipeCatalysts.getRecipeCatalysts(catalogue))
                    panel.x = 100 - panel.w
                    FMLCommonHandler.instance().bus().post(TickEvent.ClientTickEvent(TickEvent.Phase.END))
                    assertTrue(panel.isShowWidget)
                    assertEquals(
                        1,
                        panel.widgets.size,
                    )
                    assertEquals(
                        100,
                        panel.x + panel.w,
                    )
                    val slot = panel.widgets.single()
                    val hovered = panel.getPositionedStackMouseOver(
                        slot.x + 8,
                        slot.y + 8,
                    )
                    val expected = catalogue.controller(pages.recipeIndices.single())
                    assertTrue(hovered != null && hovered.item.isItemEqual(expected))
                    assertTrue(RecipeCatalysts.getRecipeCatalysts(catalogue).isEmpty())
                    pages.changePage(1)
                }
                noResults = true
                FMLCommonHandler.instance().bus().post(TickEvent.ClientTickEvent(TickEvent.Phase.END))
                assertFalse(panel.isShowWidget)
                assertTrue(panel.widgets.isEmpty())
                assertEquals(
                    100,
                    panel.x + panel.w,
                )
                // A different NEI category must retain its own native catalyst list.
                currentHandler = object : TemplateRecipeHandler() {

                    override fun getRecipeName(): String {
                        return "Other category"
                    }

                    override fun getGuiTexture(): String {
                        return ""
                    }
                }
                val unrelated = PositionedStack(
                    ItemStack(Blocks.dirt),
                    0,
                    0,
                    false,
                )
                panel.setCatalysts(listOf(unrelated))
                panel.update()
                val originalSlot = panel.widgets.single()
                FMLCommonHandler.instance().bus().post(TickEvent.ClientTickEvent(TickEvent.Phase.END))
                assertTrue(panel.widgets.single() === originalSlot)
            } finally {
                client.currentScreen = null
            }
        }
    }
}
