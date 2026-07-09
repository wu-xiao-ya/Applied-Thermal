package com.recursivepineapple.appliedthermal.integration.thermal;

import cofh.core.block.TilePowered;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.appflux.AppFluxBridge;
import com.recursivepineapple.appliedthermal.integration.appflux.AppFluxCompat;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public final class AppliedThermalFluxAttachment {

    private static final String TAG_ROOT = "AppliedThermalFlux";

    private final TileMachineBase tile;
    private final AppFluxBridge bridge;
    private final FluxEnergyStorage energyStorage = new FluxEnergyStorage();
    private boolean loaded;
    @Nullable
    private NBTTagCompound savedState;

    public AppliedThermalFluxAttachment(TileMachineBase tile) {
        this.tile = tile;
        this.bridge = AppFluxCompat.bridge();
    }

    public TileMachineBase getTile() {
        return tile;
    }

    public boolean hasFluxInductionSupport() {
        return bridge.isAvailable();
    }

    public boolean isEnabled() {
        return hasFluxInductionSupport() && isInductionCardInstalled();
    }

    public void onReady() {
        this.loaded = true;
        bridge.onLoad(tile);
        applySavedState();
    }

    public void invalidate() {
        captureState();
        this.loaded = false;
        bridge.onUnload(tile);
    }

    public void tickServer() {
        if (!loaded || !isEnabled() || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        if (!(tile instanceof TilePowered)) {
            return;
        }
        TilePowered powered = (TilePowered) tile;
        int space = powered.getMaxEnergyStored(null) - powered.getEnergyStored(null);
        if (space <= 0) {
            return;
        }
        long extracted = bridge.extractEnergyFromGrid((AppliedThermalMachine) tile, space, false);
        if (extracted <= 0) {
            return;
        }
        powered.receiveEnergy(null, clampToInt(extracted), false);
        tile.markDirty();
    }

    public void readFromNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey(TAG_ROOT)) {
            return;
        }
        this.savedState = nbt.getCompoundTag(TAG_ROOT).copy();
        applySavedState();
    }

    public void writeToNBT(NBTTagCompound nbt) {
        captureState();
        if (savedState != null && !savedState.getKeySet().isEmpty()) {
            nbt.setTag(TAG_ROOT, savedState.copy());
        }
    }

    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return isEnabled() && capability == CapabilityEnergy.ENERGY;
    }

    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (!isEnabled() || capability != CapabilityEnergy.ENERGY) {
            return null;
        }
        return CapabilityEnergy.ENERGY.cast(energyStorage);
    }

    public IEnergyStorage getEnergyStorage(@Nullable EnumFacing facing) {
        return energyStorage;
    }

    public boolean isInductionCardInstalled() {
        Item card = bridge.getInductionCardItem();
        return card != null
            && tile instanceof AppliedThermalMachine
            && ((AppliedThermalMachine) tile).appliedthermal$getProviderAttachment().getUpgrades().isInstalled(card);
    }

    private void captureState() {
        NBTTagCompound tag = new NBTTagCompound();
        bridge.writeToNBT(tile, tag);
        this.savedState = tag.getKeySet().isEmpty() ? null : tag;
    }

    private void applySavedState() {
        if (savedState == null) {
            return;
        }
        NBTTagCompound tag = savedState.copy();
        bridge.readFromNBT(tile, tag);
    }

    private final class FluxEnergyStorage implements IEnergyStorage {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!isEnabled()) {
                return 0;
            }
            return clampToInt(bridge.extractEnergyFromGrid((AppliedThermalMachine) tile, maxReceive, simulate));
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!isEnabled()) {
                return 0;
            }
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return isEnabled();
        }
    }

    private static int clampToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int) value);
    }
}
