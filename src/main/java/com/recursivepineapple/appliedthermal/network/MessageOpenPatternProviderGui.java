package com.recursivepineapple.appliedthermal.network;

import appeng.api.util.AEPartLocation;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MessageOpenPatternProviderGui implements IMessage {

    private BlockPos pos;

    public MessageOpenPatternProviderGui() {
    }

    public MessageOpenPatternProviderGui(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
    }

    public static final class Handler implements IMessageHandler<MessageOpenPatternProviderGui, IMessage> {

        @Override
        public IMessage onMessage(MessageOpenPatternProviderGui message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (message.pos == null || player.getDistanceSq(message.pos) > 64.0D) {
                    return;
                }
                TileEntity tile = player.world.getTileEntity(message.pos);
                if (tile instanceof AppliedThermalMachine
                    && ((AppliedThermalMachine) tile).appliedthermal$hasPatternProviderAugment()) {
                    Platform.openGUI(player, tile, AEPartLocation.INTERNAL, GuiBridge.GUI_INTERFACE);
                }
            });
            return null;
        }
    }
}
