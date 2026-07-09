package com.recursivepineapple.appliedthermal.core;

import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.ILateMixinLoader;
import zone.rong.mixinbooter.MixinLoader;

@MixinLoader
public final class AppliedThermalLateMixinLoader implements ILateMixinLoader {

    private static final String THERMAL_MIXIN_CONFIG = "appliedthermal.thermal.mixins.json";

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList(THERMAL_MIXIN_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(Context context) {
        return THERMAL_MIXIN_CONFIG.equals(context.mixinConfig())
            && Loader.isModLoaded("thermalexpansion")
            && Loader.isModLoaded("cofhcore");
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        return THERMAL_MIXIN_CONFIG.equals(mixinConfig);
    }

    @Override
    public void onMixinConfigQueued(Context context) {
    }

    @Override
    public void onMixinConfigQueued(String mixinConfig) {
    }
}
