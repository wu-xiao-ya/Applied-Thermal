package com.recursivepineapple.appliedthermal.mixin;

import ae2.api.AECapabilities;
import ae2.api.networking.IInWorldGridNodeHost;
import cofh.core.block.TilePowered;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import javax.annotation.Nullable;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TilePowered.class, remap = false)
public abstract class MixinTilePowered {

    @Inject(method = "hasCapability", at = @At("HEAD"), cancellable = true)
    private void appliedthermal$hasCapability(Capability<?> capability, @Nullable EnumFacing facing,
                                              CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (self instanceof TileMachineBase && self instanceof AppliedThermalMachine) {
            AppliedThermalMachine machine = (AppliedThermalMachine) self;
            if (machine.appliedthermal$getProviderAttachment().isEnabled()
                && capability == AECapabilities.IN_WORLD_GRID_NODE_HOST) {
                cir.setReturnValue(true);
                return;
            }
            if (machine.appliedthermal$hasFluxInductionSupport()
                && machine.appliedthermal$getFluxAttachment().hasCapability(capability, facing)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true)
    private <T> void appliedthermal$getCapability(Capability<T> capability, @Nullable EnumFacing facing,
                                                  CallbackInfoReturnable<T> cir) {
        Object self = this;
        if (self instanceof TileMachineBase && self instanceof AppliedThermalMachine) {
            AppliedThermalMachine machine = (AppliedThermalMachine) self;
            if (machine.appliedthermal$getProviderAttachment().isEnabled()
                && capability == AECapabilities.IN_WORLD_GRID_NODE_HOST) {
                cir.setReturnValue(AECapabilities.IN_WORLD_GRID_NODE_HOST.cast(
                    (IInWorldGridNodeHost) self));
                return;
            }
            if (machine.appliedthermal$hasFluxInductionSupport()) {
                T provided = machine.appliedthermal$getFluxAttachment().getCapability(capability, facing);
                if (provided != null) {
                    cir.setReturnValue(provided);
                }
            }
        }
    }
}
