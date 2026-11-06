package space.impact.iblueprints.mixins.ae2;

import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "appeng.container.implementations.ContainerInterfaceTerminal$InvTracker", remap = false)
public interface InterfaceTerminalTrackerAccessor {

    @Accessor("patterns")
    IInventory iblueprints$getPatterns();
}
