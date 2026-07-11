package com.recursivepineapple.appliedthermal;

import com.recursivepineapple.appliedthermal.init.ATItems;
import com.recursivepineapple.appliedthermal.menu.ATMenus;
import com.recursivepineapple.appliedthermal.network.ATNetwork;
import cofh.thermal.lib.util.ThermalAugmentRules;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AppliedThermal.MOD_ID)
public final class AppliedThermal {

    public static final String MOD_ID = "appliedthermal";
    public static final String MOD_NAME = "Applied Thermal";
    public static final String VERSION = "1.20.1-0.1.0";

    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public AppliedThermal() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ATItems.register(modEventBus);
        ATMenus.init();
        ATNetwork.init();
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                ThermalAugmentRules.flagUniqueAugment(ATItems.getPatternProviderAugment()));
    }
}
