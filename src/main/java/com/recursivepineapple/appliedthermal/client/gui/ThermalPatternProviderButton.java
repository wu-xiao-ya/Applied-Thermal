package com.recursivepineapple.appliedthermal.client.gui;

import appeng.core.definitions.AEBlocks;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ThermalPatternProviderButton extends Button {

    private final BooleanSupplier available;

    public ThermalPatternProviderButton(int x, int y, BooleanSupplier available, OnPress onPress) {
        super(x, y, 22, 22, Component.translatable("screen.appliedthermal.open_pattern_provider"),
                onPress, DEFAULT_NARRATION);
        this.available = available;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        active = available.getAsBoolean();
        if (!active) {
            return;
        }

        int border = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF777777;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1,
                0xFF30343A);
        graphics.renderItem(new ItemStack(AEBlocks.PATTERN_PROVIDER.asItem()),
                getX() + 3, getY() + 3);
    }
}
