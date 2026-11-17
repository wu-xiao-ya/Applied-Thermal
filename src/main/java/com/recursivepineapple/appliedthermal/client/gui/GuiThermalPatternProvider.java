package com.recursivepineapple.appliedthermal.client.gui;

import ae2.api.config.BlockingMode;
import ae2.api.config.LockCraftingMode;
import ae2.api.config.PatternProviderBlockingType;
import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.Icon;
import ae2.client.gui.NumberEntryType;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.NumberEntryWidget;
import ae2.client.gui.widgets.SettingToggleButton;
import ae2.client.gui.widgets.ToggleButton;
import ae2.client.gui.widgets.UpgradesPanel;
import ae2.container.SlotSemantics;
import ae2.core.localization.GuiText;
import com.recursivepineapple.appliedthermal.network.ContainerThermalPatternProvider;
import java.util.function.Predicate;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiThermalPatternProvider extends AEBaseGui<ContainerThermalPatternProvider> {

    private final SettingToggleButton<BlockingMode> blockingButton;
    private final SettingToggleButton<PatternProviderBlockingType> blockingTypeButton;
    private final SettingToggleButton<LockCraftingMode> lockButton;
    private final ToggleButton terminalButton;
    private final NumberEntryWidget priorityField;
    private int lastPriority;

    public GuiThermalPatternProvider(ContainerThermalPatternProvider menu, InventoryPlayer inventory) {
        super(menu, inventory, GuiStyleManager.loadStyleDoc("/screens/appliedthermal_pattern_provider.json"));
        blockingButton = addSettingButton(Settings.BLOCKING_MODE, BlockingMode.NO, mode -> true);
        blockingTypeButton = addSettingButton(Settings.PATTERN_PROVIDER_BLOCKING_TYPE,
            PatternProviderBlockingType.NORMAL, mode -> true);
        lockButton = addSettingButton(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE,
            mode -> mode == LockCraftingMode.NONE || mode == LockCraftingMode.LOCK_UNTIL_RESULT);
        terminalButton = addToLeftToolbar(new ToggleButton(Icon.PATTERN_ACCESS_SHOW, Icon.PATTERN_ACCESS_HIDE,
            GuiText.PatternAccessTerminal.text(), GuiText.PatternAccessTerminalHint.text(),
            value -> menu.requestSetting(Settings.PATTERN_ACCESS_TERMINAL.getName(),
                value ? YesNo.YES.name() : YesNo.NO.name())));
        addToLeftToolbar(new GuiOutputReturnButton(() -> menu.returnEnabled, menu::requestReturnToggle));
        widgets.add("upgrades", UpgradesPanel.create(widgets, menu.getSlots(SlotSemantics.UPGRADE),
            (IUpgradeableObject) menu.getHost()));
        priorityField = widgets.addNumberEntryWidget("priority", NumberEntryType.UNITLESS);
        priorityField.setTextFieldStyle(getStyle().getWidget("priorityInput"));
        priorityField.setShowIncrementButtons(false);
        priorityField.setMinValue(Integer.MIN_VALUE);
        priorityField.setMaxValue(Integer.MAX_VALUE);
        priorityField.setLongValue(menu.priority);
        lastPriority = menu.priority;
        priorityField.setOnChange(() -> priorityField.getIntValue().ifPresent(menu::requestPriority));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        blockingButton.set(container.blockingMode);
        blockingTypeButton.set(container.blockingType);
        lockButton.set(container.lockMode);
        terminalButton.setState(container.terminalVisible == YesNo.YES);
        if (!priorityField.isFocused() && lastPriority != container.priority) {
            lastPriority = container.priority;
            priorityField.setLongValue(lastPriority);
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        fontRenderer.drawString(GuiText.Priority.text().getFormattedText(), 8, 148, 0x404040);
        if (container.lockReason == LockCraftingMode.LOCK_UNTIL_RESULT) {
            fontRenderer.drawString(GuiText.CraftingLockIsLocked.text().getFormattedText(), 8, 17, 0x404040);
        }
    }

    private <T extends Enum<T>> SettingToggleButton<T> addSettingButton(Setting<T> setting, T initial,
                                                                       Predicate<T> allowed) {
        return addToLeftToolbar(new SettingToggleButton<>(setting, initial, allowed,
            (button, backwards) -> sendSetting(button, button.getNextValue(backwards)), this::sendSetting));
    }

    private <T extends Enum<T>> void sendSetting(SettingToggleButton<T> button, T next) {
        button.set(next);
        container.requestSetting(button.getSetting().getName(), next.name());
    }
}
