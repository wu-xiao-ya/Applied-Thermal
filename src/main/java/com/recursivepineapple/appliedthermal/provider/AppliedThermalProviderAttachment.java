package com.recursivepineapple.appliedthermal.provider;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cofh.core.block.TilePowered;
import cofh.core.util.core.SlotConfig;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermalexpansion.block.machine.BlockMachine;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.init.ATItems;
import com.recursivepineapple.appliedthermal.integration.fluxapplied.FluxAppliedCompat;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.mixin.AccessorTileMachineBase;
import com.recursivepineapple.appliedthermal.mixin.AccessorTileReconfigurable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.ItemStackHandler;

public final class AppliedThermalProviderAttachment {

    private static final String TAG_ROOT = "AppliedThermalProviderUEL";
    private static final String TAG_RETURN_OUTPUTS = "ReturnOutputsToNetwork";
    private static final String TAG_INTERFACE = "Interface";
    private static final String TAG_FLUX_CARD = "FluxCard";
    private static final String TAG_PATTERN_IN_FLIGHT = "PatternInFlight";
    private static final String TAG_LAST_PATTERN_PUSH = "LastPatternPush";
    private static final long BUSY_RELEASE_DELAY = 2L;

    private final TileMachineBase tile;
    private final ItemStackHandler fluxCard = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() == ATItems.getFluxInductionCard();
        }

        @Override
        protected void onContentsChanged(int slot) {
            saveChanges();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private AENetworkProxy proxy;
    private DualityInterface duality;
    private boolean loaded;
    private boolean enabled;
    private boolean returnOutputsToNetwork = true;
    private boolean patternInFlight;
    private long lastPatternPushTick = Long.MIN_VALUE;
    @Nullable
    private NBTTagCompound savedState;
    @Nullable
    private String customName;

    public AppliedThermalProviderAttachment(TileMachineBase tile) {
        this.tile = tile;
        rebuildAeState();
    }

    private void rebuildAeState() {
        ItemStack upgradeIdentity = new ItemStack(ATItems.getPatternProviderAugment());
        this.proxy = new AENetworkProxy((IGridProxyable) tile, "proxy", upgradeIdentity, true);
        this.proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        this.proxy.setValidSides(EnumSet.allOf(EnumFacing.class));
        this.duality = new DualityInterface(this.proxy, (IInterfaceHost) tile);
        this.proxy.setVisualRepresentation(getMachineRepresentation());
        applySavedState();
    }

    public TileMachineBase getTile() {
        return tile;
    }

    public AENetworkProxy getProxy() {
        return proxy;
    }

    public DualityInterface getDuality() {
        return duality;
    }

    public ItemStackHandler getFluxCardInventory() {
        return fluxCard;
    }

    public boolean isFluxCardInstalled() {
        ItemStack stack = fluxCard.getStackInSlot(0);
        return !stack.isEmpty() && stack.getItem() == ATItems.getFluxInductionCard();
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            if (enabled && loaded) {
                ensureReady();
            }
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            ensureReady();
        } else {
            captureState();
            proxy.invalidate();
        }
        saveChanges();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void onReady() {
        this.loaded = true;
        ensureReady();
    }

    public void onChunkUnload() {
        captureState();
        this.loaded = false;
        proxy.onChunkUnload();
    }

    public void invalidate() {
        captureState();
        this.loaded = false;
        proxy.invalidate();
    }

    private void ensureReady() {
        if (!enabled || !loaded || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        proxy.setVisualRepresentation(getMachineRepresentation());
        if (proxy.isReady()) {
            return;
        }
        proxy.onReady();
        duality.initialize();
    }

    public void readFromNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey(TAG_ROOT, 10)) {
            return;
        }
        this.savedState = nbt.getCompoundTag(TAG_ROOT).copy();
        applySavedState();
    }

    public void writeToNBT(NBTTagCompound nbt) {
        captureState();
        if (savedState != null) {
            nbt.setTag(TAG_ROOT, savedState.copy());
        }
    }

    private void captureState() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound interfaceTag = new NBTTagCompound();
        duality.writeToNBT(interfaceTag);
        proxy.writeToNBT(interfaceTag);
        tag.setTag(TAG_INTERFACE, interfaceTag);
        tag.setTag(TAG_FLUX_CARD, fluxCard.serializeNBT());
        tag.setBoolean(TAG_RETURN_OUTPUTS, returnOutputsToNetwork);
        tag.setBoolean(TAG_PATTERN_IN_FLIGHT, patternInFlight);
        if (lastPatternPushTick != Long.MIN_VALUE) {
            tag.setLong(TAG_LAST_PATTERN_PUSH, lastPatternPushTick);
        }
        if (customName != null && !customName.isEmpty()) {
            tag.setString("CustomName", customName);
        }
        this.savedState = tag;
    }

    private void applySavedState() {
        if (savedState == null) {
            return;
        }
        NBTTagCompound tag = savedState.copy();
        if (tag.hasKey(TAG_INTERFACE, 10)) {
            NBTTagCompound interfaceTag = tag.getCompoundTag(TAG_INTERFACE);
            duality.readFromNBT(interfaceTag);
            proxy.readFromNBT(interfaceTag);
        }
        if (tag.hasKey(TAG_FLUX_CARD, 10)) {
            fluxCard.deserializeNBT(tag.getCompoundTag(TAG_FLUX_CARD));
        }
        returnOutputsToNetwork = !tag.hasKey(TAG_RETURN_OUTPUTS) || tag.getBoolean(TAG_RETURN_OUTPUTS);
        patternInFlight = tag.getBoolean(TAG_PATTERN_IN_FLIGHT);
        lastPatternPushTick = tag.hasKey(TAG_LAST_PATTERN_PUSH)
            ? tag.getLong(TAG_LAST_PATTERN_PUSH)
            : Long.MIN_VALUE;
        customName = tag.hasKey("CustomName") ? tag.getString("CustomName") : null;
    }

    public void ejectContentsOnAugmentRemoval() {
        if (!enabled || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>();
        duality.addDrops(drops);
        ItemStack card = fluxCard.getStackInSlot(0);
        if (!card.isEmpty()) {
            drops.add(card.copy());
        }
        Platform.spawnDrops(tile.getWorld(), tile.getPos(), drops);

        proxy.invalidate();
        savedState = null;
        customName = null;
        returnOutputsToNetwork = true;
        patternInFlight = false;
        lastPatternPushTick = Long.MIN_VALUE;
        fluxCard.setStackInSlot(0, ItemStack.EMPTY);
        rebuildAeState();
        saveChanges();
    }

    public boolean tryPushPattern(InventoryCrafting table) {
        if (!enabled || !proxy.isActive()) {
            return false;
        }
        if (isBlockingEnabled() && isPatternTargetBusy()) {
            return false;
        }

        List<ItemStack> snapshot = snapshotInventory();
        boolean hasInput = false;
        for (int i = 0; i < table.getSizeInventory(); i++) {
            ItemStack input = table.getStackInSlot(i);
            if (input.isEmpty()) {
                continue;
            }
            hasInput = true;
            if (!insertIntoSnapshot(snapshot, input.copy())) {
                return false;
            }
        }
        if (!hasInput) {
            return false;
        }

        commitSnapshot(snapshot);
        if (isBlockingEnabled()) {
            patternInFlight = true;
            lastPatternPushTick = tile.getWorld() == null ? 0L : tile.getWorld().getTotalWorldTime();
        }
        saveChanges();
        return true;
    }

    private List<ItemStack> snapshotInventory() {
        List<ItemStack> snapshot = new ArrayList<>(tile.getSizeInventory());
        for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
            snapshot.add(tile.getStackInSlot(slot).copy());
        }
        return snapshot;
    }

    private boolean insertIntoSnapshot(List<ItemStack> snapshot, ItemStack input) {
        for (int slot = 0; slot < snapshot.size() && !input.isEmpty(); slot++) {
            if (!isInputSlot(slot) || !tile.isItemValidForSlot(slot, input)) {
                continue;
            }
            ItemStack existing = snapshot.get(slot);
            int limit = Math.min(tile.getInventoryStackLimit(), input.getMaxStackSize());
            if (existing.isEmpty()) {
                int moved = Math.min(limit, input.getCount());
                ItemStack placed = input.copy();
                placed.setCount(moved);
                snapshot.set(slot, placed);
                input.shrink(moved);
            } else if (ItemHelper.itemsIdentical(existing, input)) {
                int slotLimit = Math.min(limit, existing.getMaxStackSize());
                int moved = Math.min(slotLimit - existing.getCount(), input.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    input.shrink(moved);
                }
            }
        }
        return input.isEmpty();
    }

    private void commitSnapshot(List<ItemStack> snapshot) {
        for (int slot = 0; slot < snapshot.size(); slot++) {
            if (!isInputSlot(slot)) {
                continue;
            }
            ItemStack before = tile.getStackInSlot(slot);
            ItemStack after = snapshot.get(slot);
            if (!ItemStack.areItemStacksEqual(before, after)) {
                tile.setInventorySlotContents(slot, after);
            }
        }
        tile.markDirty();
    }

    public boolean isPatternTargetBusy() {
        if (!isBlockingEnabled()) {
            return false;
        }
        if (patternInFlight) {
            return true;
        }
        return hasMachineContentsOrProcess();
    }

    private boolean hasMachineContentsOrProcess() {
        if (((AccessorTileMachineBase) tile).appliedthermal$getProcessRem() > 0) {
            return true;
        }
        for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
            if ((isInputSlot(slot) || isOutputSlot(slot)) && !tile.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void refreshInFlightState() {
        if (!patternInFlight || tile.getWorld() == null) {
            return;
        }
        if (lastPatternPushTick == Long.MIN_VALUE) {
            lastPatternPushTick = tile.getWorld().getTotalWorldTime() - BUSY_RELEASE_DELAY;
        }
        long age = tile.getWorld().getTotalWorldTime() - lastPatternPushTick;
        if (age >= BUSY_RELEASE_DELAY && !hasMachineContentsOrProcess()) {
            patternInFlight = false;
            saveChanges();
        }
    }

    public void tickServer() {
        if (!enabled || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        tryReturnOutputsToNetwork();
        tryChargeMachineFromFluxNetwork();
        refreshInFlightState();
    }

    public void tryReturnOutputsToNetwork() {
        if (!returnOutputsToNetwork || !proxy.isActive()) {
            return;
        }
        try {
            IStorageGrid storage = proxy.getStorage();
            IItemStorageChannel channel =
                AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEInventory<IAEItemStack> inventory = storage.getInventory(channel);
            MachineSource source = new MachineSource((IActionHost) tile);

            for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
                if (!isOutputSlot(slot)) {
                    continue;
                }
                ItemStack stack = tile.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                IAEItemStack simulatedInput = AEItemStack.fromItemStack(stack);
                IAEItemStack simulatedRemainder =
                    Platform.poweredInsert(proxy.getEnergy(), inventory, simulatedInput, source, Actionable.SIMULATE);
                long accepted = stack.getCount() - stackSize(simulatedRemainder);
                if (accepted <= 0) {
                    continue;
                }

                ItemStack offered = stack.copy();
                offered.setCount((int) Math.min(accepted, Integer.MAX_VALUE));
                IAEItemStack actualRemainder = Platform.poweredInsert(
                    proxy.getEnergy(), inventory, AEItemStack.fromItemStack(offered), source, Actionable.MODULATE);
                int inserted = offered.getCount() - (int) stackSize(actualRemainder);
                if (inserted <= 0) {
                    continue;
                }

                ItemStack remaining = stack.copy();
                remaining.shrink(inserted);
                tile.setInventorySlotContents(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                ItemStack returned = stack.copy();
                returned.setCount(inserted);
                duality.onStackReturnedToNetwork(AEItemStack.fromItemStack(returned));
                tile.markDirty();
            }
        } catch (GridAccessException ignored) {
        }
    }

    private static long stackSize(@Nullable IAEItemStack stack) {
        return stack == null ? 0L : stack.getStackSize();
    }

    private void tryChargeMachineFromFluxNetwork() {
        if (!isFluxCardInstalled() || !FluxAppliedCompat.bridge().isAvailable() || !(tile instanceof TilePowered)) {
            return;
        }
        TilePowered powered = (TilePowered) tile;
        int requested = powered.getMaxEnergyStored(null) - powered.getEnergyStored(null);
        if (requested <= 0) {
            return;
        }
        int accepted = powered.receiveEnergy(null, requested, true);
        if (accepted <= 0) {
            return;
        }
        long extracted = FluxAppliedCompat.bridge().extractEnergy(this, accepted, false);
        if (extracted <= 0) {
            return;
        }
        int received = powered.receiveEnergy(null, (int) Math.min(extracted, Integer.MAX_VALUE), false);
        if (received < extracted) {
            FluxAppliedCompat.bridge().insertEnergy(this, extracted - received, false);
        }
        if (received > 0) {
            tile.markDirty();
        }
    }

    public boolean shouldReturnOutputsToNetwork() {
        return returnOutputsToNetwork;
    }

    public void setReturnOutputsToNetwork(boolean enabled) {
        if (returnOutputsToNetwork != enabled) {
            returnOutputsToNetwork = enabled;
            saveChanges();
        }
    }

    public boolean isBlockingEnabled() {
        return duality.getConfigManager().getSetting(Settings.BLOCK) == YesNo.YES;
    }

    public void saveChanges() {
        tile.markDirty();
    }

    public IGridNode getActionableNode() {
        return proxy.getNode();
    }

    @Nullable
    public IGrid getGrid() {
        try {
            return proxy.getGrid();
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    public IGridNode getGridNode(AEPartLocation side) {
        return enabled ? proxy.getNode() : null;
    }

    public AECableType getCableConnectionType(AEPartLocation side) {
        return enabled ? AECableType.SMART : AECableType.NONE;
    }

    public DimensionalCoord getLocation() {
        return new DimensionalCoord(tile);
    }

    public ItemStack getMachineRepresentation() {
        if (tile.getBlockType() instanceof BlockMachine) {
            return new ItemStack(tile.getBlockType(), 1, tile.getBlockMetadata());
        }
        return new ItemStack(ATItems.getPatternProviderAugment());
    }

    public String getCustomInventoryName() {
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        return tile.getDisplayName().getUnformattedText();
    }

    public boolean hasCustomInventoryName() {
        return customName != null && !customName.isEmpty();
    }

    public void setCustomName(@Nullable String name) {
        customName = name == null || name.isEmpty() ? null : name;
        saveChanges();
    }

    private boolean isInputSlot(int slot) {
        SlotConfig config = ((AccessorTileReconfigurable) tile).appliedthermal$getSlotConfig();
        return config != null && config.allowInsertionSlot != null && slot >= 0
            && slot < config.allowInsertionSlot.length && config.allowInsertionSlot[slot];
    }

    private boolean isOutputSlot(int slot) {
        SlotConfig config = ((AccessorTileReconfigurable) tile).appliedthermal$getSlotConfig();
        return config != null && config.allowExtractionSlot != null && slot >= 0
            && slot < config.allowExtractionSlot.length && config.allowExtractionSlot[slot];
    }
}
