package com.recursivepineapple.appliedthermal.mixin;

import cofh.core.block.TileReconfigurable;
import cofh.core.util.core.SlotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileReconfigurable.class, remap = false)
public interface AccessorTileReconfigurable {

    @Accessor("slotConfig")
    SlotConfig appliedthermal$getSlotConfig();
}
