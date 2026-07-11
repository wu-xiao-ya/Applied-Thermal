package com.recursivepineapple.appliedthermal.client.gui;

import ae2.client.gui.Icon;
import ae2.client.gui.widgets.IconButton;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public final class GuiOutputReturnButton extends IconButton {

    private static final ItemStack PISTON = new ItemStack(Blocks.PISTON);
    private static final int DISABLED_COLOR = 0xFFE33B32;

    private final BooleanSupplier state;
    private final Runnable onToggle;

    public GuiOutputReturnButton(BooleanSupplier state, Runnable onToggle) {
        super(null);
        this.state = state;
        this.onToggle = onToggle;
    }

    public boolean getState() {
        return state.getAsBoolean();
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        boolean releasedInside = enabled && visible
            && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        super.mouseReleased(mouseX, mouseY);
        if (releasedInside) {
            onToggle.run();
        }
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        super.drawButton(minecraft, mouseX, mouseY, partialTicks);
        if (!visible || getState()) {
            return;
        }
        int top = y + (hovered ? 1 : 0);
        for (int i = 2; i < 14; i++) {
            drawRect(x + i, top + i, x + i + 2, top + i + 2, DISABLED_COLOR);
            drawRect(x + 14 - i, top + i, x + 16 - i, top + i + 2, DISABLED_COLOR);
        }
    }

    @Override
    protected Icon getIcon() {
        return null;
    }

    @Override
    protected ItemStack getItemStackOverlay() {
        return PISTON;
    }

    @Override
    public List<ITextComponent> getTooltipMessage() {
        return Arrays.asList(
            new TextComponentTranslation("gui.appliedthermal.output_return"),
            new TextComponentTranslation(getState()
                ? "gui.appliedthermal.output_return.on"
                : "gui.appliedthermal.output_return.off"),
            new TextComponentTranslation("gui.appliedthermal.output_return.desc"));
    }
}
