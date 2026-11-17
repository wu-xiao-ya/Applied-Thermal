package com.recursivepineapple.appliedthermal.mixin;

import ae2.api.config.LockCraftingMode;
import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.util.IConfigManager;
import ae2.helpers.patternprovider.PatternProviderLogic;
import com.recursivepineapple.appliedthermal.provider.ThermalMachinePatternProviderLogic;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class MixinPatternProviderLogic {

    @Inject(method = "configChanged", at = @At("HEAD"), cancellable = true)
    private void appliedthermal$restrictCraftingLock(IConfigManager manager, Setting<?> setting, CallbackInfo ci) {
        if (!((Object) this instanceof ThermalMachinePatternProviderLogic) || setting != Settings.LOCK_CRAFTING_MODE) {
            return;
        }

        LockCraftingMode mode = manager.getSetting(Settings.LOCK_CRAFTING_MODE);
        if (mode != LockCraftingMode.NONE && mode != LockCraftingMode.LOCK_UNTIL_RESULT) {
            manager.putSetting(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE);
            ci.cancel();
        }
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void appliedthermal$clearLegacyPulseLock(NBTTagCompound tag, CallbackInfo ci) {
        if ((Object) this instanceof ThermalMachinePatternProviderLogic) {
            PatternProviderLogic logic = (PatternProviderLogic) (Object) this;
            // AE2S restores the pending unlock event after reading the configured lock mode.
            if (logic.getCraftingLockedReason() == LockCraftingMode.LOCK_UNTIL_PULSE) {
                logic.resetCraftingLock();
            }
        }
    }
}
