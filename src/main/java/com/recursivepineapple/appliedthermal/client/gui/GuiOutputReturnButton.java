package com.recursivepineapple.appliedthermal.client.gui;

import appeng.client.gui.widgets.ITooltip;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import net.minecraft.init.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

public final class GuiOutputReturnButton extends GuiButton implements ITooltip {

    private static final ItemStack PISTON = new ItemStack(Blocks.PISTON);
    private static final ResourceLocation DISABLED_OVERLAY =
        new ResourceLocation(AppliedThermal.MOD_ID, "textures/gui/output_return.png");

    private boolean active;

    public GuiOutputReturnButton(int x, int y) {
        super(0, x, y, 18, 18, "");
    }

    public void setState(boolean active) {
        this.active = active;
    }

    public boolean getState() {
        return active;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        minecraft.getTextureManager().bindTexture(DISABLED_OVERLAY);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(x, y, 0, 0, 18, 18);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
        minecraft.getRenderItem().renderItemAndEffectIntoGUI(PISTON, x + 1, y + 1);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        if (!active) {
            minecraft.getTextureManager().bindTexture(DISABLED_OVERLAY);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedModalRect(x + 1, y + 1, 18, 0, 16, 16);
        }
        mouseDragged(minecraft, mouseX, mouseY);
    }

    @Override
    public String getMessage() {
        return I18n.translateToLocal("gui.appliedthermal.return_outputs")
            + '\n' + I18n.translateToLocal(active
            ? "gui.appliedthermal.return_outputs.enabled"
            : "gui.appliedthermal.return_outputs.disabled");
    }

    @Override
    public int xPos() {
        return x;
    }

    @Override
    public int yPos() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
