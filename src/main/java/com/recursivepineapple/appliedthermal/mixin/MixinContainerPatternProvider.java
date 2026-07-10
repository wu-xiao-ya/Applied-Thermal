package com.recursivepineapple.appliedthermal.mixin;

import ae2.container.implementations.ContainerPatternProvider;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import com.recursivepineapple.appliedthermal.container.AppliedThermalPatternProviderContainer;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import javax.annotation.Nullable;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerPatternProvider.class, remap = false)
public abstract class MixinContainerPatternProvider implements AppliedThermalPatternProviderContainer {

    @Unique
    @Nullable
    private AppliedThermalMachine appliedthermal$machine;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void appliedthermal$captureMachine(InventoryPlayer playerInventory, PatternProviderLogicHost host,
                                               CallbackInfo ci) {
        if (host instanceof AppliedThermalMachine) {
            appliedthermal$machine = (AppliedThermalMachine) host;
        }
    }

    @Nullable
    @Override
    public AppliedThermalMachine appliedthermal$getMachine() {
        return appliedthermal$machine;
    }
}
