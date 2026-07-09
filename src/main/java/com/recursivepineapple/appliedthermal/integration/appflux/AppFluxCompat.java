package com.recursivepineapple.appliedthermal.integration.appflux;

import net.minecraftforge.fml.common.Loader;

public final class AppFluxCompat {

    public static final String MOD_ID = "appflux";
    private static final AppFluxBridge BRIDGE = Loader.isModLoaded(MOD_ID) ? new ReflectiveAppFluxBridge()
        : AppFluxNoopBridge.INSTANCE;

    private AppFluxCompat() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    public static AppFluxBridge bridge() {
        return BRIDGE;
    }
}
