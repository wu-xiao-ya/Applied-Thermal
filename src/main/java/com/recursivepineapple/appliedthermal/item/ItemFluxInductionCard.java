package com.recursivepineapple.appliedthermal.item;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.integration.fluxapplied.FluxAppliedCompat;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemFluxInductionCard extends Item {

    public ItemFluxInductionCard() {
        setTranslationKey(AppliedThermal.MOD_ID + ".flux_induction_card");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(64);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (FluxAppliedCompat.isLoaded() && isInCreativeTab(tab)) {
            items.add(new ItemStack(this));
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(net.minecraft.client.resources.I18n.format("info.appliedthermal.flux_induction_card.0"));
        if (!FluxAppliedCompat.isLoaded()) {
            tooltip.add(TextFormatting.RED
                + net.minecraft.client.resources.I18n.format("info.appliedthermal.flux_induction_card.missing"));
        }
    }
}
