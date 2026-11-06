package space.impact.iblueprints.integration.ae2

import appeng.api.AEApi
import appeng.api.parts.IPart
import appeng.api.parts.IPartItem
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.world.World

internal object MeBlueprintPlannerItem : Item(), IPartItem {

    internal var brightIcon: IIcon? = null
        private set
    internal var darkIcon: IIcon? = null
        private set

    init {
        setUnlocalizedName("iblueprints.me_blueprint_planner")
        setTextureName("iblueprints:me_blueprint_terminal")
        setCreativeTab(CreativeTabs.tabMisc)
    }

    internal fun register() {
        GameRegistry.registerItem(
            this,
            "me_blueprint_planner",
        )
    }

    @SideOnly(Side.CLIENT)
    internal fun registerRenderer() {
        AEApi.instance().partHelper().setItemBusRenderer(this)
    }

    override fun createPartFromItemStack(stack: ItemStack): IPart {
        return MeBlueprintPlannerPart(stack)
    }

    @SideOnly(Side.CLIENT)
    override fun registerIcons(register: IIconRegister) {
        super.registerIcons(register)
        brightIcon = register.registerIcon("iblueprints:me_blueprint_terminal_bright")
        darkIcon = register.registerIcon("iblueprints:me_blueprint_terminal_dark")
    }

    @SideOnly(Side.CLIENT)
    override fun getSpriteNumber(): Int {
        return 0
    }

    override fun onItemUse(
        stack: ItemStack,
        player: EntityPlayer,
        world: World,
        x: Int,
        y: Int,
        z: Int,
        side: Int,
        hitX: Float,
        hitY: Float,
        hitZ: Float,
    ): Boolean {
        return AEApi.instance().partHelper().placeBus(
            stack,
            x,
            y,
            z,
            side,
            player,
            world,
        )
    }
}
