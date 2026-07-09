package com.recursivepineapple.appliedthermal.network;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ATNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(AppliedThermal.MOD_ID);

    private ATNetwork() {
    }

    public static void preInit(Side side) {
        CHANNEL.registerMessage(MessageToggleOutputReturn.Handler.class, MessageToggleOutputReturn.class, 0,
            Side.SERVER);
        CHANNEL.registerMessage(MessageOpenPatternProviderGui.Handler.class, MessageOpenPatternProviderGui.class, 2,
            Side.SERVER);
        if (side.isClient()) {
            CHANNEL.registerMessage(MessageSyncSettings.Handler.class, MessageSyncSettings.class, 1, Side.CLIENT);
        }
    }
}
