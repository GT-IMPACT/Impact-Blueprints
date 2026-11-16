package space.impact.iblueprints.scene

import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidBlock

internal object FluidMaterial {

    internal fun stack(block: Block): ItemStack? {
        // Scene materials also work without the optional ImpactStructures integration.
        val fluid = when {
            block is IFluidBlock -> block.fluid
            block === Blocks.water || block === Blocks.flowing_water -> FluidRegistry.WATER
            block === Blocks.lava || block === Blocks.flowing_lava -> FluidRegistry.LAVA
            block.material.isLiquid -> FluidRegistry.lookupFluidForBlock(block)
            else -> null
        } ?: return null
        val request = FluidStack(
            fluid,
            FluidContainerRegistry.BUCKET_VOLUME,
        )
        val bucket = FluidContainerRegistry.fillFluidContainer(
            request,
            FluidContainerRegistry.EMPTY_BUCKET,
        )
        if (bucket != null) {
            return bucket
        }
        return FluidContainerRegistry.getRegisteredFluidContainerData().firstOrNull {
            it.fluid.isFluidEqual(request) && it.fluid.amount == request.amount
        }?.filledContainer?.copy()
    }
}
