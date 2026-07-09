package com.recursivepineapple.appliedthermal.mixin;

import appeng.client.gui.implementations.GuiInterface;
import com.recursivepineapple.appliedthermal.client.gui.GuiOutputReturnButton;
import com.recursivepineapple.appliedthermal.container.AppliedThermalInterfaceContainer;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.network.ATNetwork;
import com.recursivepineapple.appliedthermal.network.MessageToggleOutputReturn;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiInterface.class, remap = false)
public abstract class MixinGuiInterface {

    @Shadow
    protected int guiLeft;

    @Shadow
    protected int guiTop;

    @Shadow
    protected List<GuiButton> buttonList;

    @Shadow
    public Container inventorySlots;

    @Unique
    @Nullable
    private GuiOutputReturnButton appliedthermal$outputReturn;

    @Inject(method = "addButtons", at = @At("RETURN"))
    private void appliedthermal$addOutputReturnButton(CallbackInfo ci) {
        if (appliedthermal$getMachine() != null) {
            appliedthermal$outputReturn = new GuiOutputReturnButton(guiLeft - 18, guiTop + 62);
            buttonList.add(appliedthermal$outputReturn);
        }
    }

    @Inject(method = "drawFG", at = @At("RETURN"))
    private void appliedthermal$updateOutputButton(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        AppliedThermalMachine machine = appliedthermal$getMachine();
        if (machine != null && appliedthermal$outputReturn != null) {
            appliedthermal$outputReturn.setState(
                machine.appliedthermal$getProviderAttachment().shouldReturnOutputsToNetwork());
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void appliedthermal$toggleOutputReturn(GuiButton button, CallbackInfo ci) {
        AppliedThermalMachine machine = appliedthermal$getMachine();
        if (machine == null || button != appliedthermal$outputReturn || appliedthermal$outputReturn == null) {
            return;
        }
        boolean next = !appliedthermal$outputReturn.getState();
        appliedthermal$outputReturn.setState(next);
        machine.appliedthermal$getProviderAttachment().setReturnOutputsToNetwork(next);
        ATNetwork.CHANNEL.sendToServer(new MessageToggleOutputReturn(
            machine.appliedthermal$getProviderAttachment().getTile().getPos(), next));
        ci.cancel();
    }

    @Nullable
    @Unique
    private AppliedThermalMachine appliedthermal$getMachine() {
        if (inventorySlots instanceof AppliedThermalInterfaceContainer) {
            return ((AppliedThermalInterfaceContainer) inventorySlots).appliedthermal$getMachine();
        }
        return null;
    }
}
