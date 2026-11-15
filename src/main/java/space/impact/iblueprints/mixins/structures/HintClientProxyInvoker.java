package space.impact.iblueprints.mixins.structures;

import net.minecraft.util.IIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import space.impact.structures.ClientProxy;

@Mixin(value = ClientProxy.class, remap = false)
public interface HintClientProxyInvoker {

    @Invoker("markTextureUsed")
    static void iblueprints$markTextureUsed(IIcon icon) {
        throw new AssertionError();
    }
}
