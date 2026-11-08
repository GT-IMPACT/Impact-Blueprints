package space.impact.iblueprints

import codechicken.nei.event.NEIRegisterHandlerInfosEvent
import codechicken.nei.filter.RecipeFilter
import codechicken.nei.ItemListLoader
import codechicken.nei.PositionedStack
import codechicken.nei.PopupInputHandler
import codechicken.nei.guihook.GuiContainerManager
import codechicken.nei.recipe.IRecipeHandler
import codechicken.nei.recipe.RecipePageManager
import codechicken.nei.scroll.ScrollContainer
import codechicken.nei.recipe.GuiRecipeTab
import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.recipe.TemplateRecipeHandler
import codechicken.nei.recipe.RecipeCatalysts
import codechicken.nei.recipe.GuiCraftingRecipe
import codechicken.nei.recipe.GuiOverlayButton
import codechicken.nei.recipe.GuiRecipeButton
import codechicken.nei.recipe.RecipeHandlerRef
import codechicken.nei.recipe.widget.RecipeWidget
import com.mojang.authlib.GameProfile
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.DummyModContainer
import cpw.mods.fml.common.LoadController
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.ModMetadata
import cpw.mods.fml.common.discovery.ASMDataTable
import cpw.mods.fml.common.event.FMLLoadCompleteEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.FMLInjectionData
import cpw.mods.fml.relauncher.FMLRelaunchLog
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Bootstrap
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.launchwrapper.Launch
import net.minecraft.launchwrapper.LaunchClassLoader
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.impact.iblueprints.integration.nei.SceneCatalysts
import space.impact.iblueprints.integration.nei.SceneHandlerInfo
import space.impact.iblueprints.integration.nei.ScenePreviewLayout
import space.impact.iblueprints.integration.nei.SceneRecipeHandler
import space.impact.iblueprints.integration.nei.NEIImpactBlueprintsConfig
import space.impact.iblueprints.integration.nei.BlueprintViewInput
import space.impact.iblueprints.integration.nei.PlannerRecipeSelection
import space.impact.iblueprints.item.StructureBlueprintItem
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.proxy.ClientProxy
import space.impact.iblueprints.planner.BlueprintPlannerTile
import space.impact.iblueprints.planner.PlannerContainer
import space.impact.iblueprints.planner.PlannerSync
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.ScenePosition
import space.impact.iblueprints.scene.world.SceneWorld
import java.awt.Point
import java.io.File
import java.lang.reflect.Constructor
import java.net.URLClassLoader
import java.util.UUID
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
                        val scene = BlockScene("First structure")
                        for (x in 0..1) {
                            scene.block(
                                position = ScenePosition(
                                    x = x,
                                    y = 64,
                                    z = 0,
                                ),
                                block = Blocks.wool,
                                metadata = 4,
                            )
                        }
                        scene
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
            engineMetadata.modId = "iblueprints"
            val engineContainer = DummyModContainer(engineMetadata)
            val activeContainer = LoadController::class.java.getDeclaredField("activeContainer")
            activeContainer.isAccessible = true
            activeContainer.set(
                loadController,
                engineContainer,
            )
            // Exercise the real subscription point, before any IConfigureNEI plugin is loaded.
            val preInit = FMLPreInitializationEvent(
                ASMDataTable(),
                File("build/test-config"),
            )
            preInit.applyModContainer(engineContainer)
            ClientProxy().preInit(preInit)
            assertTrue(GameRegistry.findItem(
                "iblueprints",
                "structure_blueprint",
            ) === StructureBlueprintItem)
            val getPermutations = ItemListLoader::class.java.getDeclaredMethod(
                "getPermutations",
                Item::class.java,
            )
            getPermutations.isAccessible = true
            val blueprintVariants = getPermutations.invoke(
                null,
                StructureBlueprintItem,
            ) as List<*>
            assertTrue((blueprintVariants.single() as ItemStack).item === StructureBlueprintItem)
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
            checkPlannerSelection(controller)
            MinecraftForge.EVENT_BUS.unregister(SceneHandlerInfo)
            checkCatalogueAndCatalysts(controller)
            NeiMaterialIndexChecks.run()
            FMLCommonHandler.instance().bus().unregister(SceneCatalysts)
            val serverPlayerConstructor = factoryClass.getMethod(
                "newConstructorForSerialization",
                Class::class.java,
                Constructor::class.java,
            ).invoke(
                factory,
                EntityPlayerMP::class.java,
                EntityPlayer::class.java.getConstructor(
                    World::class.java,
                    GameProfile::class.java,
                ),
            ) as Constructor<*>
            val serverPlayer = serverPlayerConstructor.newInstance(
                SceneWorld(),
                GameProfile(
                    UUID.randomUUID(),
                    "BlueprintSourceTest",
                ),
            ) as EntityPlayerMP
            GregTechSceneChecks.run(serverPlayer)
        }

        private fun checkPlannerSelection(controller: ItemStack) {
            val plannerTile = BlueprintPlannerTile()
            val plannerSync = PlannerSync(
                state = plannerTile.planner,
                accessible = plannerTile::accessible,
            )
            val planner = object : GuiContainer(PlannerContainer(plannerSync)) {

                override fun drawGuiContainerBackgroundLayer(
                    partialTicks: Float,
                    mouseX: Int,
                    mouseY: Int,
                ) {
                }
            }
            val browsing = SceneRecipeHandler()
            browsing.loadCraftingRecipes(controller)
            val selecting = SceneRecipeHandler()
            selecting.loadCraftingRecipes(PlannerRecipeSelection.QUERY)
            for (handler in listOf(
                browsing,
                selecting,
            )) {
                val gui = object : GuiRecipe<TemplateRecipeHandler>(planner) {

                    override fun getCurrentRecipeHandlers(): ArrayList<TemplateRecipeHandler> {
                        return arrayListOf(handler)
                    }
                }
                val event = GuiRecipeButton.UpdateRecipeButtonsEvent.Post(
                    gui,
                    RecipeHandlerRef.of(
                        handler,
                        0,
                    ),
                    ScenePreviewLayout.WIDTH,
                    ScenePreviewLayout.HEIGHT,
                    mutableListOf(),
                )
                MinecraftForge.EVENT_BUS.post(event)
                if (handler === selecting) {
                    val button = event.buttonList.single() as GuiOverlayButton
                    assertTrue(button.enabled && button.canFillCraftingGrid())
                    assertEquals("?", button.displayString)
                    assertTrue(handler.getResultStack(0).item.isItemEqual(controller))
                } else {
                    assertTrue(event.buttonList.isEmpty())
                }
            }
        }

        private fun checkCatalogueAndCatalysts(controller: ItemStack) {
            val otherController = ItemStack(Blocks.stone)
            SceneRegistry.register(
                SceneEntry(
                    id = "test:other_controller",
                    item = otherController,
                    factory = Supplier {
                        BlockScene("Second structure").block(
                            position = ScenePosition(
                                x = 0,
                                y = 64,
                                z = 0,
                            ),
                            block = Blocks.wool,
                            metadata = 4,
                        ).block(
                            position = ScenePosition(
                                x = 1,
                                y = 64,
                                z = 0,
                            ),
                            block = Blocks.planks,
                        )
                    },
                ),
            )
            ClientProxy().loadComplete(FMLLoadCompleteEvent())
            while (SceneCatalysts.advanceIndex()) {
                // Drain the client-tick work without starting Minecraft's main loop.
            }
            val popup = PopupInputHandler()
            GuiContainerManager.addInputHandler(popup)
            try {
                NEIImpactBlueprintsConfig().loadConfig()
                assertTrue(
                    GuiContainerManager.inputHandlers.indexOf(BlueprintViewInput) in
                        0 until GuiContainerManager.inputHandlers.indexOf(popup),
                )
            } finally {
                GuiContainerManager.inputHandlers.remove(popup)
            }
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
            val filter = RecipeFilter(
                RecipeFilter.FilterContext.ANY,
                true,
            ) { stack ->
                stack.isItemEqual(otherController)
            }
            val matched = (0 until catalogue.numRecipes()).filter {
                filter.matches(
                    catalogue,
                    it,
                )
            }
            val result = catalogue.getResultStack(matched.single())
            assertTrue(result.item.isItemEqual(otherController))
            assertTrue(result is PositionedStack.Placeholder)
            assertFalse(result.contains(
                result.relx,
                result.rely,
            ))
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
            val sharedMaterial = ItemStack(
                Blocks.wool,
                1,
                4,
            )
            assertEquals(
                2,
                handler.getUsageAndCatalystHandler(
                    inputId = "item",
                    ingredients = arrayOf(sharedMaterial),
                ).numRecipes(),
            )
            val planksUsage = handler.getUsageHandler(
                "item",
                ItemStack(Blocks.planks),
            ) as SceneRecipeHandler
            assertEquals(
                1,
                planksUsage.numRecipes(),
            )
            assertTrue(planksUsage.controller(0)?.isItemEqual(otherController) == true)
            assertEquals(
                0,
                handler.getUsageHandler(
                    "item",
                    ItemStack(Blocks.wool),
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
                    val recipe = pages.recipeIndices.single()
                    val expected = catalogue.controller(recipe)
                    val expectedCount = if (recipe == 0) {
                        2
                    } else {
                        3
                    }
                    assertEquals(
                        expectedCount,
                        panel.widgets.size,
                    )
                    assertEquals(
                        100,
                        panel.x + panel.w,
                    )
                    val slot = panel.widgets.first()
                    val hovered = panel.getPositionedStackMouseOver(
                        slot.x + 8,
                        slot.y + 8,
                    )
                    assertTrue(hovered != null && hovered.item.isItemEqual(expected))
                    val materialSlot = panel.widgets[1]
                    val material = panel.getPositionedStackMouseOver(
                        materialSlot.x + 8,
                        materialSlot.y + 8,
                    )
                    assertTrue(material?.item?.itemDamage == 4)
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
