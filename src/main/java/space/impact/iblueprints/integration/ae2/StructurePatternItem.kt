package space.impact.iblueprints.integration.ae2

import appeng.core.localization.GuiText
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.client.gui.GuiScreen
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.StatCollector
import space.impact.iblueprints.integration.structures.SceneBlueprint
import java.text.NumberFormat

internal object StructurePatternItem : Item() {

    internal const val REGISTRY_NAME = "structure_pattern"

    init {
        setUnlocalizedName("iblueprints.structure_pattern")
        setTextureName("appliedenergistics2:ItemEncodedUltimatePattern")
        setMaxStackSize(1)
        setCreativeTab(CreativeTabs.tabMisc)
    }

    @SideOnly(Side.CLIENT)
    override fun getColorFromItemStack(
        stack: ItemStack,
        pass: Int,
    ): Int {
        return 0x6EA9FF
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
            tooltip.add("${EnumChatFormatting.GRAY}${StatCollector.translateToLocal("iblueprints.pattern.empty")}")
            return
        }
        val controller = SceneBlueprint.controller(plan)
        val materials = SceneBlueprint.materials(plan)
        if (controller == null || materials == null) {
            tooltip.add("${EnumChatFormatting.RED}${GuiText.InvalidPattern.local}")
            return
        }

        val counts = NumberFormat.getIntegerInstance()
        tooltip.add("${EnumChatFormatting.DARK_AQUA}${GuiText.Result.local}:")
        tooltip.add(materialLine(
            amount = counts.format(1),
            name = controller.displayName,
        ))
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add("${EnumChatFormatting.DARK_GREEN}${GuiText.Ingredients.local}:")
            for (material in materials.sortedByDescending { it.stackSize }) {
                tooltip.add(materialLine(
                    amount = counts.format(material.stackSize.toLong()),
                    name = material.displayName,
                ))
            }
        } else {
            tooltip.add("${EnumChatFormatting.GRAY}${GuiText.HoldShift.local}")
        }
        val no = "${EnumChatFormatting.RESET}${GuiText.No.local}"
        tooltip.add("${EnumChatFormatting.YELLOW}${GuiText.Substitute.local} $no")
        tooltip.add("${EnumChatFormatting.YELLOW}${GuiText.BeSubstitute.local} $no")
        if (!PlannerPatterns.isOrder(stack)) {
            tooltip.add("${EnumChatFormatting.GRAY}${StatCollector.translateToLocal("iblueprints.pattern.provider")}")
        }
    }

    private fun materialLine(
        amount: String,
        name: String,
    ): String {
        return "   ${EnumChatFormatting.WHITE}$amount ${EnumChatFormatting.GREEN}$name"
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        if (PlannerPatterns.isOrder(stack)) {
            val controller = SceneBlueprint.fromItem(stack)?.let(SceneBlueprint::controller)
            if (controller != null) {
                return StatCollector.translateToLocalFormatted(
                    "iblueprints.pattern.order",
                    controller.displayName,
                )
            }
        }
        return super.getItemStackDisplayName(stack)
    }
}
