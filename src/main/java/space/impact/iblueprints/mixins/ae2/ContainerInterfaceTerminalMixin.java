package space.impact.iblueprints.mixins.ae2;

import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.items.misc.ItemEncodedPattern;
import appeng.helpers.InventoryAction;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import space.impact.iblueprints.integration.ae2.StructurePatternItem;
import space.impact.iblueprints.integration.ae2.BlueprintProviderPatternInventory;
import java.util.Map;

@Mixin(value = ContainerInterfaceTerminal.class, remap = false)
public abstract class ContainerInterfaceTerminalMixin {

    @Shadow
    @Final
    private Map<Long, Object> trackedById;

    @Definition(id = "encodedPattern", type = ItemEncodedPattern.class)
    @Expression("? instanceof encodedPattern")
    @ModifyExpressionValue(method = "doAction", at = @At("MIXINEXTRAS:EXPRESSION"), require = 1)
    private boolean iblueprints$acceptStructurePattern(
        boolean original,
        EntityPlayerMP player,
        InventoryAction action,
        int slot,
        long id
    ) {
        if (original) {
            return true;
        }
        ItemStack held = player.inventory.getItemStack();
        Object tracker = trackedById.get(id);
        return held != null && held.getItem() == StructurePatternItem.INSTANCE
            && tracker instanceof InterfaceTerminalTrackerAccessor
            && ((InterfaceTerminalTrackerAccessor) tracker).iblueprints$getPatterns()
                instanceof BlueprintProviderPatternInventory;
    }
}
