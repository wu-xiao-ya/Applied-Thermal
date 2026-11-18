package com.recursivepineapple.appliedthermal.client.gui;

import ae2.client.gui.Icon;
import ae2.client.gui.widgets.IconButton;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public final class GuiOutputReturnButton extends IconButton {

    private static final Icon RETURN_ON = Icon.register(new ResourceLocation(AppliedThermal.MOD_ID, "output_return_on"),
        16, 16);
    private static final Icon RETURN_OFF = Icon.register(new ResourceLocation(AppliedThermal.MOD_ID, "output_return_off"),
        16, 16);

    private final BooleanSupplier state;

    public GuiOutputReturnButton(BooleanSupplier state, Runnable onToggle) {
        super(onToggle);
        this.state = state;
        setMessage(new TextComponentTranslation("gui.appliedthermal.output_return"));
    }

    @Override
    protected Icon getIcon() {
        return state.getAsBoolean() ? RETURN_ON : RETURN_OFF;
    }

    @Override
    public List<ITextComponent> getTooltipMessage() {
        boolean enabled = state.getAsBoolean();
        return Arrays.asList(
            new TextComponentTranslation(enabled
                ? "gui.appliedthermal.output_return.on"
                : "gui.appliedthermal.output_return.off"),
            new TextComponentTranslation("gui.appliedthermal.output_return.desc"),
            new TextComponentTranslation(enabled
                ? "gui.appliedthermal.output_return.disable"
                : "gui.appliedthermal.output_return.enable"));
    }
}
