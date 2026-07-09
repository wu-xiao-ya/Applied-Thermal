package com.recursivepineapple.appliedthermal.mixin;

import appeng.api.config.LockCraftingMode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.me.helpers.AENetworkProxy;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import java.util.Set;
import net.minecraft.inventory.InventoryCrafting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DualityInterface.class, remap = false)
public abstract class MixinDualityInterface {

    @Shadow
    @Final
    private IInterfaceHost iHost;

    @Shadow
    @Final
    private AENetworkProxy gridProxy;

    @Shadow
    private Set<ICraftingPatternDetails> craftingList;

    @Shadow
    private boolean hasItemsToSend() {
        throw new AssertionError();
    }

    @Shadow
    private boolean hasItemsToSendFacing() {
        throw new AssertionError();
    }

    @Shadow
    private void onPushPatternSuccess(ICraftingPatternDetails pattern) {
        throw new AssertionError();
    }

    @Shadow
    public abstract LockCraftingMode getCraftingLockedReason();

    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true)
    private void appliedthermal$pushIntoMachine(ICraftingPatternDetails pattern, InventoryCrafting table,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (!(iHost instanceof AppliedThermalMachine)) {
            return;
        }
        AppliedThermalMachine machine = (AppliedThermalMachine) iHost;
        if (pattern.isCraftable() || hasItemsToSend() || hasItemsToSendFacing() || !gridProxy.isActive()
            || craftingList == null || !craftingList.contains(pattern)
            || getCraftingLockedReason() != LockCraftingMode.NONE) {
            cir.setReturnValue(false);
            return;
        }
        if (machine.appliedthermal$getProviderAttachment().tryPushPattern(table)) {
            onPushPatternSuccess(pattern);
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isBusy", at = @At("HEAD"), cancellable = true)
    private void appliedthermal$useMachineBusyState(CallbackInfoReturnable<Boolean> cir) {
        if (iHost instanceof AppliedThermalMachine) {
            AppliedThermalMachine machine = (AppliedThermalMachine) iHost;
            cir.setReturnValue(hasItemsToSend() || hasItemsToSendFacing()
                || machine.appliedthermal$getProviderAttachment().isPatternTargetBusy());
        }
    }
}
