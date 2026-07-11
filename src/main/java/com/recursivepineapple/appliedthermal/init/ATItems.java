package com.recursivepineapple.appliedthermal.init;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import cofh.core.util.helpers.AugmentDataHelper;
import cofh.core.common.item.IAugmentItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static cofh.lib.util.constants.NBTTags.TAG_AUGMENT_TYPE_MACHINE;

public final class ATItems {

    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, AppliedThermal.MOD_ID);

    public static final RegistryObject<Item> PATTERN_PROVIDER_AUGMENT = ITEMS.register(
        "pattern_provider_augment",
        () -> new PatternProviderAugmentItem(
            new Item.Properties().stacksTo(1),
            AugmentDataHelper.builder().type(TAG_AUGMENT_TYPE_MACHINE).build()));

    private ATItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    public static Item getPatternProviderAugment() {
        return PATTERN_PROVIDER_AUGMENT.get();
    }

    private static final class PatternProviderAugmentItem extends Item implements IAugmentItem {

        private final CompoundTag augmentData;

        private PatternProviderAugmentItem(Properties properties, CompoundTag augmentData) {
            super(properties);
            this.augmentData = augmentData;
        }

        @Override
        public CompoundTag getAugmentData(ItemStack stack) {
            return augmentData.copy();
        }
    }
}
