package com.recursivepineapple.appliedthermal.menu;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.PatternProviderMenu;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import net.minecraft.world.entity.player.Inventory;

public final class AppliedThermalPatternProviderMenu extends PatternProviderMenu {

    private static final String ACTION_TOGGLE_OUTPUT_RETURN = "toggleOutputReturn";

    private final AppliedThermalProviderAttachment attachment;

    @GuiSync(100)
    public boolean outputReturnEnabled;

    public AppliedThermalPatternProviderMenu(int id, Inventory inventory,
            PatternProviderLogicHost host) {
        super(ATMenus.PATTERN_PROVIDER, id, inventory, host);
        this.attachment = host instanceof AppliedThermalProviderAttachment provider
                ? provider
                : null;
        this.outputReturnEnabled = attachment == null || attachment.isOutputReturnEnabled();

        registerClientAction(ACTION_TOGGLE_OUTPUT_RETURN, () -> {
            if (attachment == null) {
                return;
            }
            attachment.setOutputReturnEnabled(!attachment.isOutputReturnEnabled());
            outputReturnEnabled = attachment.isOutputReturnEnabled();
        });
    }

    @Override
    public void broadcastChanges() {
        if (attachment != null) {
            outputReturnEnabled = attachment.isOutputReturnEnabled();
        }
        super.broadcastChanges();
    }

    public void toggleOutputReturn() {
        sendClientAction(ACTION_TOGGLE_OUTPUT_RETURN);
    }

    public boolean isFor(AppliedThermalProviderAttachment provider) {
        return attachment == provider;
    }
}
