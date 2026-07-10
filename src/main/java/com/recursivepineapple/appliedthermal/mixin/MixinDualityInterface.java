package com.recursivepineapple.appliedthermal.mixin;

import appeng.api.config.LockCraftingMode;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.me.helpers.AENetworkProxy;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
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

    @Shadow
    public abstract IItemHandler getPatterns();

    @Inject(method = "pushPattern", at = @At("HEAD"), cancellable = true)
    private void appliedthermal$pushIntoMachine(ICraftingPatternDetails pattern, InventoryCrafting table,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (!(iHost instanceof AppliedThermalMachine)) {
            return;
        }
        AppliedThermalMachine machine = (AppliedThermalMachine) iHost;
        if (pattern.isCraftable() || hasItemsToSend() || hasItemsToSendFacing() || !gridProxy.isActive()
            || !appliedthermal$isInstalledPattern(pattern)
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

    private boolean appliedthermal$isInstalledPattern(ICraftingPatternDetails requestedPattern) {
        World world = iHost.getTileEntity().getWorld();
        IItemHandler patterns = getPatterns();
        for (int slot = 0; slot < patterns.getSlots(); slot++) {
            ItemStack stack = patterns.getStackInSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ICraftingPatternItem)) {
                continue;
            }
            try {
                ICraftingPatternDetails installed =
                    ((ICraftingPatternItem) stack.getItem()).getPatternForItem(stack, world);
                if (requestedPattern.equals(installed)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Invalid encoded patterns are ignored just like DualityInterface's pattern refresh.
            }
        }
        return false;
    }
}
