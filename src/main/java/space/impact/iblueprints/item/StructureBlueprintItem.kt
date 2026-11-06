package space.impact.iblueprints.item

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import java.text.NumberFormat
import net.minecraft.client.gui.GuiScreen
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.StatCollector
import space.impact.iblueprints.integration.nei.BlueprintViewInput
import space.impact.iblueprints.integration.structures.SceneBlueprint

internal object StructureBlueprintItem : Item() {

    internal const val REGISTRY_NAME = "structure_blueprint"
    internal const val PLAN_TAG = "structurePlan"
    internal const val RECIPE_TAG = "structureRecipe"
    internal const val PLAN_VERSION = 1
    internal const val PLACEMENT_SIZE = 5
    private const val TRANSLATION_PREFIX = "item.iblueprints.structure_blueprint"

    init {
        setUnlocalizedName("iblueprints.structure_blueprint")
        setTextureName("minecraft:paper")
        setMaxStackSize(1)
        setCreativeTab(CreativeTabs.tabMisc)
    }

    internal fun register() {
        GameRegistry.registerItem(
            this,
            REGISTRY_NAME,
        )
    }

    @SideOnly(Side.CLIENT)
    override fun getColorFromItemStack(
        stack: ItemStack,
        pass: Int,
    ): Int {
        return 0x538CF3
    }

    @SideOnly(Side.CLIENT)
    override fun addInformation(
        stack: ItemStack,
        player: EntityPlayer?,
        tooltip: MutableList<String>,
        advanced: Boolean,
    ) {
        val plan = SceneBlueprint.fromItem(stack)
        if (plan == null) {
            tooltip.add("${EnumChatFormatting.GRAY}${text("empty")}")
            return
        }
        if (plan.getInteger("version") != PLAN_VERSION) {
            tooltip.add("${EnumChatFormatting.RED}${text("unknown")}")
            return
        }
        val controllerName = SceneBlueprint.controller(plan)?.displayName
            ?: plan.getCompoundTag("controller").getString("item")
        tooltip.add("${EnumChatFormatting.DARK_AQUA}${text("controller")}")
        tooltip.add(materialLine(
            count = "1",
            name = controllerName,
        ))
        if (GuiScreen.isShiftKeyDown()) {
            addMaterials(
                plan = plan,
                tooltip = tooltip,
            )
        } else {
            tooltip.add("${EnumChatFormatting.GRAY}${text("hold_shift")}")
        }
        if (Loader.isModLoaded("NotEnoughItems") && Loader.isModLoaded("modularui2")) {
            val key = BlueprintViewInput.keyName()
            if (key != null) {
                tooltip.add(
                    EnumChatFormatting.GRAY.toString() + StatCollector.translateToLocalFormatted(
                        "$TRANSLATION_PREFIX.preview",
                        key,
                    ),
                )
            }
        }
        if (Loader.isModLoaded("impactapi")) {
            tooltip.add("${EnumChatFormatting.GRAY}${text("use")}")
        }
    }

    @SideOnly(Side.CLIENT)
    private fun addMaterials(
        plan: NBTTagCompound,
        tooltip: MutableList<String>,
    ) {
        val materials = SceneBlueprint.materials(plan)
        if (materials == null) {
            tooltip.add("${EnumChatFormatting.RED}${text("missing_materials")}")
            return
        }
        tooltip.add("${EnumChatFormatting.DARK_GREEN}${text("materials")}")
        val countFormat = NumberFormat.getIntegerInstance()
        for (material in materials.sortedBy { it.stackSize }.asReversed()) {
            tooltip.add(materialLine(
                count = countFormat.format(material.stackSize.toLong()),
                name = material.displayName,
            ))
        }
    }

    private fun materialLine(
        count: String,
        name: String,
    ): String {
        return "   ${EnumChatFormatting.WHITE}$count ${EnumChatFormatting.GREEN}$name"
    }

    private fun text(key: String): String {
        return StatCollector.translateToLocal("$TRANSLATION_PREFIX.$key")
    }
}
