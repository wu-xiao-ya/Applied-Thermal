package com.recursivepineapple.appliedthermal.mixin;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "yalter.mousetweaks.api.IMTModGuiContainer2", remap = false)
public interface MixinMouseTweaksGuiContainer2 {

    default boolean MT_isMouseTweaksDisabled() {
        return false;
    }

    default boolean MT_isWheelTweakDisabled() {
        return false;
    }

    default Container MT_getContainer() {
        return null;
    }

    default Slot MT_getSlotUnderMouse() {
        return null;
    }

    default boolean MT_isCraftingOutput(Slot slot) {
        return false;
    }

    default boolean MT_isIgnored(Slot slot) {
        return false;
    }

    default boolean MT_disableRMBDraggingFunctionality() {
        return false;
    }
}
