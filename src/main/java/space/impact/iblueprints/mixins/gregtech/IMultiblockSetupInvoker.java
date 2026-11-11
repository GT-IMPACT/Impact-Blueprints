package space.impact.iblueprints.mixins.gregtech;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GT_MetaTileEntity_MultiBlockBase.class, remap = false)
public interface IMultiblockSetupInvoker {

    @Invoker("setupMachine")
    void iblueprints$setupMachine(IGregTechTileEntity tile, ItemStack stack);
}
