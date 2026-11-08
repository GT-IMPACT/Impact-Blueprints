package space.impact.iblueprints

import java.io.ByteArrayInputStream
import java.util.function.Predicate
import net.minecraft.item.ItemStack
import net.minecraft.util.StringTranslate
import org.junit.Assert.assertEquals
import space.impact.iblueprints.api.SceneAlternativeGroups

internal object SceneAlternativeGroupPriorityChecks {

    internal fun run(fluidOutput: ItemStack) {
        for (role in listOf(
            "casings",
            "fluid_input",
            "fluid_output",
            "item_input",
            "item_output",
            "combined_input",
            "energy_input",
            "energy_output",
            "maintenance",
            "mufflers",
            "data_access",
        )) {
            assertEquals(
                1,
                SceneAlternativeGroups.priority("iblueprints.variants.group.$role"),
            )
        }
        assertEquals(
            Int.MAX_VALUE,
            SceneAlternativeGroups.priority(SceneAlternativeGroups.OTHER),
        )
        val legacyRegistration = SceneAlternativeGroups::class.java.getMethod(
            "register",
            String::class.java,
            Predicate::class.java,
        )
        legacyRegistration.invoke(
            null,
            "test.group_priority.legacy",
            Predicate<ItemStack> {
                false
            },
        )
        assertEquals(
            Int.MAX_VALUE,
            SceneAlternativeGroups.priority("test.group_priority.legacy"),
        )
        SceneAlternativeGroups::class.java.getMethod(
            "register",
            String::class.java,
            Int::class.javaPrimitiveType,
            Predicate::class.java,
        )
        SceneAlternativeGroups.register(
            translationKey = "test.group_priority.first",
            priority = Int.MIN_VALUE,
            matches = Predicate {
                false
            },
        )
        SceneAlternativeGroups.register(
            translationKey = "test.group_priority.a",
            priority = 4,
            matches = Predicate {
                false
            },
        )
        SceneAlternativeGroups.register(
            translationKey = "test.group_priority.a",
            priority = 1,
            matches = Predicate {
                false
            },
        )
        SceneAlternativeGroups.register(
            translationKey = "test.group_priority.z",
            priority = 1,
            matches = Predicate {
                false
            },
        )
        StringTranslate.inject(ByteArrayInputStream((
            "test.group_priority.first=zzzz\n" +
                "test.group_priority.a=Zulu\n" +
                "test.group_priority.z=Alpha\n" +
                "test.group_priority.legacy=zzzz\n"
        ).toByteArray(Charsets.UTF_8)))
        assertEquals(
            listOf(
                "test.group_priority.first",
                "test.group_priority.z",
                "test.group_priority.a",
                SceneAlternativeGroups.OTHER,
                "test.group_priority.legacy",
            ),
            listOf(
                "test.group_priority.legacy",
                "test.group_priority.a",
                "test.group_priority.z",
                SceneAlternativeGroups.OTHER,
                "test.group_priority.first",
            ).sortedWith(Comparator(SceneAlternativeGroups::compareGroups)),
        )
        SceneAlternativeGroups.register(
            translationKey = "test.group_priority.later_rule",
            priority = Int.MIN_VALUE,
            matches = fluidOutput::isItemEqual,
        )
        assertEquals(
            "test.alternatives.custom",
            SceneAlternativeGroups.group(fluidOutput),
        )
    }
}
