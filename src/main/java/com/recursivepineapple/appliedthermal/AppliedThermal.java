package com.recursivepineapple.appliedthermal;

import com.recursivepineapple.appliedthermal.init.ATItems;
import com.recursivepineapple.appliedthermal.integration.thermal.ThermalMachineAugments;
import com.recursivepineapple.appliedthermal.network.ATGuiHandler;
import com.recursivepineapple.appliedthermal.network.ATNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = AppliedThermal.MOD_ID,
    name = AppliedThermal.MOD_NAME,
    version = AppliedThermal.VERSION,
    dependencies = AppliedThermal.DEPENDENCIES,
    acceptedMinecraftVersions = "[1.12.2]")
public final class AppliedThermal {

    public static final String MOD_ID = "appliedthermal";
    public static final String MOD_NAME = "Applied Thermal";
    public static final String VERSION = "0.1.0";
    public static final String DEPENDENCIES =
        "required-after:ae2;required-after:thermalexpansion;required-after:cofhcore;required-after:thermalfoundation;"
            + "after:appflux";

    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @Mod.Instance(MOD_ID)
    public static AppliedThermal instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ATItems.preInit();
        ATNetwork.preInit(event.getSide());
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new ATGuiHandler());
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ThermalMachineAugments.registerMachineAugment();
    }
}
