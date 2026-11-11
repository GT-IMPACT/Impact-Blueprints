package space.impact.iblueprints.item

import cpw.mods.fml.common.Loader
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.StatCollector

internal object BlankBlueprintItem : Item() {

    internal const val REGISTRY_NAME = "blank_blueprint"

    init {
        setUnlocalizedName("iblueprints.blank_blueprint")
        setTextureName("iblueprints:blank_blueprint")
        setCreativeTab(CreativeTabs.tabMisc)
    }

    @SideOnly(Side.CLIENT)
    override fun addInformation(
        stack: ItemStack,
        player: EntityPlayer?,
        tooltip: MutableList<String>,
        advanced: Boolean,
    ) {
        if (Loader.isModLoaded("gregtech") && Loader.isModLoaded("impactapi")) {
            tooltip.add(
                EnumChatFormatting.GRAY.toString() + StatCollector.translateToLocal(
                    "item.iblueprints.blank_blueprint.use",
                ),
            )
        }
    }
}
