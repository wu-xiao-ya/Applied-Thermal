package com.recursivepineapple.appliedthermal.network;

import ae2.api.config.BlockingMode;
import ae2.api.config.LockCraftingMode;
import ae2.api.config.PatternProviderBlockingType;
import ae2.api.config.Settings;
import ae2.api.config.Setting;
import ae2.api.util.IConfigManager;
import ae2.api.config.YesNo;
import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.container.guisync.GuiSync;
import ae2.container.me.patternaccess.IPatternAccess;
import ae2.container.slot.AppEngSlot;
import ae2.container.slot.RestrictedInputSlot;
import ae2.helpers.patternprovider.PatternProviderReturnInventory;
import ae2.util.ConfigGuiInventory;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import com.recursivepineapple.appliedthermal.provider.ThermalMachinePatternProviderLogic;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Independent AE2S menu for Thermal's embedded provider. */
public final class ContainerThermalPatternProvider extends AEBaseContainer {

    private static final String ACTION_SETTING = "appliedthermal:set_setting";
    private static final String ACTION_RETURN = "appliedthermal:set_return";
    private static final String ACTION_PRIORITY = "appliedthermal:set_priority";

    private final AppliedThermalMachine host;
    private final AppliedThermalProviderAttachment attachment;
    private final ThermalMachinePatternProviderLogic logic;
    private final List<AppEngSlot> patternSlots = new ArrayList<>();
    private final boolean remote;

    @GuiSync(3) public BlockingMode blockingMode = BlockingMode.NO;
    @GuiSync(4) public PatternProviderBlockingType blockingType = PatternProviderBlockingType.NORMAL;
    @GuiSync(5) public LockCraftingMode lockMode = LockCraftingMode.NONE;
    @GuiSync(6) public LockCraftingMode lockReason = LockCraftingMode.NONE;
    @GuiSync(8) public YesNo terminalVisible = YesNo.YES;
    @GuiSync(9) public int activeSlots = 9;
    @GuiSync(11) public int priority;
    @GuiSync(12) public boolean returnEnabled;
    @GuiSync(13) public boolean remoteMenu;

    public ContainerThermalPatternProvider(InventoryPlayer inventory, AppliedThermalMachine host) {
        super(inventory, (TileEntity) host);
        this.host = host;
        this.attachment = host.appliedthermal$getProviderAttachment();
        this.logic = attachment.getLogic();
        this.remote = inventory.player.openContainer instanceof IPatternAccess;
        this.remoteMenu = remote;
        this.returnEnabled = true;
        registerClientAction(ACTION_SETTING, String[].class, this::setSetting);
        registerClientAction(ACTION_RETURN, Boolean.class, this::setReturnEnabled);
        registerClientAction(ACTION_PRIORITY, Integer.class, this::setPriority);

        for (int i = 0; i < logic.getPatternInv().size(); i++) {
            AppEngSlot slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.PROVIDER_PATTERN,
                logic.getPatternInv(), i);
            patternSlots.add(slot);
            addSlot(slot, SlotSemantics.ENCODED_PATTERN);
        }
        setupUpgrades(logic.getUpgrades());
        ConfigGuiInventory returns = logic.getReturnInv().createGuiWrapper();
        for (int i = 0; i < PatternProviderReturnInventory.NUMBER_OF_SLOTS && i < returns.size(); i++) {
            addSlot(new AppEngSlot(returns, i), SlotSemantics.STORAGE);
        }
        addPlayerInventorySlots(8, 163);
        refreshSlots();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player == getPlayer() && isCurrentMachine() && super.canInteractWith(player)
            && attachment.getTileEntity().canPlayerAccess(player);
    }

    @Override
    protected boolean canInteractiveDistance(EntityPlayer player, TileEntity tile) {
        return remote ? tile.hasWorld() && tile.getWorld().isBlockLoaded(tile.getPos())
            : super.canInteractiveDistance(player, tile);
    }

    private boolean isCurrentMachine() {
        TileMachineBase tile = attachment.getTileEntity();
        World world = tile.getWorld();
        BlockPos pos = tile.getPos();
        return attachment.isEnabled() && attachment.getLogic() == logic && host.appliedthermal$hasPatternProviderAugment()
            && world != null && !tile.isInvalid() && world.isBlockLoaded(pos) && world.getTileEntity(pos) == tile;
    }

    @Override
    public void onClientDataSync(ShortSet updated) {
        super.onClientDataSync(updated);
        setExternalGuiReturn(remoteMenu);
        refreshSlots();
    }

    @Override
    public void broadcastChanges() {
        if (!isClientSide()) {
            blockingMode = logic.getConfigManager().getSetting(Settings.BLOCKING_MODE);
            blockingType = logic.getConfigManager().getSetting(Settings.PATTERN_PROVIDER_BLOCKING_TYPE);
            lockMode = logic.getConfigManager().getSetting(Settings.LOCK_CRAFTING_MODE);
            lockReason = logic.getCraftingLockedReason();
            terminalVisible = logic.getConfigManager().getSetting(Settings.PATTERN_ACCESS_TERMINAL);
            activeSlots = logic.getActivePatternSlots();
            priority = logic.getPriority();
            returnEnabled = attachment.shouldReturnOutputsToNetwork();
            refreshSlots();
        }
        super.broadcastChanges();
    }

    @Override
    public boolean isValidForSlot(Slot slot, ItemStack stack) {
        return !patternSlots.contains(slot) || slot.getSlotIndex() < activeSlots
            && logic.getPatternInv().isItemValid(slot.getSlotIndex(), stack);
    }

    private void setSetting(String[] args) {
        if (!authorizedAction() || args == null || args.length != 2 || args[1] == null) {
            return;
        }
        IConfigManager manager = logic.getConfigManager();
        for (Setting<?> setting : manager.getSettings()) {
            if (setting.getName().equals(args[0])
                && setting.getValues().stream().anyMatch(value -> value.name().equals(args[1]))) {
                setting.setFromString(manager, args[1]);
                return;
            }
        }
    }

    private void setReturnEnabled(Boolean value) {
        if (authorizedAction() && value != null) {
            attachment.setReturnOutputsToNetwork(value);
        }
    }

    private void setPriority(Integer value) {
        if (authorizedAction() && value != null) {
            logic.setPriority(value);
        }
    }

    private boolean authorizedAction() {
        return isServerSide() && getPlayer().openContainer == this && canInteractWith(getPlayer());
    }

    private void refreshSlots() {
        for (AppEngSlot slot : patternSlots) {
            boolean visible = slot.getSlotIndex() < activeSlots;
            slot.setSlotEnabled(visible);
            slot.setActive(visible);
        }
    }

    public AppliedThermalMachine getHost() {
        return host;
    }

    public void requestSetting(String key, String value) {
        sendClientAction(ACTION_SETTING, new String[] {key, value});
    }

    public void requestPriority(int value) {
        sendClientAction(ACTION_PRIORITY, value);
    }

    public void requestReturnToggle() {
        sendClientAction(ACTION_RETURN, !returnEnabled);
    }
}
