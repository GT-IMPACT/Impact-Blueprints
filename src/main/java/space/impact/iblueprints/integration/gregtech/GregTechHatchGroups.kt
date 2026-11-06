package space.impact.iblueprints.integration.gregtech

import gregtech.api.enums.Casings
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_DataAccess
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Dynamo
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_DynamoMulti
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Energy
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_EnergyMulti
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Input
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Maintenance
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus
import gregtech.api.metatileentity.implementations.GTMTE_BusHatch_Input
import gregtech.api.util.GTStructureUtility
import gregtech.common.blocks.GT_Block_Casings_Abstract
import net.minecraft.block.Block
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack

internal object GregTechHatchGroups {

    internal fun group(stack: ItemStack): String? {
        if (stack.item is ItemBlock) {
            val block = Block.getBlockFromItem(stack.item)
            if (block is GT_Block_Casings_Abstract || Casings.entries.any {
                    it.block === block && it.metadata == stack.itemDamage
                }
            ) {
                return "iblueprints.variants.group.casings"
            }
        }
        val role = when (GTStructureUtility.getMetaTileEntity(stack)) {
            is GT_MetaTileEntity_Hatch_Input -> "fluid_input"
            is GT_MetaTileEntity_Hatch_Output -> "fluid_output"
            is GT_MetaTileEntity_Hatch_InputBus -> "item_input"
            is GT_MetaTileEntity_Hatch_OutputBus -> "item_output"
            is GTMTE_BusHatch_Input -> "combined_input"
            is GT_MetaTileEntity_Hatch_Energy, is GT_MetaTileEntity_Hatch_EnergyMulti -> "energy_input"
            is GT_MetaTileEntity_Hatch_Dynamo, is GT_MetaTileEntity_Hatch_DynamoMulti -> "energy_output"
            is GT_MetaTileEntity_Hatch_Maintenance -> "maintenance"
            is GT_MetaTileEntity_Hatch_Muffler -> "mufflers"
            is GT_MetaTileEntity_Hatch_DataAccess -> "data_access"
            else -> return null
        }
        return "iblueprints.variants.group.$role"
    }

    internal fun tier(stack: ItemStack): Int {
        return (GTStructureUtility.getMetaTileEntity(stack) as? GT_MetaTileEntity_Hatch)?.mTier?.toInt() ?: -1
    }
}
