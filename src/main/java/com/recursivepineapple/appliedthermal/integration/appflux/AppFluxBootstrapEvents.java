package com.recursivepineapple.appliedthermal.integration.appflux;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = AppliedThermal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AppFluxBootstrapEvents {

    private AppFluxBootstrapEvents() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AppFluxCompat::bootstrap);
    }
}
