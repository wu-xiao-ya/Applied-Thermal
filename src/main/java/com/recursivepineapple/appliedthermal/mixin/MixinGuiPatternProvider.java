package com.recursivepineapple.appliedthermal.mixin;

import ae2.client.gui.implementations.GuiPatternProvider;
import ae2.client.gui.style.GuiStyle;
import ae2.container.implementations.ContainerPatternProvider;
import com.recursivepineapple.appliedthermal.client.gui.GuiOutputReturnButton;
import com.recursivepineapple.appliedthermal.container.AppliedThermalPatternProviderContainer;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.network.ATNetwork;
import com.recursivepineapple.appliedthermal.network.MessageToggleOutputReturn;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiPatternProvider.class, remap = false)
public abstract class MixinGuiPatternProvider extends GuiContainer {

    @Unique
    @Nullable
    private GuiOutputReturnButton appliedthermal$outputReturn;

    protected MixinGuiPatternProvider(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Shadow
    protected abstract <B extends GuiButton> B addToLeftToolbar(B button);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void appliedthermal$addOutputReturnButton(ContainerPatternProvider container,
                                                      InventoryPlayer playerInventory,
                                                      ITextComponent unusedTitle,
                                                      GuiStyle style,
                                                      CallbackInfo ci) {
        AppliedThermalMachine machine = appliedthermal$getMachine();
        if (machine == null) {
            return;
        }
        appliedthermal$outputReturn =
            addToLeftToolbar(new GuiOutputReturnButton(this::appliedthermal$toggleOutputReturn));
        appliedthermal$outputReturn.setState(
            machine.appliedthermal$getProviderAttachment().shouldReturnOutputsToNetwork());
    }

    @Inject(method = "updateBeforeRender", at = @At("RETURN"))
    private void appliedthermal$updateOutputReturnButton(CallbackInfo ci) {
        AppliedThermalMachine machine = appliedthermal$getMachine();
        if (machine != null && appliedthermal$outputReturn != null) {
            appliedthermal$outputReturn.setState(
                machine.appliedthermal$getProviderAttachment().shouldReturnOutputsToNetwork());
        }
    }

    @Unique
    private void appliedthermal$toggleOutputReturn() {
        AppliedThermalMachine machine = appliedthermal$getMachine();
        if (machine == null || appliedthermal$outputReturn == null) {
            return;
        }
        boolean next = !appliedthermal$outputReturn.getState();
        appliedthermal$outputReturn.setState(next);
        machine.appliedthermal$getProviderAttachment().setReturnOutputsToNetwork(next);
        ATNetwork.CHANNEL.sendToServer(new MessageToggleOutputReturn(
            machine.appliedthermal$getProviderAttachment().getTileEntity().getPos(), next));
    }

    @Nullable
    @Unique
    private AppliedThermalMachine appliedthermal$getMachine() {
        if (inventorySlots instanceof AppliedThermalPatternProviderContainer) {
            return ((AppliedThermalPatternProviderContainer) inventorySlots).appliedthermal$getMachine();
        }
        return null;
    }
}
