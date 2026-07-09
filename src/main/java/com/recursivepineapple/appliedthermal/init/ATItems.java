package com.recursivepineapple.appliedthermal.init;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.item.ItemPatternProviderAugment;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class ATItems {

    public static ItemPatternProviderAugment patternProviderAugment;

    private ATItems() {
    }

    public static void preInit() {
        patternProviderAugment = new ItemPatternProviderAugment();
        ForgeRegistries.ITEMS.register(patternProviderAugment.setRegistryName(AppliedThermal.MOD_ID, "pattern_provider_augment"));
    }

    public static Item getPatternProviderAugment() {
        return patternProviderAugment;
    }
}
