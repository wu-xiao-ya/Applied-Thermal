package com.recursivepineapple.appliedthermal.mixin;

import cofh.core.block.BlockCoreTile;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockCoreTile.class, remap = false)
public abstract class MixinBlockMachine {

    @Inject(method = "getItemStackTag", at = @At("RETURN"), cancellable = true)
    private void appliedthermal$writeDropTag(IBlockAccess world, BlockPos pos,
                                             CallbackInfoReturnable<NBTTagCompound> cir) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMachineBase && tile instanceof AppliedThermalMachine) {
            NBTTagCompound tag = cir.getReturnValue();
            if (tag == null) {
                tag = new NBTTagCompound();
            }
            ((AppliedThermalMachine) tile).appliedthermal$getProviderAttachment().writeToNBT(tag);
            cir.setReturnValue(tag);
        }
    }
}
