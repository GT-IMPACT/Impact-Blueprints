package space.impact.iblueprints.mixins.ae2fc;

import appeng.api.config.CraftingStatus;
import appeng.api.config.Settings;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.container.AEBaseContainer;
import appeng.core.AEConfig;
import com.glodblock.github.common.item.ItemBaseWirelessTerminal;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMEMonitorable.class)
public abstract class GuiMEMonitorableMixin extends AEBaseGui {

    @Shadow(remap = false)
    @Final
    private boolean viewCell;

    @Unique
    private boolean iblueprints$handlingWirelessClick;

    @Unique
    private boolean iblueprints$resizeAfterClick;

    protected GuiMEMonitorableMixin(Container container) {
        super(container);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void iblueprints$beginClick(int x, int y, int button, CallbackInfo callback) {
        iblueprints$handlingWirelessClick = iblueprints$isAe2fcTerminal();
    }

    @Inject(method = "reinitalize", at = @At("HEAD"), cancellable = true, remap = false)
    private void iblueprints$deferResize(CallbackInfo callback) {
        if (iblueprints$handlingWirelessClick) {
            // Forge keeps scanning the button list until the mouse click returns.
            iblueprints$resizeAfterClick = true;
            callback.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void iblueprints$endClick(int x, int y, int button, CallbackInfo callback) {
        iblueprints$handlingWirelessClick = false;
        if (iblueprints$resizeAfterClick) {
            iblueprints$resizeAfterClick = false;
            reinitalize();
        }
    }

    @ModifyArg(
        method = "initGui",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/client/gui/implementations/GuiMEMonitorable;initCustomButtons(II)V",
            remap = false
        ),
        index = 1
    )
    private int iblueprints$reserveCraftingStatusRow(int offset) {
        if (viewCell && iblueprints$isAe2fcTerminal()
            && AEConfig.instance.getConfigManager().getSetting(Settings.CRAFTING_STATUS) == CraftingStatus.BUTTON) {
            return offset + 20;
        }
        return offset;
    }

    @Unique
    private boolean iblueprints$isAe2fcTerminal() {
        if (!(inventorySlots instanceof AEBaseContainer)) {
            return false;
        }
        Object target = ((AEBaseContainer) inventorySlots).getTarget();
        if (!(target instanceof IGuiItemObject)) {
            return false;
        }
        ItemStack stack = ((IGuiItemObject) target).getItemStack();
        return stack != null && stack.getItem() instanceof ItemBaseWirelessTerminal;
    }
}
