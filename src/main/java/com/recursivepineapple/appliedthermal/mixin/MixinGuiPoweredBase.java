package com.recursivepineapple.appliedthermal.mixin;

import cofh.thermalexpansion.block.machine.TileMachineBase;
import cofh.thermalexpansion.gui.client.GuiPoweredBase;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.client.gui.TabAppliedPatternProvider;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiPoweredBase.class, remap = false)
public abstract class MixinGuiPoweredBase {

    @Shadow
    protected cofh.core.block.TilePowered baseTile;

    @Inject(method = "func_73866_w_", at = @At("RETURN"))
    private void appliedthermal$addProviderTab(CallbackInfo ci) {
        if (baseTile instanceof TileMachineBase && baseTile instanceof AppliedThermalMachine) {
            AppliedThermalMachine machine = (AppliedThermalMachine) baseTile;
            if (machine.appliedthermal$hasPatternProviderAugment()) {
                ((GuiPoweredBase) (Object) this)
                    .addTab(new TabAppliedPatternProvider((GuiPoweredBase) (Object) this, machine));
            }
            AppliedThermal.LOG.debug("Applied Thermal tabs updated for tile {}.", baseTile.getPos());
        } else {
            AppliedThermal.LOG.debug("Skipped Applied Thermal tabs for tile {}.", baseTile);
        }
    }
}
