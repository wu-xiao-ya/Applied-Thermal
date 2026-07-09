package com.recursivepineapple.appliedthermal.mixin;

import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntity.class, remap = false)
public abstract class MixinTileEntity {

    @Inject(method = {"invalidate", "func_145843_s"}, at = @At("HEAD"))
    private void appliedthermal$invalidate(CallbackInfo ci) {
        Object self = this;
        if (self instanceof TileMachineBase && self instanceof AppliedThermalMachine) {
            ((AppliedThermalMachine) self).appliedthermal$getProviderAttachment().invalidate();
        }
    }
}
