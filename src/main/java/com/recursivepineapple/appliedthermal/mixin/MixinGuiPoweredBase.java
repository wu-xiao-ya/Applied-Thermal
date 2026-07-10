package com.recursivepineapple.appliedthermal.mixin;

import cofh.thermalexpansion.block.machine.TileMachineBase;
import cofh.thermalexpansion.gui.client.GuiPoweredBase;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.client.gui.TabAppliedPatternProvider;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiPoweredBase.class, remap = false)
public abstract class MixinGuiPoweredBase {

    @Shadow
    protected cofh.core.block.TilePowered baseTile;

    @Unique
    private TabAppliedPatternProvider appliedthermal$providerTab;

    @Inject(method = "func_73866_w_", at = @At("RETURN"))
    private void appliedthermal$addProviderTab(CallbackInfo ci) {
        appliedthermal$providerTab = null;
        appliedthermal$syncProviderTab();
    }

    @Inject(method = "func_73876_c", at = @At("RETURN"))
    private void appliedthermal$refreshProviderTab(CallbackInfo ci) {
        appliedthermal$syncProviderTab();
    }

    @Unique
    private void appliedthermal$syncProviderTab() {
        if (!(baseTile instanceof TileMachineBase) || !(baseTile instanceof AppliedThermalMachine)) {
            return;
        }

        GuiPoweredBase gui = (GuiPoweredBase) (Object) this;
        AppliedThermalMachine machine = (AppliedThermalMachine) baseTile;
        boolean shouldShow = machine.appliedthermal$hasPatternProviderAugment();
        boolean isShown = appliedthermal$providerTab != null && gui.tabs.contains(appliedthermal$providerTab);

        if (shouldShow == isShown) {
            return;
        }

        if (shouldShow) {
            appliedthermal$providerTab = new TabAppliedPatternProvider(gui, machine);
            gui.addTab(appliedthermal$providerTab);
        } else {
            gui.tabs.remove(appliedthermal$providerTab);
            appliedthermal$providerTab = null;
        }
        AppliedThermal.LOG.debug("Applied Thermal provider tab visibility changed to {} for tile {}.",
            shouldShow, baseTile.getPos());
    }
}
