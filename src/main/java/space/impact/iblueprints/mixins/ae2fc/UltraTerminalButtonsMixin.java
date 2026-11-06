package space.impact.iblueprints.mixins.ae2fc;

import com.glodblock.github.client.UltraTerminalButtons;
import com.glodblock.github.util.UltraTerminalModes;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import space.impact.iblueprints.integration.ae2.WirelessPlannerMode;

@Mixin(value = UltraTerminalButtons.class, remap = false)
public abstract class UltraTerminalButtonsMixin {

    @Shadow
    private ItemStack terminal;

    @Shadow
    public UltraTerminalModes terminalMode;

    @Redirect(
        method = "toggleSwitchButtons",
        at = @At(
            value = "FIELD",
            target = "Lcom/glodblock/github/client/UltraTerminalButtons;"
                + "terminalMode:Lcom/glodblock/github/util/UltraTerminalModes;"
        ),
        require = 5,
        expect = 5,
        allow = 5
    )
    private UltraTerminalModes iblueprints$showNativeModes(UltraTerminalButtons buttons) {
        return WirelessPlannerMode.isSelected(terminal) ? null : terminalMode;
    }
}
