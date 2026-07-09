package com.recursivepineapple.appliedthermal.mixin;

import cofh.thermalexpansion.block.machine.BlockMachine;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockMachine.class, remap = false)
public abstract class MixinBlockMachine {

    @Inject(method = "getItemStackTag", at = @At("RETURN"), cancellable = true, require = 0)
    private void appliedthermal$writeDropTag(World world, BlockPos pos, CallbackInfoReturnable<NBTTagCompound> cir) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileMachineBase && tile instanceof AppliedThermalMachine) {
            AppliedThermalMachine machine = (AppliedThermalMachine) tile;
            NBTTagCompound tag = cir.getReturnValue();
            if (tag == null) {
                tag = new NBTTagCompound();
            }
            machine.appliedthermal$getProviderAttachment().writeToNBT(tag);
            machine.appliedthermal$getFluxAttachment().writeToNBT(tag);
            cir.setReturnValue(tag);
        }
    }
}
