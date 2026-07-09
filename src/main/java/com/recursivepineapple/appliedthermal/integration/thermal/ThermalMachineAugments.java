package com.recursivepineapple.appliedthermal.integration.thermal;

import ae2.api.upgrades.Upgrades;
import ae2.core.AEConfig;
import ae2.core.definitions.AEItems;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.integration.appflux.AppFluxCompat;
import com.recursivepineapple.appliedthermal.init.ATItems;
import java.util.HashSet;
import net.minecraft.item.Item;

public final class ThermalMachineAugments {

    public static final String PATTERN_PROVIDER_AUGMENT = AppliedThermal.MOD_ID + ":pattern_provider";

    private ThermalMachineAugments() {
    }

    public static void registerMachineAugment() {
        for (HashSet<String> validAugments : TileMachineBase.VALID_AUGMENTS) {
            if (validAugments != null) {
                validAugments.add(PATTERN_PROVIDER_AUGMENT);
            }
        }
        Upgrades.add(AEItems.PATTERN_EXPANSION_CARD.item(), ATItems.getPatternProviderAugment(),
            AEConfig.instance().getPatternProviderExpansionCardLimit());
        Upgrades.add(AEItems.PSEUDO_CRAFTING_CARD.item(), ATItems.getPatternProviderAugment(), 1);
        Item inductionCard = AppFluxCompat.bridge().getInductionCardItem();
        if (inductionCard != null) {
            Upgrades.add(inductionCard, ATItems.getPatternProviderAugment(), 1, "group.pattern_provider.name");
        }
    }
}
