package com.recursivepineapple.appliedthermal.integration.appflux;

import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import net.minecraft.world.level.ItemLike;

final class AppFluxNoopBridge implements AppFluxBridge {

    static final AppFluxNoopBridge INSTANCE = new AppFluxNoopBridge();

    private AppFluxNoopBridge() {
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public void registerUpgrade(ItemLike machineIcon) {
    }

    @Override
    public boolean hasInductionCard(Object providerLogic) {
        return false;
    }

    @Override
    public long transferEnergy(Object machine, MEStorage storage, IActionSource source) {
        return 0L;
    }
}
