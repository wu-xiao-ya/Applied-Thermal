package com.recursivepineapple.appliedthermal.integration.fluxapplied;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import net.minecraftforge.fml.common.Loader;

public final class FluxAppliedCompat {

    public static final String MOD_ID = "flux_applied";
    private static final FluxAppliedBridge BRIDGE = createBridge();

    private FluxAppliedCompat() {
    }

    private static FluxAppliedBridge createBridge() {
        if (!Loader.isModLoaded(MOD_ID)) {
            return NoopFluxAppliedBridge.INSTANCE;
        }
        try {
            FluxAppliedBridge bridge = new ReflectiveFluxAppliedBridge();
            if (bridge.isAvailable()) {
                return bridge;
            }
        } catch (LinkageError | RuntimeException e) {
            AppliedThermal.LOG.error("Failed to initialize Flux_Applied integration.", e);
        }
        return NoopFluxAppliedBridge.INSTANCE;
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    public static FluxAppliedBridge bridge() {
        return BRIDGE;
    }
}
