package space.impact.iblueprints.mixins.ae2fc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.impact.iblueprints.integration.ae2.WirelessPlannerMode;

@Pseudo
@Mixin(targets = "com.glodblock.github.common.item.ItemBaseWirelessTerminal", remap = false)
public abstract class ItemBaseWirelessTerminalMixin {

    @Inject(method = "setMode", at = @At("HEAD"))
    private static void iblueprints$selectNativeMode(ItemStack stack, @Coerce Object mode, CallbackInfo callback) {
        WirelessPlannerMode.clear(stack);
    }

    @Inject(method = "openGui", at = @At("HEAD"), cancellable = true)
    private void iblueprints$restorePlanner(
        ItemStack stack,
        World world,
        EntityPlayer player,
        Object mode,
        CallbackInfo callback
    ) {
        if (WirelessPlannerMode.restore(stack, player, Integer.MIN_VALUE)) {
            callback.cancel();
        }
    }
}
