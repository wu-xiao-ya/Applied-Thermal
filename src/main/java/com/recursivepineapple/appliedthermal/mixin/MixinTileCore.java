package com.recursivepineapple.appliedthermal.mixin;

import cofh.core.block.TileCore;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileCore.class, remap = false)
public abstract class MixinTileCore {

    @Inject(method = "onLoad", at = @At("RETURN"), require = 0)
    private void appliedthermal$onLoad(CallbackInfo ci) {
        Object self = this;
        if (self instanceof TileMachineBase && self instanceof AppliedThermalMachine) {
            AppliedThermalMachine machine = (AppliedThermalMachine) self;
            machine.appliedthermal$getProviderAttachment().onReady();
            machine.appliedthermal$getFluxAttachment().onReady();
            machine.appliedthermal$getProviderAttachment().setEnabled(machine.appliedthermal$hasPatternProviderAugment());
        }
    }

    @Inject(method = "onChunkUnload", at = @At("HEAD"), require = 0)
    private void appliedthermal$onChunkUnload(CallbackInfo ci) {
        Object self = this;
        if (self instanceof TileMachineBase && self instanceof AppliedThermalMachine) {
            AppliedThermalMachine machine = (AppliedThermalMachine) self;
            machine.appliedthermal$getProviderAttachment().invalidate();
            machine.appliedthermal$getFluxAttachment().invalidate();
        }
    }

    @Inject(method = "invalidate", at = @At("HEAD"), require = 0)
    private void appliedthermal$invalidate(CallbackInfo ci) {
        Object self = this;
        if (self instanceof TileMachineBase && self instanceof AppliedThermalMachine) {
            AppliedThermalMachine machine = (AppliedThermalMachine) self;
            machine.appliedthermal$getProviderAttachment().invalidate();
            machine.appliedthermal$getFluxAttachment().invalidate();
        }
    }
}
