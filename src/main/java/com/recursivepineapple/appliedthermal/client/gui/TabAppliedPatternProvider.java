package com.recursivepineapple.appliedthermal.client.gui;

import cofh.core.gui.GuiContainerCore;
import cofh.core.gui.element.tab.TabBase;
import cofh.core.util.helpers.StringHelper;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.network.ATNetwork;
import com.recursivepineapple.appliedthermal.network.MessageOpenPatternProviderGui;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;

public final class TabAppliedPatternProvider extends TabBase {

    private static final String PATTERN_PROVIDER_SPRITE = "appliedenergistics2:blocks/interface";

    private final AppliedThermalProviderAttachment attachment;
    private final TileEntity tile;
    private int openRequestCooldown;

    public TabAppliedPatternProvider(GuiContainerCore gui, AppliedThermalMachine machine) {
        super(gui, 1);
        this.attachment = machine.appliedthermal$getProviderAttachment();
        this.tile = this.attachment.getTile();
        this.maxWidth = this.minWidth;
        this.maxHeight = this.minHeight;
        this.backgroundColor = 0x51456B;
    }

    @Override
    public void drawForeground() {
        drawTabIcon(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(PATTERN_PROVIDER_SPRITE));
    }

    @Override
    public void addTooltip(List<String> tooltip) {
        tooltip.add(StringHelper.localize("tab.appliedthermal.pattern_provider"));
        tooltip.add(StringHelper.GRAY + StringHelper.localize("tab.appliedthermal.open"));
    }

    @Override
    public void update() {
        super.update();
        if (openRequestCooldown > 0) {
            openRequestCooldown--;
        }
    }

    @Override
    public boolean onMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && openRequestCooldown == 0 && tile.hasWorld()) {
            openRequestCooldown = 1;
            GuiContainerCore.playClickSound(0.8F);
            ATNetwork.CHANNEL.sendToServer(new MessageOpenPatternProviderGui(tile.getPos()));
        }
        return true;
    }
}
