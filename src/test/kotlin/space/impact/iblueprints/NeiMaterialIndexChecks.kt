package space.impact.iblueprints

import java.util.function.Function
import java.util.function.IntUnaryOperator
import java.util.function.Supplier
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import space.impact.iblueprints.api.SceneChannel
import space.impact.iblueprints.api.SceneEntry
import space.impact.iblueprints.api.SceneRegistry
import space.impact.iblueprints.integration.nei.SceneCatalysts
import space.impact.iblueprints.integration.nei.SceneRecipeHandler
import space.impact.iblueprints.scene.BlockScene
import space.impact.iblueprints.scene.SceneElement
import space.impact.iblueprints.scene.ScenePosition

internal object NeiMaterialIndexChecks {

    internal fun run() {
        val position = ScenePosition(
            x = 0,
            y = 64,
            z = 0,
        )
        val firstController = ItemStack(Blocks.brick_block)
        val laterController = ItemStack(Blocks.gold_block)
        val casing = ItemStack(
            Blocks.wool,
            1,
            11,
        )
        val laterCasing = ItemStack(
            Blocks.stained_hardened_clay,
            1,
            2,
        )
        val alternative = ItemStack(Blocks.emerald_block)
        var inspectedAlternatives = false
        SceneRegistry.register(SceneEntry(
            id = "test:material_index_first",
            item = firstController,
            factory = Supplier {
                val scene = BlockScene("Early structure").block(
                    position = position,
                    block = Blocks.wool,
                    metadata = casing.itemDamage,
                )
                scene.elements[position] = SceneElement(
                    requiredAir = false,
                    unresolved = false,
                    choices = listOf(casing),
                    channels = emptyList(),
                ).apply {
                    hatchChoices = {
                        // Queries must work before expensive hatch enumeration has finished, even for later scenes.
                        assertUsage(
                            material = casing,
                            controller = firstController,
                        )
                        assertUsage(
                            material = laterCasing,
                            controller = laterController,
                        )
                        inspectedAlternatives = true
                        listOf(alternative)
                    }
                }
                scene
            },
        ))
        SceneRegistry.register(SceneEntry(
            id = "test:material_index_later",
            item = laterController,
            factory = Supplier {
                error("Configured factory required")
            },
            channels = listOf(SceneChannel(
                id = "tier",
                label = "Tier",
                description = "Casing tier",
                minimum = 1,
                maximum = 3,
                normalizer = IntUnaryOperator.identity(),
            )),
            configuredFactory = Function { values ->
                if (values["tier"] == 2) {
                    assertUsage(
                        material = alternative,
                        controller = firstController,
                    )
                }
                if (values["tier"] == 3) {
                    assertUsage(
                        material = ItemStack(
                            Blocks.stained_hardened_clay,
                            1,
                            3,
                        ),
                        controller = laterController,
                    )
                }
                BlockScene("Later structure").block(
                    position = position,
                    block = Blocks.stained_hardened_clay,
                    metadata = values.getValue("tier") + 1,
                )
            },
        ))
        SceneCatalysts.index()
        val preview = SceneRecipeHandler()
        preview.loadCraftingRecipes(firstController)
        preview.catalysts(0)
        assertUsage(
            material = casing,
            controller = firstController,
        )
        while (SceneCatalysts.advanceIndex()) {
            // Finish the same work normally resumed by client ticks.
        }
        assertTrue(inspectedAlternatives)
        assertUsage(
            material = ItemStack(
                Blocks.stained_hardened_clay,
                1,
                4,
            ),
            controller = laterController,
        )
        val entry = SceneRegistry.all().single {
            it.id == "test:material_index_later"
        }
        val usage = requireNotNull(SceneCatalysts.usage(
            entry = entry,
            stack = laterCasing,
        ))
        assertEquals(
            mapOf("tier" to 1),
            usage.values,
        )
        assertEquals(
            setOf(position),
            usage.positions,
        )
    }

    private fun assertUsage(
        material: ItemStack,
        controller: ItemStack,
    ) {
        val handler = SceneRecipeHandler().getUsageHandler(
            "item",
            material,
        ) as SceneRecipeHandler
        assertTrue(
            "Material ${material.itemDamage} must be searchable before alternative enumeration completes",
            (0 until handler.numRecipes()).any {
                handler.controller(it)?.isItemEqual(controller) == true
            },
        )
    }
}
