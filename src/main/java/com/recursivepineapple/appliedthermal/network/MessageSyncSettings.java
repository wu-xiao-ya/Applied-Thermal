package com.recursivepineapple.appliedthermal.network;

import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MessageSyncSettings implements IMessage {

    private BlockPos pos;
    private boolean returnOutputs;

    public MessageSyncSettings() {
    }

    public MessageSyncSettings(BlockPos pos, boolean returnOutputs) {
        this.pos = pos;
        this.returnOutputs = returnOutputs;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.returnOutputs = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeBoolean(returnOutputs);
    }

    public static final class Handler implements IMessageHandler<MessageSyncSettings, IMessage> {

        @Override
        public IMessage onMessage(MessageSyncSettings message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().world == null) {
                    return;
                }
                TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(message.pos);
                if (tile instanceof AppliedThermalMachine) {
                    ((AppliedThermalMachine) tile).appliedthermal$getProviderAttachment()
                        .setReturnOutputsToNetwork(message.returnOutputs);
                }
            });
            return null;
        }
    }
}
