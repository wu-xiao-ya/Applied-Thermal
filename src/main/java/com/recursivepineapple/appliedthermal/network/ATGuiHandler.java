package com.recursivepineapple.appliedthermal.network;

import ae2.core.gui.PatternContainerGuiReturnContext;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.client.gui.GuiThermalPatternProvider;
import net.minecraft.entity.player.EntityPlayer;
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
            ContainerThermalPatternProvider container = new ContainerThermalPatternProvider(player.inventory, machine);
            return PatternContainerGuiReturnContext.initializeContainer(container);
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
            ContainerThermalPatternProvider container = new ContainerThermalPatternProvider(player.inventory, machine);
            return new GuiThermalPatternProvider(container, player.inventory);
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
        return machine.appliedthermal$hasPatternProviderAugment()
            && machine.appliedthermal$getProviderAttachment().isEnabled() ? machine : null;
    }
}
