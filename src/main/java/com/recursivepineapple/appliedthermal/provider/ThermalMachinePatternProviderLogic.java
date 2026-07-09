package com.recursivepineapple.appliedthermal.provider;

import ae2.api.config.Actionable;
import ae2.api.config.BlockingMode;
import ae2.api.config.PatternProviderBlockingType;
import ae2.api.config.Settings;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.IManagedGridNode;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.crafting.pattern.AEProcessingPattern;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.helpers.patternprovider.PseudoPatternDetails;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.item.Item;

public final class ThermalMachinePatternProviderLogic extends PatternProviderLogic {

    private final AppliedThermalProviderAttachment attachment;
    private boolean hasLastSuccessfulPatternHash;
    private int lastSuccessfulPatternHash;

    public ThermalMachinePatternProviderLogic(IManagedGridNode mainNode, PatternProviderLogicHost host, Item machineType,
                                              int patternInventorySize,
                                              AppliedThermalProviderAttachment attachment) {
        super(mainNode, host, machineType, patternInventorySize);
        this.attachment = attachment;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int multiplier) {
        IPatternDetails basePatternDetails = PseudoPatternDetails.unwrap(patternDetails);
        if (!attachment.isEnabled() || !getAvailablePatterns().contains(patternDetails)
            && !getAvailablePatterns().contains(basePatternDetails)) {
            return false;
        }
        if (!(basePatternDetails instanceof AEProcessingPattern) || multiplier != 1) {
            return false;
        }
        if (isBlocked(basePatternDetails, inputHolder)) {
            return false;
        }
        if (!attachment.canAcceptInputs(inputHolder)) {
            return false;
        }
        basePatternDetails.pushInputsToExternalInventory(inputHolder,
            (what, amount) -> attachment.insertInput(what, amount, Actionable.MODULATE));
        this.hasLastSuccessfulPatternHash = true;
        this.lastSuccessfulPatternHash = getPatternHash(basePatternDetails);
        saveChanges();
        return true;
    }

    @Override
    public boolean canMergePatternPush(IPatternDetails patternDetails) {
        return false;
    }

    @Override
    public int getMaxPatternPushMultiplier(IPatternDetails patternDetails, int maxMultiplier) {
        return 0;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    private boolean isBlocked(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        BlockingMode mode = getConfigManager().getSetting(Settings.BLOCKING_MODE);
        if (mode == BlockingMode.NO || shouldBypassSmartBlocking(patternDetails)) {
            return false;
        }
        if (mode == BlockingMode.STRONG) {
            for (KeyCounter counter : inputHolder) {
                for (Object2LongMap.Entry<AEKey> input : counter) {
                    if (attachment.containsInput(input.getKey())) {
                        return true;
                    }
                }
            }
            return false;
        }
        return attachment.containsAnyInput();
    }

    private boolean shouldBypassSmartBlocking(IPatternDetails patternDetails) {
        return getConfigManager().getSetting(Settings.PATTERN_PROVIDER_BLOCKING_TYPE)
            == PatternProviderBlockingType.SMART
            && hasLastSuccessfulPatternHash
            && lastSuccessfulPatternHash == getPatternHash(patternDetails);
    }

    private int getPatternHash(IPatternDetails patternDetails) {
        return patternDetails.getDefinition().hashCode();
    }
}
