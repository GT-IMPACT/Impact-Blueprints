package space.impact.iblueprints.item

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import space.impact.iblueprints.integration.structures.SceneBlueprint

internal object StructureBlueprintRenderer : IItemRenderer {

    private var renderingController = false

    override fun handleRenderType(
        item: ItemStack,
        type: ItemRenderType,
    ): Boolean {
        return !renderingController && type == ItemRenderType.INVENTORY &&
            (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) &&
            controller(item) != null
    }

    override fun shouldUseRenderHelper(
        type: ItemRenderType,
        item: ItemStack,
        helper: ItemRendererHelper,
    ): Boolean {
        return false
    }

    override fun renderItem(
        type: ItemRenderType,
        item: ItemStack,
        vararg data: Any,
    ) {
        val controller = controller(item) ?: return
        val client = Minecraft.getMinecraft()
        renderingController = true
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT or GL11.GL_COLOR_BUFFER_BIT or GL11.GL_LIGHTING_BIT or GL11.GL_CURRENT_BIT)
        try {
            // Forge applies the blueprint's blue tint before calling the custom renderer.
            GL11.glColor4f(
                1f,
                1f,
                1f,
                1f,
            )
            RenderHelper.enableGUIStandardItemLighting()
            GL11.glEnable(GL12.GL_RESCALE_NORMAL)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            RenderItem.getInstance().renderItemAndEffectIntoGUI(
                client.fontRenderer,
                client.textureManager,
                controller,
                0,
                0,
            )
        } finally {
            GL11.glPopAttrib()
            renderingController = false
        }
    }

    private fun controller(blueprint: ItemStack): ItemStack? {
        val plan = SceneBlueprint.fromItem(blueprint) ?: return null
        if (plan.getInteger("version") != StructureBlueprintItem.PLAN_VERSION) {
            return null
        }
        return SceneBlueprint.controller(plan)?.takeUnless {
            it.item === StructureBlueprintItem
        }
    }
}
