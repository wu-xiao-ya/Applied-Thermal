package com.recursivepineapple.appliedthermal.network;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ATNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AppliedThermal.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ATNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(
                0,
                OpenPatternProviderPacket.class,
                OpenPatternProviderPacket::encode,
                OpenPatternProviderPacket::decode,
                OpenPatternProviderPacket::handle);
    }

    public static void openPatternProvider(net.minecraft.core.BlockPos pos) {
        CHANNEL.sendToServer(new OpenPatternProviderPacket(pos));
    }
}
