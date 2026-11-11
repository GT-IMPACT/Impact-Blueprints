package space.impact.iblueprints.mixins.gregtech;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase;
import org.spongepowered.asm.mixin.Mixin;
import space.impact.iblueprints.integration.gregtech.BuiltStructureCapture;

@Mixin(value = GT_MetaTileEntity_EnhancedMultiBlockBase.class, remap = false)
public abstract class MultiblockPieceMixin {

    @WrapMethod(method = "checkPiece")
    private boolean iblueprints$collectSuccessfulPiece(
        String piece,
        int horizontal,
        int vertical,
        int depth,
        Operation<Boolean> original
    ) {
        return BuiltStructureCapture.INSTANCE.checkPiece(
            (GT_MetaTileEntity_EnhancedMultiBlockBase<?>) (Object) this,
            () -> original.call(piece, horizontal, vertical, depth)
        );
    }
}
