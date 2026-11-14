package space.impact.iblueprints.mixins.structures;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import space.impact.iblueprints.integration.structures.ProjectorProgramming;
import space.impact.structures.item.ItemConstructableTrigger;

@Mixin(ItemConstructableTrigger.class)
public abstract class ItemConstructableTriggerMixin extends Item {

    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    private void iblueprints$clearLayout(
        ItemStack stack,
        World world,
        EntityPlayer player,
        CallbackInfoReturnable<ItemStack> callback
    ) {
        // Forge also tries to use the held item after a canceled block click.
        if (player.isSneaking() && ProjectorProgramming.hasLayout(stack)
            && super.getMovingObjectPositionFromPlayer(world, player, true) == null) {
            callback.setReturnValue(ProjectorProgramming.clearedCopy(stack));
        }
    }

    @Inject(method = "addInformation", at = @At("RETURN"))
    private void iblueprints$showClearLayoutHint(
        ItemStack stack,
        EntityPlayer player,
        List<String> tooltip,
        boolean advanced,
        CallbackInfo callback
    ) {
        if (ProjectorProgramming.hasLayout(stack)) {
            tooltip.add(StatCollector.translateToLocal("iblueprints.projector.clear_hint"));
        }
    }
}
