package com.recursivepineapple.appliedthermal.integration.thermal;

import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;

public interface AppliedThermalMachine {

    AppliedThermalProviderAttachment appliedthermal$getProviderAttachment();

    AppliedThermalFluxAttachment appliedthermal$getFluxAttachment();

    boolean appliedthermal$hasPatternProviderAugment();

    boolean appliedthermal$hasFluxInductionSupport();
}
