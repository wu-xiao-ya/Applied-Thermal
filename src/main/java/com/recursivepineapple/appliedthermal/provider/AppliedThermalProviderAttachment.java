package com.recursivepineapple.appliedthermal.provider;

import ae2.api.config.Actionable;
import ae2.api.crafting.IPatternDetails;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.networking.GridFlags;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IInWorldGridNodeHost;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.security.IActionHost;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.storage.IStorageService;
import ae2.api.orientation.BlockOrientation;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.api.storage.StorageHelper;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.util.AECableType;
import ae2.util.Platform;
import cofh.core.block.TileReconfigurable;
import cofh.core.block.TilePowered;
import cofh.core.fluid.FluidTankCore;
import cofh.core.util.core.SlotConfig;
import cofh.core.util.helpers.ItemHelper;
import cofh.thermalexpansion.block.machine.BlockMachine;
import cofh.thermalexpansion.block.machine.TileBrewer;
import cofh.thermalexpansion.block.machine.TileCentrifuge;
import cofh.thermalexpansion.block.machine.TileCrucible;
import cofh.thermalexpansion.block.machine.TileFurnace;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import cofh.thermalexpansion.block.machine.TileRefinery;
import cofh.thermalexpansion.block.machine.TileSawmill;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.integration.appflux.AppFluxCompat;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.init.ATItems;
import com.recursivepineapple.appliedthermal.network.ATGuiIds;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fluids.FluidStack;

public final class AppliedThermalProviderAttachment implements IActionHost, IInWorldGridNodeHost {

    private static final String TAG_ROOT = "AppliedThermalProvider";
    private static final String TAG_RETURN_OUTPUTS = "ReturnOutputsToNetwork";
    @Nullable
    private static Field slotConfigField;
    private static boolean slotConfigFieldResolved;

    private final TileMachineBase tile;
    private IManagedGridNode mainNode;
    private ThermalMachinePatternProviderLogic logic;
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

