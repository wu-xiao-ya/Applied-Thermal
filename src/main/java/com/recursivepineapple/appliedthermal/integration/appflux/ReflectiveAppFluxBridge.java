package com.recursivepineapple.appliedthermal.integration.appflux;

import ae2.api.config.Actionable;
import ae2.api.networking.IGrid;
import ae2.api.networking.security.IActionHost;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.storage.IStorageService;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

final class ReflectiveAppFluxBridge implements AppFluxBridge {

    private static final String AF_ITEMS = "com.glodblock.github.appflux.common.AFItemAndBlock";
    private static final String AF_ITEM_FIELD = "INDUCTION_CARD";
    private static final String FLUX_KEY = "com.glodblock.github.appflux.common.me.key.FluxKey";
    private static final String ENERGY_TYPE = "com.glodblock.github.appflux.common.me.key.type.EnergyType";

    @Nullable
    private final Item inductionCard;
    @Nullable
    private final Object fluxKeyFe;
    @Nullable
    private final Method extractMethod;
    @Nullable
    private final Method insertMethod;

    ReflectiveAppFluxBridge() {
        this.inductionCard = resolveInductionCard();
        this.fluxKeyFe = resolveFluxKeyFe();
        Method[] methods = resolveStorageMethods();
        this.extractMethod = methods[0];
        this.insertMethod = methods[1];
    }

    @Override
    public boolean isAvailable() {
        return inductionCard != null && fluxKeyFe != null && extractMethod != null && insertMethod != null;
    }

    @Override
    @Nullable
    public Item getInductionCardItem() {
        return inductionCard;
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
        if (amount <= 0 || !isAvailable()) {
            return 0;
        }
        IStorageService storage = getStorage(machine);
        if (storage == null) {
            return 0;
        }
        try {
            Object inventory = storage.getInventory();
            Object result = extractMethod.invoke(inventory, fluxKeyFe, amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE,
                IActionSource.ofMachine((IActionHost) machine));
            return result instanceof Number ? ((Number) result).longValue() : 0L;
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    @Nullable
    private static IStorageService getStorage(AppliedThermalMachine machine) {
        IGrid grid = machine.appliedthermal$getProviderAttachment().getGrid();
        return grid == null ? null : grid.getStorageService();
    }

    @Nullable
    private static Item resolveInductionCard() {
        try {
            Class<?> afItems = Class.forName(AF_ITEMS);
            Field field = afItems.getField(AF_ITEM_FIELD);
            return (Item) field.get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nullable
    private static Object resolveFluxKeyFe() {
        try {
            Class<?> energyTypeClass = Class.forName(ENERGY_TYPE);
            Object feType = Enum.valueOf((Class<Enum>) energyTypeClass.asSubclass(Enum.class), "FE");
            Class<?> fluxKeyClass = Class.forName(FLUX_KEY);
            Method of = fluxKeyClass.getMethod("of", energyTypeClass);
            return of.invoke(null, feType);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method[] resolveStorageMethods() {
        try {
            Class<?> meStorage = Class.forName("ae2.api.storage.MEStorage");
            Class<?> aeKey = Class.forName("ae2.api.stacks.AEKey");
            Class<?> actionable = Class.forName("ae2.api.config.Actionable");
            Class<?> actionSource = Class.forName("ae2.api.networking.security.IActionSource");
            Method extract = meStorage.getMethod("extract", aeKey, long.class, actionable, actionSource);
            Method insert = meStorage.getMethod("insert", aeKey, long.class, actionable, actionSource);
            return new Method[] {extract, insert};
        } catch (ReflectiveOperationException ignored) {
            return new Method[] {null, null};
        }
    }
}
