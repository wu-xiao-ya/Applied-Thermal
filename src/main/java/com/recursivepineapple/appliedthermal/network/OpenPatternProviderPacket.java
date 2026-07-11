package com.recursivepineapple.appliedthermal.network;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.recursivepineapple.appliedthermal.machine.MachineBlockEntityAccess;
import com.recursivepineapple.appliedthermal.menu.ATMenus;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

public record OpenPatternProviderPacket(BlockPos pos) {

    static void encode(OpenPatternProviderPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    static OpenPatternProviderPacket decode(FriendlyByteBuf buffer) {
        return new OpenPatternProviderPacket(buffer.readBlockPos());
    }

    static void handle(OpenPatternProviderPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> open(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void open(OpenPatternProviderPacket packet, ServerPlayer player) {
        if (player == null || !player.level().isLoaded(packet.pos)
                || player.distanceToSqr(
                        packet.pos.getX() + 0.5,
                        packet.pos.getY() + 0.5,
                        packet.pos.getZ() + 0.5) > 64.0) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);
        if (!(blockEntity instanceof MachineBlockEntityAccess access)) {
            return;
        }

        var provider = access.appliedthermal$getProviderAttachment();
        if (provider == null || !provider.isAugmentInstalled(blockEntity)
                || !provider.isNodeRunning()) {
            return;
        }

        MenuOpener.open(ATMenus.PATTERN_PROVIDER, player,
                MenuLocators.forBlockEntity(blockEntity));
    }
}