    private void rebuildAeState() {
        this.mainNode = createManagedNode();
        this.logic = new ThermalMachinePatternProviderLogic(this.mainNode, 36, this);
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

    public ThermalMachinePatternProviderLogic getLogic() {
        return logic;
    }

    public TileMachineBase getTileEntity() {
        return tile;
    }

    public void openGui(EntityPlayer player) {
        if (!enabled || tile.isInvalid() || tile.getWorld() != player.world || player.world.isRemote
            || !tile.canPlayerAccess(player)) {
            return;
        }
        BlockPos pos = tile.getPos();
        if (player.world.isBlockLoaded(pos) && player.world.getTileEntity(pos) == tile) {
            player.openGui(AppliedThermal.instance, ATGuiIds.PATTERN_PROVIDER, player.world,
                pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public void setCustomName(@Nullable String name) {
        this.customName = name;
        saveChanges();
    }

    public void saveChanges() {
        tile.markDirty();
    }

    public ItemStack getMainContainerIcon() {
        if (tile.getBlockType() instanceof BlockMachine) {
            return new ItemStack(tile.getBlockType(), 1, tile.getBlockMetadata());
        }
        return new ItemStack(ATItems.getPatternProviderAugment());
    }

    public PatternContainerGroup getTerminalGroup() {
        AEItemKey icon = AEItemKey.of(getMainContainerIcon());
        ITextComponent name = customName == null || customName.isEmpty()
            ? tile.getDisplayName() : new TextComponentString(customName);
        return new PatternContainerGroup(icon, name, java.util.Collections.emptyList());
    }

    public IUpgradeInventory getUpgrades() {
        return this.logic.getUpgrades();
    }

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

    /**
     * Simulates and commits the complete input sequence emitted by an AE processing pattern.
     * Every request is planned against one shared inventory snapshot, so repeated ingredients
     * cannot each claim the same free slot capacity.
     */
    public boolean pushInputsAtomically(IPatternDetails pattern, KeyCounter[] inputHolder) {
        List<InputRequest> requests = new ArrayList<>();
        pattern.pushInputsToExternalInventory(inputHolder,
            (what, amount) -> requests.add(new InputRequest(what, amount)));
        if (requests.isEmpty()) {
            return false;
        }

        List<ItemStack> plannedSlots = new ArrayList<>(tile.getSizeInventory());
        for (int slot = 0; slot < tile.getSizeInventory(); slot++) {
            plannedSlots.add(tile.getStackInSlot(slot).copy());
        }
        for (InputRequest request : requests) {
            if (!applyInputRequest(request.key, request.amount, plannedSlots)) {
                return false;
            }
        }

        for (int slot = 0; slot < plannedSlots.size(); slot++) {
            ItemStack planned = plannedSlots.get(slot);
            if (!ItemStack.areItemStacksEqual(tile.getStackInSlot(slot), planned)) {
                tile.setInventorySlotContents(slot, planned);
            }
        }
        tile.markDirty();
        return true;
    }

    private boolean applyInputRequest(AEKey what, long amount, List<ItemStack> slots) {
        if (!(what instanceof AEItemKey) || amount <= 0 || amount > Integer.MAX_VALUE) {
            return false;
        }
        ItemStack remaining = ((AEItemKey) what).toStack((int) amount);
        if (remaining.isEmpty()) {
            return false;
        }
        // Match existing stacks first so an earlier ingredient cannot consume an empty slot
        // that is the only valid destination for a later, different ingredient.
        for (int slot = 0; slot < slots.size() && !remaining.isEmpty(); slot++) {
            if (!isInputSlot(slot) || !tile.isItemValidForSlot(slot, remaining)) {
                continue;
            }
            ItemStack existing = slots.get(slot);
            if (existing.isEmpty() || !ItemHelper.itemsIdentical(existing, remaining)) {
                continue;
            }
            int limit = Math.min(remaining.getMaxStackSize(), tile.getInventoryStackLimit());
            limit = Math.min(limit, existing.getMaxStackSize());
            int move = Math.min(remaining.getCount(), Math.max(0, limit - existing.getCount()));
            if (move > 0) {
                existing.grow(move);
                remaining.shrink(move);
            }
        }
        for (int slot = 0; slot < slots.size() && !remaining.isEmpty(); slot++) {
            if (!isInputSlot(slot) || !slots.get(slot).isEmpty() || !tile.isItemValidForSlot(slot, remaining)) {
                continue;
            }
            int move = Math.min(remaining.getCount(),
                Math.min(remaining.getMaxStackSize(), tile.getInventoryStackLimit()));
            ItemStack placed = remaining.copy();
            placed.setCount(move);
            slots.set(slot, placed);
            remaining.shrink(move);
        }
        return remaining.isEmpty();
    }

    private static final class InputRequest {
        private final AEKey key;
        private final long amount;

        private InputRequest(AEKey key, long amount) {
            this.key = key;
            this.amount = amount;
        }
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
        if (!enabled || tile.getWorld() == null || tile.getWorld().isRemote) {
            return;
        }
        logic.flushReturnInventory();
        if (!returnOutputsToNetwork) {
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
            logic.notifyStackReturnedToNetwork(key, inserted);
            tile.markDirty();
        }
        tryReturnFluidOutput(grid, storage, source);
    }

    private void tryReturnFluidOutput(IGrid grid, MEStorage storage, IActionSource source) {
        FluidTankCore tank = getFluidOutputTank();
        if (tank == null) {
            return;
        }
        FluidStack fluid = tank.getFluid();
        AEFluidKey key = AEFluidKey.of(fluid);
        if (key == null) {
            return;
        }
        long accepted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, fluid.amount, source,
            Actionable.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        FluidStack drained = tank.drain(key.toStack((int) accepted), true);
        if (drained == null || drained.amount <= 0) {
            return;
        }
        long inserted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, drained.amount, source,
            Actionable.MODULATE);
        if (inserted < drained.amount) {
            // Storage can accept less during the real insertion. Put the remainder back in its output tank.
            tank.fill(new FluidStack(drained, drained.amount - (int) inserted), true);
        }
        if (inserted > 0) {
            tile.markDirty();
            logic.notifyStackReturnedToNetwork(key, inserted);
        }
    }

    @Nullable
    private FluidTankCore getFluidOutputTank() {
        // Brewer and Refinery reserve tank 0 for recipe inputs and tank 1 for outputs.
        if (tile instanceof TileBrewer) {
            return ((TileBrewer) tile).getTank(1);
        }
        if (tile instanceof TileRefinery) {
            return ((TileRefinery) tile).getTank(1);
        }
        if (tile instanceof TileCentrifuge) {
            return ((TileCentrifuge) tile).getTank();
        }
        if (tile instanceof TileCrucible) {
            return ((TileCrucible) tile).getTank();
        }
        if (tile instanceof TileFurnace) {
            return ((TileFurnace) tile).getTank();
        }
        if (tile instanceof TileSawmill) {
            return ((TileSawmill) tile).getTank();
        }
        // The Transposer's shared tank can still hold recipe inputs after switching to extraction mode.
        return null;
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
