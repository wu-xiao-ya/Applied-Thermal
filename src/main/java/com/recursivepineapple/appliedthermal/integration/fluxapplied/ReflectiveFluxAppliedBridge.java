package com.recursivepineapple.appliedthermal.integration.fluxapplied;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

final class ReflectiveFluxAppliedBridge implements FluxAppliedBridge {

    private static final String CHANNEL_CLASS = "com.flux_applied.ae2.FluxStorageChannel";
    private static final String STACK_CLASS = "com.flux_applied.ae2.FluxStack";

    private final IStorageChannel channel;
    private final Constructor<?> stackConstructor;

    ReflectiveFluxAppliedBridge() {
        try {
            Class<?> channelClass = Class.forName(CHANNEL_CLASS);
            Field instanceField = channelClass.getField("INSTANCE");
            this.channel = (IStorageChannel) instanceField.get(null);
            Class<?> stackClass = Class.forName(STACK_CLASS);
            this.stackConstructor = stackClass.getConstructor(long.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Flux_Applied 1.0.0 API was not found.", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return channel != null && stackConstructor != null;
    }

    @Override
    public long extractEnergy(AppliedThermalProviderAttachment attachment, long amount, boolean simulate) {
        if (amount <= 0 || !attachment.getProxy().isActive()) {
            return 0;
        }
        try {
            IMEInventory inventory = attachment.getProxy().getStorage().getInventory(channel);
            IAEStack request = newStack(amount);
            IAEStack extracted = (IAEStack) inventory.extractItems(
                request, simulate ? Actionable.SIMULATE : Actionable.MODULATE,
                new MachineSource((IActionHost) attachment.getTile()));
            return extracted == null ? 0L : extracted.getStackSize();
        } catch (GridAccessException | ReflectiveOperationException | ClassCastException ignored) {
            return 0;
        }
    }

    @Override
    public long insertEnergy(AppliedThermalProviderAttachment attachment, long amount, boolean simulate) {
        if (amount <= 0 || !attachment.getProxy().isActive()) {
            return 0;
        }
        try {
            IMEInventory inventory = attachment.getProxy().getStorage().getInventory(channel);
            IAEStack input = newStack(amount);
            IAEStack remainder = (IAEStack) inventory.injectItems(
                input, simulate ? Actionable.SIMULATE : Actionable.MODULATE,
                new MachineSource((IActionHost) attachment.getTile()));
            return amount - (remainder == null ? 0L : remainder.getStackSize());
        } catch (GridAccessException | ReflectiveOperationException | ClassCastException ignored) {
            return 0;
        }
    }

    private IAEStack newStack(long amount) throws ReflectiveOperationException {
        return (IAEStack) stackConstructor.newInstance(amount);
    }
}
