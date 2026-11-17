package com.recursivepineapple.appliedthermal.network;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ATNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(AppliedThermal.MOD_ID);

    private ATNetwork() {
    }

    public static void preInit() {
        CHANNEL.registerMessage(MessageOpenPatternProviderGui.Handler.class, MessageOpenPatternProviderGui.class, 2,
            Side.SERVER);
    }
}
