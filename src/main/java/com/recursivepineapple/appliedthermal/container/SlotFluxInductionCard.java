package com.recursivepineapple.appliedthermal.container;

import appeng.container.slot.AppEngSlot;
import com.recursivepineapple.appliedthermal.init.ATItems;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public final class SlotFluxInductionCard extends AppEngSlot {

    public SlotFluxInductionCard(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ATItems.getFluxInductionCard();
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}
