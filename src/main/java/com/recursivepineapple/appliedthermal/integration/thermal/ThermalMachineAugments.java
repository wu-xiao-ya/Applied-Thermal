package com.recursivepineapple.appliedthermal.integration.thermal;

import appeng.api.config.Upgrades;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.init.ATItems;
import java.util.HashSet;
import net.minecraft.item.ItemStack;

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
        ItemStack provider = new ItemStack(ATItems.getPatternProviderAugment());
        Upgrades.PATTERN_EXPANSION.registerItem(provider, 3);
        Upgrades.CRAFTING.registerItem(provider, 1);
    }
}
