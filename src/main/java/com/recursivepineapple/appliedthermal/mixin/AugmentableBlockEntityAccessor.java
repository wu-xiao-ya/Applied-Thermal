package com.recursivepineapple.appliedthermal.mixin;

import cofh.lib.common.fluid.ManagedTankInv;
import cofh.lib.common.inventory.ItemStorageCoFH;
import cofh.lib.common.inventory.ManagedItemInv;
import cofh.thermal.lib.common.block.entity.AugmentableBlockEntity;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AugmentableBlockEntity.class, remap = false)
public interface AugmentableBlockEntityAccessor {

    @Accessor("augments")
    List<ItemStorageCoFH> appliedthermal$getAugmentSlots();

    @Accessor("inventory")
    ManagedItemInv appliedthermal$getMachineInventory();

    @Accessor("tankInv")
    ManagedTankInv appliedthermal$getMachineTankInventory();
}
