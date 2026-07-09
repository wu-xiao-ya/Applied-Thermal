package com.recursivepineapple.appliedthermal.integration.fluxapplied;

import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;

public interface FluxAppliedBridge {

    boolean isAvailable();

    long extractEnergy(AppliedThermalProviderAttachment attachment, long amount, boolean simulate);

    long insertEnergy(AppliedThermalProviderAttachment attachment, long amount, boolean simulate);
}
