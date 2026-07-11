package com.recursivepineapple.appliedthermal.client.gui;

import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import com.recursivepineapple.appliedthermal.menu.AppliedThermalPatternProviderMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AppliedThermalPatternProviderScreen
        extends PatternProviderScreen<AppliedThermalPatternProviderMenu> {

    private final OutputReturnButton outputReturnButton;

    public AppliedThermalPatternProviderScreen(AppliedThermalPatternProviderMenu menu,
            Inventory inventory, Component title, ScreenStyle style) {
        super(menu, inventory, title, style);
        outputReturnButton = new OutputReturnButton(button -> menu.toggleOutputReturn());
        widgets.add("output_return", outputReturnButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        outputReturnButton.setState(menu.outputReturnEnabled);
    }
}
