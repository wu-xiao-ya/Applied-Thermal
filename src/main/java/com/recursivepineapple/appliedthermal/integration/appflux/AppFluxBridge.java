package com.recursivepineapple.appliedthermal.integration.appflux;

import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import net.minecraft.world.level.ItemLike;

public interface AppFluxBridge {

    boolean isPresent();

    void registerUpgrade(ItemLike machineIcon);

    boolean hasInductionCard(Object providerLogic);

    long transferEnergy(Object machine, MEStorage storage, IActionSource source);
}
