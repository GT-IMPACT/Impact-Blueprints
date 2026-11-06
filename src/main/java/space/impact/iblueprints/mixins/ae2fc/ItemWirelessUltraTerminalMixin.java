package space.impact.iblueprints.mixins.ae2fc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import space.impact.iblueprints.integration.ae2.WirelessPlannerMode;

@Pseudo
@Mixin(targets = "com.glodblock.github.common.item.ItemWirelessUltraTerminal", remap = false)
public abstract class ItemWirelessUltraTerminalMixin {

    @Inject(
        method = "openGui(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;"
            + "Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/Object;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void iblueprints$restorePlanner(
        ItemStack stack,
        World world,
        EntityPlayer player,
        Object mode,
        int slot,
        CallbackInfo callback
    ) {
        if (WirelessPlannerMode.restore(stack, player, slot)) {
            callback.cancel();
        }
    }
}
