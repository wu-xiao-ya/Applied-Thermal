package com.recursivepineapple.appliedthermal.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "yalter.mousetweaks.handlers.IMTModGuiContainer2Handler", remap = false)
public abstract class MixinMouseTweaksGuiContainer2Handler {

    @Inject(method = "isMouseTweaksDisabled", at = @At("HEAD"), cancellable = true)
    private void appliedthermal$handleLegacyGuiApi(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
