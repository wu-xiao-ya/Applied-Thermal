package com.recursivepineapple.appliedthermal.network;

import ae2.container.implementations.ContainerPatternProvider;
import ae2.client.gui.implementations.GuiPatternProvider;
import ae2.client.gui.style.GuiStyleManager;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ATGuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        AppliedThermalMachine machine = getEnabledMachine(id, world, x, y, z);
        if (machine == null) {
            return null;
        }
        if (id == ATGuiIds.PATTERN_PROVIDER) {
            if (!(machine instanceof PatternProviderLogicHost)) {
                return null;
            }
            if (player instanceof EntityPlayerMP) {
                ATNetwork.CHANNEL.sendTo(
                    new MessageSyncSettings(new BlockPos(x, y, z),
                        machine.appliedthermal$getProviderAttachment().shouldReturnOutputsToNetwork()),
                    (EntityPlayerMP) player);
            }
            return new ContainerPatternProvider(player.inventory, (PatternProviderLogicHost) machine);
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        AppliedThermalMachine machine = getEnabledMachine(id, world, x, y, z);
        if (machine == null) {
            return null;
        }
        if (id == ATGuiIds.PATTERN_PROVIDER) {
            if (!(machine instanceof PatternProviderLogicHost)) {
                return null;
            }
            ContainerPatternProvider container =
                new ContainerPatternProvider(player.inventory, (PatternProviderLogicHost) machine);
            return new GuiPatternProvider(container, player.inventory, null,
                GuiStyleManager.loadStyleDoc("/screens/pattern_provider.json"));
        }
        return null;
    }

    private static AppliedThermalMachine getEnabledMachine(int id, World world, int x, int y, int z) {
        if (id != ATGuiIds.PATTERN_PROVIDER) {
            return null;
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tile instanceof TileMachineBase) || !(tile instanceof AppliedThermalMachine)) {
            return null;
        }
        AppliedThermalMachine machine = (AppliedThermalMachine) tile;
        return machine.appliedthermal$hasPatternProviderAugment() ? machine : null;
    }
}
