package space.impact.iblueprints.item

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item

internal object BlankBlueprintItem : Item() {

    internal const val REGISTRY_NAME = "blank_blueprint"

    init {
        setUnlocalizedName("iblueprints.blank_blueprint")
        setTextureName("iblueprints:blank_blueprint")
        setCreativeTab(CreativeTabs.tabMisc)
    }
}
