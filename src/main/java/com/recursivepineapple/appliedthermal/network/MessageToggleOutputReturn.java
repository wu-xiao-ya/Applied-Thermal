package com.recursivepineapple.appliedthermal.network;

import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MessageToggleOutputReturn implements IMessage {

    private BlockPos pos;
    private boolean enabled;

    public MessageToggleOutputReturn() {
    }

    public MessageToggleOutputReturn(BlockPos pos, boolean enabled) {
        this.pos = pos;
        this.enabled = enabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.enabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeBoolean(enabled);
    }

    public static final class Handler implements IMessageHandler<MessageToggleOutputReturn, IMessage> {

        @Override
        public IMessage onMessage(MessageToggleOutputReturn message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                if (message.pos == null
                    || ctx.getServerHandler().player.getDistanceSq(message.pos) > 64.0D) {
                    return;
                }
                TileEntity tile = ctx.getServerHandler().player.world.getTileEntity(message.pos);
                if (tile instanceof AppliedThermalMachine) {
                    AppliedThermalMachine machine = (AppliedThermalMachine) tile;
                    if (machine.appliedthermal$hasPatternProviderAugment()) {
                        machine.appliedthermal$getProviderAttachment()
                            .setReturnOutputsToNetwork(message.enabled);
                    }
                }
            });
            return null;
        }
    }
}
