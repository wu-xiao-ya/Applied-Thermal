package com.recursivepineapple.appliedthermal.mixin;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.GenericStack;
import ae2.helpers.patternprovider.PatternProviderLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = PatternProviderLogic.class, remap = false)
public interface PatternProviderLogicInvoker {

    @Invoker("onPushPatternSuccess")
    void appliedthermal$onPushPatternSuccess(IPatternDetails pattern);

    @Invoker("onStackReturnedToNetwork")
    void appliedthermal$onStackReturnedToNetwork(GenericStack stack);
}
