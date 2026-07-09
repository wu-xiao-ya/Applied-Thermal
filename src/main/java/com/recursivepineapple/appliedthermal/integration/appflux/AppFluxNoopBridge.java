package com.recursivepineapple.appliedthermal.integration.appflux;

import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

final class AppFluxNoopBridge implements AppFluxBridge {

    static final AppFluxNoopBridge INSTANCE = new AppFluxNoopBridge();

    private AppFluxNoopBridge() {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    @Nullable
    public Item getInductionCardItem() {
        return null;
    }

    @Override
    public void onLoad(TileMachineBase tile) {
    }

    @Override
    public void onUnload(TileMachineBase tile) {
    }

    @Override
    public void readFromNBT(TileMachineBase tile, NBTTagCompound tag) {
    }

    @Override
    public void writeToNBT(TileMachineBase tile, NBTTagCompound tag) {
    }

    @Override
    public long extractEnergyFromGrid(AppliedThermalMachine machine, long amount, boolean simulate) {
        return 0;
    }
}
