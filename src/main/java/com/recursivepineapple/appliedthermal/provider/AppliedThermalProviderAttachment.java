package com.recursivepineapple.appliedthermal.provider;

import ae2.api.config.Actionable;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.networking.GridFlags;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IInWorldGridNodeHost;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.networking.security.IActionHost;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.storage.IStorageService;
import ae2.api.orientation.BlockOrientation;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.api.storage.StorageHelper;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.util.AECableType;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.util.Platform;
import cofh.core.block.TileReconfigurable;
import cofh.core.block.TilePowered;
import cofh.core.util.core.SlotConfig;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermalexpansion.block.machine.BlockMachine;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.appflux.AppFluxCompat;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.init.ATItems;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

public final class AppliedThermalProviderAttachment implements PatternProviderLogicHost, IActionHost,
    IInWorldGridNodeHost {

    private static final String TAG_ROOT = "AppliedThermalProvider";
    private static final String TAG_RETURN_OUTPUTS = "ReturnOutputsToNetwork";
    @Nullable
    private static Field slotConfigField;
    private static boolean slotConfigFieldResolved;

    private final TileMachineBase tile;
    private IManagedGridNode mainNode;
    private PatternProviderLogic logic;
    private boolean loaded;
    private boolean enabled;
    @Nullable
    private NBTTagCompound savedProviderState;
    @Nullable
    private String customName;
    private boolean returnOutputsToNetwork = true;

    public AppliedThermalProviderAttachment(TileMachineBase tile) {
        this.tile = tile;
        rebuildAeState();
    }

    private IManagedGridNode createManagedNode() {
        return GridHelper.createManagedNode(this.tile, TileListener.INSTANCE)
            .setFlags(GridFlags.REQUIRE_CHANNEL)
            .setInWorldNode(true)
            .setExposedOnSides(EnumSet.allOf(EnumFacing.class))
            .setVisualRepresentation(new ItemStack(ATItems.getPatternProviderAugment()))
            .setTagName("appliedthermal");
    }

    private PatternProviderLogic createLogic(IManagedGridNode node) {
        PatternProviderLogicHost host = tile instanceof PatternProviderLogicHost ? (PatternProviderLogicHost) tile : this;
        return new ThermalMachinePatternProviderLogic(node, host, ATItems.getPatternProviderAugment(), 36, this);
    }

    private void rebuildAeState() {
        this.mainNode = createManagedNode();
        this.logic = createLogic(this.mainNode);
        applySavedProviderState();
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            createNodeIfReady();
            refreshProviderState();
        } else {
            captureProviderState();
            rebuildAfterDestroy();
        }
        saveChanges();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void ejectContentsOnAugmentRemoval() {
        if (!this.enabled || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>();
        this.logic.addDrops(drops);
        for (ItemStack upgrade : this.logic.getUpgrades()) {
            if (!upgrade.isEmpty()) {
                drops.add(upgrade.copy());
            }
        }

        this.logic.clearContent();
        captureProviderState();
        saveChanges();
        Platform.spawnDrops(tile.getWorld(), tile.getPos(), drops);
    }

    public void onReady() {
        this.loaded = true;
        createNodeIfReady();
        if (enabled) {
            refreshProviderState();
        }
    }

    public void invalidate() {
        captureProviderState();
        this.loaded = false;
        rebuildAfterDestroy();
    }

    public void readFromNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey(TAG_ROOT)) {
            return;
        }
        NBTTagCompound tag = nbt.getCompoundTag(TAG_ROOT);
        this.savedProviderState = tag.copy();
        applySavedProviderState();
        if (tag.hasKey("CustomName")) {
            this.customName = tag.getString("CustomName");
        }
        this.returnOutputsToNetwork = !tag.hasKey(TAG_RETURN_OUTPUTS) || tag.getBoolean(TAG_RETURN_OUTPUTS);
    }

    public void writeToNBT(NBTTagCompound nbt) {
        captureProviderState();
        if (this.savedProviderState != null) {
            nbt.setTag(TAG_ROOT, this.savedProviderState.copy());
        }
    }

    private void createNodeIfReady() {
        if (enabled && loaded && this.mainNode.getNode() == null && tile.hasWorld() && !tile.getWorld().isRemote) {
            try {
                this.mainNode.create(tile.getWorld(), tile.getPos());
            } catch (IllegalStateException e) {
                captureProviderState();
                rebuildAfterDestroy();
                this.mainNode.create(tile.getWorld(), tile.getPos());
            }
        }
        if (enabled && this.mainNode.getNode() != null) {
            this.mainNode.setVisualRepresentation(getMainContainerIcon());
        }
    }

    private void rebuildAfterDestroy() {
        this.mainNode.destroy();
        rebuildAeState();
    }

    private void captureProviderState() {
        NBTTagCompound tag = new NBTTagCompound();
        this.logic.writeToNBT(tag);
        this.mainNode.saveToNBT(tag);
        if (this.customName != null && !this.customName.isEmpty()) {
            tag.setString("CustomName", this.customName);
        }
        tag.setBoolean(TAG_RETURN_OUTPUTS, this.returnOutputsToNetwork);
        this.savedProviderState = tag;
    }

    private void applySavedProviderState() {
        if (this.savedProviderState == null) {
            return;
        }
        NBTTagCompound tag = this.savedProviderState.copy();
        this.logic.readFromNBT(tag);
        this.mainNode.loadFromNBT(tag);
        if (tag.hasKey("CustomName")) {
            this.customName = tag.getString("CustomName");
        }
        this.returnOutputsToNetwork = !tag.hasKey(TAG_RETURN_OUTPUTS) || tag.getBoolean(TAG_RETURN_OUTPUTS);
        if (enabled && loaded) {
            refreshProviderState();
        }
    }

    @Override
    public PatternProviderLogic getLogic() {
        return logic;
    }

    @Override
    public TileEntity getTileEntity() {
        return tile;
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        return EnumSet.allOf(EnumFacing.class);
    }

    @Override
    public boolean hasCustomName() {
        return customName != null && !customName.isEmpty();
    }

    @Override
    @Nullable
    public String getCustomName() {
        return customName;
    }

    @Override
    public void setCustomName(@Nullable String name) {
        this.customName = name;
        saveChanges();
    }

    @Override
    public void saveChanges() {
        tile.markDirty();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        ItemStack stack = tile.getBlockType() instanceof BlockMachine
            ? new ItemStack(tile.getBlockType(), 1, tile.getBlockMetadata())
            : new ItemStack(ATItems.getPatternProviderAugment());
        return AEItemKey.of(stack);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        if (tile.getBlockType() instanceof BlockMachine) {
            return new ItemStack(tile.getBlockType(), 1, tile.getBlockMetadata());
        }
        return new ItemStack(ATItems.getPatternProviderAugment());
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        AEItemKey icon = getTerminalIcon();
        ITextComponent name = tile.getDisplayName();
        return new PatternContainerGroup(icon, name, java.util.Collections.emptyList());
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.logic.getUpgrades();
    }

    @Override
    @Nullable
    public IGrid getGrid() {
        return logic.getGrid();
    }

    @Override
    @Nullable
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    @Override
    @Nullable
    public IGridNode getGridNode(EnumFacing dir) {
        return enabled ? mainNode.getNode() : null;
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return enabled ? AECableType.SMART : AECableType.NONE;
    }

    public long insertInput(AEKey what, long amount, Actionable action) {
        if (!(what instanceof AEItemKey) || amount <= 0 || amount > Integer.MAX_VALUE) {
            return 0;
        }
        AEItemKey itemKey = (AEItemKey) what;
        ItemStack stack = itemKey.toStack((int) amount);
        ItemStack simulated = stack.copy();
        int inserted = 0;
        for (int slot = 0; slot < tile.getSizeInventory() && inserted < amount; slot++) {
            if (!isInputSlot(slot) || !tile.isItemValidForSlot(slot, simulated)) {
                continue;
            }
            ItemStack existing = tile.getStackInSlot(slot);
            if (existing.isEmpty()) {
                int move = Math.min(simulated.getCount(), simulated.getMaxStackSize());
                if (action == Actionable.MODULATE) {
                    ItemStack copy = simulated.copy();
                    copy.setCount(move);
                    tile.setInventorySlotContents(slot, copy);
                }
                inserted += move;
                simulated.shrink(move);
            } else if (ItemHelper.itemsIdentical(existing, simulated)
                && existing.getCount() < Math.min(existing.getMaxStackSize(), tile.getInventoryStackLimit())) {
                int move = Math.min(simulated.getCount(),
                    Math.min(existing.getMaxStackSize(), tile.getInventoryStackLimit()) - existing.getCount());
                if (action == Actionable.MODULATE) {
                    existing.grow(move);
                    tile.setInventorySlotContents(slot, existing);
                }
                inserted += move;
                simulated.shrink(move);
            }
        }
        if (inserted > 0 && action == Actionable.MODULATE) {
            tile.markDirty();
        }
        return inserted;
    }

    public boolean shouldReturnOutputsToNetwork() {
        return returnOutputsToNetwork;
    }

    public void setReturnOutputsToNetwork(boolean returnOutputsToNetwork) {
        if (this.returnOutputsToNetwork == returnOutputsToNetwork) {
            return;
        }
        this.returnOutputsToNetwork = returnOutputsToNetwork;
        saveChanges();
    }

    public void tryReturnOutputsToNetwork() {
        if (!enabled || !returnOutputsToNetwork || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        IGrid grid = getGrid();
        IGridNode node = getActionableNode();
        if (grid == null || node == null || !node.isActive()) {
            return;
        }
        IStorageService storageService = grid.getStorageService();
        if (storageService == null) {
            return;
        }
        MEStorage storage = storageService.getInventory();
        IActionSource source = IActionSource.ofMachine(this);
        for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
            if (!isOutputSlot(slot)) {
                continue;
            }
            ItemStack stack = tile.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            AEItemKey key = AEItemKey.of(stack);
            long requested = stack.getCount();
            long accepted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, requested, source,
                Actionable.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            long inserted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, accepted, source,
                Actionable.MODULATE);
            if (inserted <= 0) {
                continue;
            }
            ItemStack remaining = stack.copy();
            remaining.shrink((int) Math.min(inserted, Integer.MAX_VALUE));
            tile.setInventorySlotContents(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
            tile.markDirty();
        }
    }

    public void tryChargeMachineFromFluxNetwork() {
        if (!enabled || tile.getWorld() == null || tile.getWorld().isRemote || !(tile instanceof TilePowered)
            || !(tile instanceof AppliedThermalMachine)) {
            return;
        }
        AppliedThermalMachine machine = (AppliedThermalMachine) tile;
        if (!machine.appliedthermal$getFluxAttachment().isInductionCardInstalled()) {
            return;
        }
        TilePowered powered = (TilePowered) tile;
        int energySpace = powered.getMaxEnergyStored(null) - powered.getEnergyStored(null);
        if (energySpace <= 0) {
            return;
        }
        long extracted = AppFluxCompat.bridge().extractEnergyFromGrid(machine, energySpace, false);
        if (extracted <= 0) {
            return;
        }
        powered.receiveEnergy(null, clampToInt(extracted), false);
        tile.markDirty();
    }

    private boolean isOutputSlot(int slot) {
        SlotConfig slotConfig = getSlotConfig(tile);
        return slotConfig != null
            && slotConfig.allowExtractionSlot != null
            && slot >= 0
            && slot < slotConfig.allowExtractionSlot.length
            && slotConfig.allowExtractionSlot[slot];
    }

    private boolean isInputSlot(int slot) {
        SlotConfig slotConfig = getSlotConfig(tile);
        return slotConfig != null
            && slotConfig.allowInsertionSlot != null
            && slot >= 0
            && slot < slotConfig.allowInsertionSlot.length
            && slotConfig.allowInsertionSlot[slot];
    }

    @Nullable
    private static SlotConfig getSlotConfig(TileMachineBase tile) {
        try {
            Field field = getSlotConfigField();
            return field == null ? null : (SlotConfig) field.get(tile);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    @Nullable
    private static Field getSlotConfigField() {
        if (!slotConfigFieldResolved) {
            slotConfigFieldResolved = true;
            try {
                slotConfigField = TileReconfigurable.class.getDeclaredField("slotConfig");
                slotConfigField.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                slotConfigField = null;
            }
        }
        return slotConfigField;
    }

    public boolean canAcceptInputs(KeyCounter[] inputs) {
        for (KeyCounter counter : inputs) {
            for (Object2LongMap.Entry<AEKey> input : counter) {
                if (insertInput(input.getKey(), input.getLongValue(), Actionable.SIMULATE) < input.getLongValue()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean acceptsAny(KeyCounter[] inputs) {
        for (KeyCounter counter : inputs) {
            for (Object2LongMap.Entry<AEKey> ignored : counter) {
                if (ignored.getLongValue() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsAnyInput() {
        for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
            if (isInputSlot(slot) && !tile.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean containsInput(AEKey key) {
        if (!(key instanceof AEItemKey)) {
            return false;
        }
        AEItemKey itemKey = (AEItemKey) key;
        for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
            if (isInputSlot(slot) && itemKey.matches(tile.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    private static int clampToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int) value);
    }

    private void refreshProviderState() {
        this.logic.updatePatterns();
        ICraftingProvider.requestUpdate(this.mainNode);
    }

    private static AppliedThermalProviderAttachment getAttachment(TileMachineBase tile) {
        return ((AppliedThermalMachine) tile).appliedthermal$getProviderAttachment();
    }

    private enum TileListener implements IGridNodeListener<TileMachineBase> {
        INSTANCE;

        @Override
        public void onSaveChanges(TileMachineBase nodeOwner, IGridNode node) {
            getAttachment(nodeOwner).saveChanges();
        }

        @Override
        public void onStateChanged(TileMachineBase nodeOwner, IGridNode node, State reason) {
            getAttachment(nodeOwner).logic.onMainNodeStateChanged();
        }
    }
}
