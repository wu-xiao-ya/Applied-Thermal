package com.recursivepineapple.appliedthermal.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.IEarlyMixinLoader;

@IFMLLoadingPlugin.Name("AppliedThermalCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class AppliedThermalCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    private static final String AE_MIXIN_CONFIG = "appliedthermal.ae.mixins.json";

    public AppliedThermalCore() {
        MixinBootstrap.init();
    }

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList(AE_MIXIN_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(Context context) {
        return AE_MIXIN_CONFIG.equals(context.mixinConfig()) && context.isModPresent("ae2");
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        return AE_MIXIN_CONFIG.equals(mixinConfig);
    }

    @Override
    public void onMixinConfigQueued(Context context) {
    }

    @Override
    public void onMixinConfigQueued(String mixinConfig) {
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
