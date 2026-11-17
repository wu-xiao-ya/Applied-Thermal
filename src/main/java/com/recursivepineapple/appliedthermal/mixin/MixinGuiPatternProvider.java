package com.recursivepineapple.appliedthermal.mixin;

import ae2.api.config.LockCraftingMode;
import ae2.api.config.Settings;
import ae2.client.gui.implementations.GuiPatternProvider;
import ae2.client.gui.widgets.SettingToggleButton;
import ae2.container.implementations.ContainerPatternProvider;
import ae2.core.network.InitNetwork;
import ae2.core.network.serverbound.ConfigValueServerPacket;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = GuiPatternProvider.class, remap = false)
public abstract class MixinGuiPatternProvider {

    @ModifyArg(method = "<init>",
        slice = @Slice(from = @At(value = "FIELD",
            target = "Lae2/api/config/Settings;LOCK_CRAFTING_MODE:Lae2/api/config/Setting;")),
        at = @At(value = "INVOKE", ordinal = 0,
            target = "Lae2/client/gui/implementations/GuiPatternProvider;addToLeftToolbar"
                + "(Lnet/minecraft/client/gui/GuiButton;)Lnet/minecraft/client/gui/GuiButton;"),
        index = 0)
    private GuiButton appliedthermal$restrictCraftingLockButton(GuiButton original) {
        ContainerPatternProvider container = (ContainerPatternProvider) ((GuiContainer) (Object) this).inventorySlots;
        if (!(container.getHost() instanceof AppliedThermalMachine)) {
            return original;
        }

        return new SettingToggleButton<>(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE,
            mode -> mode == LockCraftingMode.NONE || mode == LockCraftingMode.LOCK_UNTIL_RESULT,
            (button, backwards) -> appliedthermal$sendCraftingLock(container, button,
                button.getNextValue(backwards)),
            (button, value) -> appliedthermal$sendCraftingLock(container, button, value));
    }

    @Unique
    private static void appliedthermal$sendCraftingLock(ContainerPatternProvider container,
                                                       SettingToggleButton<LockCraftingMode> button,
                                                       LockCraftingMode mode) {
        button.set(mode);
        InitNetwork.sendToServer(new ConfigValueServerPacket(container.windowId, Settings.LOCK_CRAFTING_MODE, mode));
    }
}
