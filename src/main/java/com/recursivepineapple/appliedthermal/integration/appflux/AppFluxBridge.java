package com.recursivepineapple.appliedthermal.integration.appflux;

import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

public interface AppFluxBridge {

    String TAG_UPGRADES = "Upgrades";

    boolean isAvailable();

    @Nullable
    Item getInductionCardItem();

    void onLoad(TileMachineBase tile);

    void onUnload(TileMachineBase tile);

    void readFromNBT(TileMachineBase tile, NBTTagCompound tag);

    void writeToNBT(TileMachineBase tile, NBTTagCompound tag);

    long extractEnergyFromGrid(AppliedThermalMachine machine, long amount, boolean simulate);
}
