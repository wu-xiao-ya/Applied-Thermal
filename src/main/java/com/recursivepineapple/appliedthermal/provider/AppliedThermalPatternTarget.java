package com.recursivepineapple.appliedthermal.provider;

import ae2.api.config.Actionable;
import ae2.api.config.PatternProviderInsertionMode;
import ae2.api.stacks.AEKey;
import ae2.helpers.patternprovider.PatternProviderTarget;
import java.util.Set;

public final class AppliedThermalPatternTarget implements PatternProviderTarget {

    private final AppliedThermalProviderAttachment attachment;

    public AppliedThermalPatternTarget(AppliedThermalProviderAttachment attachment) {
        this.attachment = attachment;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable type) {
        return attachment.insertInput(what, amount, type);
    }

    @Override
    public long insert(AEKey what, long amount, Actionable type, PatternProviderInsertionMode insertionMode) {
        return insert(what, amount, type);
    }

    @Override
    public boolean containsPatternInput(Set<AEKey> patternInputs) {
        return false;
    }

    @Override
    public boolean containsAnyStack() {
        return false;
    }

    @Override
    public boolean hasEmptySlots() {
        return true;
    }
}
