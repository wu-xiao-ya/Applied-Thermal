package com.recursivepineapple.appliedthermal.integration.fluxapplied;

import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;

final class NoopFluxAppliedBridge implements FluxAppliedBridge {

    static final NoopFluxAppliedBridge INSTANCE = new NoopFluxAppliedBridge();

    private NoopFluxAppliedBridge() {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public long extractEnergy(AppliedThermalProviderAttachment attachment, long amount, boolean simulate) {
        return 0;
    }

    @Override
    public long insertEnergy(AppliedThermalProviderAttachment attachment, long amount, boolean simulate) {
        return 0;
    }
}
