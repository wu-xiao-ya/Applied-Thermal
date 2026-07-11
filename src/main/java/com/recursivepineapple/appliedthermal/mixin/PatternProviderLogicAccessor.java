package com.recursivepineapple.appliedthermal.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = PatternProviderLogic.class, remap = false)
public interface PatternProviderLogicAccessor {

    @Invoker("onPushPatternSuccess")
    void appliedthermal$onPushPatternSuccess(IPatternDetails pattern);

    @Invoker("onStackReturnedToNetwork")
    void appliedthermal$onStackReturnedToNetwork(GenericStack stack);
}
