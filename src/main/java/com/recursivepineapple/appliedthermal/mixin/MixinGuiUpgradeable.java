package com.recursivepineapple.appliedthermal.mixin;

import appeng.client.gui.implementations.GuiInterface;
import appeng.client.gui.implementations.GuiUpgradeable;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.container.AppliedThermalInterfaceContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiUpgradeable.class, remap = false)
public abstract class MixinGuiUpgradeable extends GuiContainer {

    @Unique
    private static final ResourceLocation APPLIEDTHERMAL$EXTENSION =
        new ResourceLocation(AppliedThermal.MOD_ID, "textures/gui/interface_extension.png");

    protected MixinGuiUpgradeable(Container container) {
        super(container);
    }

    @Inject(method = "drawBG", at = @At("RETURN"))
    private void appliedthermal$drawFluxSlotExtension(int offsetX, int offsetY, int mouseX, int mouseY,
                                                       CallbackInfo ci) {
        if (!((Object) this instanceof GuiInterface)
            || !(inventorySlots instanceof AppliedThermalInterfaceContainer)
            || ((AppliedThermalInterfaceContainer) inventorySlots).appliedthermal$getMachine() == null) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(APPLIEDTHERMAL$EXTENSION);
        drawTexturedModalRect(offsetX + 177, offsetY + 79, 0, 7, 35, 25);
    }
}
