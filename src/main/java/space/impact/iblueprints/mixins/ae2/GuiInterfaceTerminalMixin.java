package space.impact.iblueprints.mixins.ae2;

import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.core.CommonHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.impact.iblueprints.integration.ae2.StructurePatternItem;

@Mixin(value = GuiInterfaceTerminal.class, remap = false)
public abstract class GuiInterfaceTerminalMixin {

    @Inject(method = "recipeIsBroken", at = @At("HEAD"), cancellable = true)
    private void iblueprints$validateStructurePattern(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (stack != null && stack.getItem() == StructurePatternItem.INSTANCE) {
            World world = CommonHelper.proxy.getWorld();
            callback.setReturnValue(world != null && !StructurePatternItem.isValidProviderPattern(stack, world));
        }
    }
}
