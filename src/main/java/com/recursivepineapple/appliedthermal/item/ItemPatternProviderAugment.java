package com.recursivepineapple.appliedthermal.item;

import cofh.api.item.IAugmentItem;
import cofh.core.util.helpers.StringHelper;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.integration.thermal.ThermalMachineAugments;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class ItemPatternProviderAugment extends Item implements IAugmentItem {

    public ItemPatternProviderAugment() {
        setTranslationKey(AppliedThermal.MOD_ID + ".pattern_provider_augment");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(64);
    }

    @Override
    public AugmentType getAugmentType(ItemStack stack) {
        return AugmentType.ADVANCED;
    }

    @Override
    public String getAugmentIdentifier(ItemStack stack) {
        return ThermalMachineAugments.PATTERN_PROVIDER_AUGMENT;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        addTooltipLines(tooltip, "info.appliedthermal.augment.pattern_provider.");
        if (StringHelper.displayShiftForDetail && !StringHelper.isShiftKeyDown()) {
            tooltip.add(StringHelper.shiftForDetails());
        }
        if (StringHelper.isShiftKeyDown()) {
            addTooltipLines(tooltip, "info.appliedthermal.augment.pattern_provider.a.");
            addTooltipLines(tooltip, "info.appliedthermal.augment.pattern_provider.c.");
        }
    }

    private static void addTooltipLines(List<String> tooltip, String prefix) {
        int index = 0;
        String key = prefix + index;
        while (StringHelper.canLocalize(key)) {
            tooltip.add(StringHelper.localize(key));
            index++;
            key = prefix + index;
        }
    }
}
