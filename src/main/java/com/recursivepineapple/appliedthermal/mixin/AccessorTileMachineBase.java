package com.recursivepineapple.appliedthermal.mixin;

import cofh.thermalexpansion.block.machine.TileMachineBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileMachineBase.class, remap = false)
public interface AccessorTileMachineBase {

    @Accessor("processRem")
    int appliedthermal$getProcessRem();
}
