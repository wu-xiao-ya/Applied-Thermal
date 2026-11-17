package com.recursivepineapple.appliedthermal.mixin;

import cofh.core.block.TileRSControl;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileRSControl.class, remap = false)
public abstract class MixinTileRSControl {

    @Inject(method = "onNeighborBlockChange", at = @At("RETURN"))
    private void appliedthermal$updateProviderRedstone(CallbackInfo ci) {
        if (this instanceof AppliedThermalMachine) {
            ((AppliedThermalMachine) this).appliedthermal$getProviderAttachment().updateRedstoneState();
        }
    }
}
