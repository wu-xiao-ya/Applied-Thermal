package com.recursivepineapple.appliedthermal.provider;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.patternprovider.PatternProviderLogic;
import com.recursivepineapple.appliedthermal.mixin.PatternProviderLogicAccessor;

/**
 * AE2 pattern-provider logic used by a Thermal machine attachment.
 *
 * <p>Thermal machines are generic processing targets rather than AE2 crafting
 * machines. The whole selected input set is therefore checked before anything
 * is inserted into the machine.</p>
 */
public final class ThermalMachinePatternProviderLogic extends PatternProviderLogic {

    private final AppliedThermalProviderAttachment attachment;

    public ThermalMachinePatternProviderLogic(IManagedGridNode mainNode,
            AppliedThermalProviderAttachment attachment) {
        super(mainNode, attachment, 36);
        this.attachment = attachment;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!attachment.isEnabled()
                || !getAvailablePatterns().contains(patternDetails)
                || !(patternDetails instanceof AEProcessingPattern)
                || isBusy()
                || isBlocked()) {
            return false;
        }

        if (!attachment.commitInputs(inputHolder)) {
            return false;
        }

        ((PatternProviderLogicAccessor) (Object) this).appliedthermal$onPushPatternSuccess(patternDetails);
        saveChanges();
        return true;
    }

    @Override
    public boolean isBusy() {
        return super.isBusy() || isBlocked();
    }

    void notifyStackReturnedToNetwork(GenericStack stack) {
        ((PatternProviderLogicAccessor) (Object) this).appliedthermal$onStackReturnedToNetwork(stack);
    }

    private boolean isBlocked() {
        return getConfigManager().getSetting(Settings.BLOCKING_MODE) == YesNo.YES
                && (attachment.containsAnyInput()
                        || attachment.hasOutputItems()
                        || attachment.hasOutputFluids());
    }

}
