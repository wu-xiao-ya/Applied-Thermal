package com.recursivepineapple.appliedthermal.client.gui;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class OutputReturnButton extends Button {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AppliedThermal.MOD_ID, "textures/guis/piston_switch.png");

    private boolean state = true;

    OutputReturnButton(OnPress onPress) {
        super(0, 0, 18, 18, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    void setState(boolean state) {
        this.state = state;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        int border = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF8B8B8B;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                0xFF2D3138);
        graphics.blit(TEXTURE, getX() + 1, getY() + 1, 0, 0, 16, 16, 16, 16);
        if (!state) {
            int color = 0xFFFF4747;
            for (int i = 3; i <= 13; i++) {
                graphics.fill(getX() + i, getY() + i, getX() + i + 2, getY() + i + 2, color);
                graphics.fill(getX() + 16 - i, getY() + i,
                        getX() + 18 - i, getY() + i + 2, color);
            }
        }
    }
}
