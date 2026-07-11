package com.recursivepineapple.appliedthermal.provider;

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import cofh.lib.common.fluid.FluidStorageCoFH;
import cofh.lib.common.inventory.ItemStorageCoFH;
import com.recursivepineapple.appliedthermal.attachment.MachineAttachment;
import com.recursivepineapple.appliedthermal.init.ATItems;
import com.recursivepineapple.appliedthermal.menu.AppliedThermalPatternProviderMenu;
import com.recursivepineapple.appliedthermal.mixin.AugmentableBlockEntityAccessor;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;

/**
 * Owns the AE2 node and provider state attached to one Thermal machine.
 *
 * <p>The attachment talks to Thermal's explicit input/output storages so it
 * does not need to infer slot intent from Forge capability validity.</p>
 */
public final class AppliedThermalProviderAttachment implements MachineAttachment,
        PatternProviderLogicHost, IActionHost, IInWorldGridNodeHost {

    private static final String TAG_ROOT = "AppliedThermalProvider1201";
    private static final String TAG_ENABLED = "Enabled";
    private static final String TAG_OUTPUT_RETURN = "OutputReturn";

    private final BlockEntity machine;
    private IManagedGridNode mainNode;
    private ThermalMachinePatternProviderLogic logic;
    private boolean enabled;
    private boolean nodeRunning;
    private boolean outputReturnEnabled = true;
    @Nullable
    private CompoundTag savedState;

    public AppliedThermalProviderAttachment(BlockEntity machine) {
        this.machine = Objects.requireNonNull(machine, "machine");
        rebuildAeState();
    }

    public BlockEntity getMachine() {
        return machine;
    }

    @Override
    public boolean isAugmentInstalled(BlockEntity host) {
        if (!((Object) host instanceof AugmentableBlockEntityAccessor accessor)) {
            return false;
        }

        for (var slot : accessor.appliedthermal$getAugmentSlots()) {
            ItemStack stack = slot.getItemStack();
            if (!stack.isEmpty() && stack.is(ATItems.getPatternProviderAugment())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isNodeRunning() {
        return nodeRunning;
    }

    @Override
    public void startNode() {
        if (nodeRunning) {
            return;
        }

        enabled = true;
        nodeRunning = true;
        rebuildAeState();
        applySavedState();

        if (machine.getLevel() != null && !machine.getLevel().isClientSide()) {
            GridHelper.onFirstTick(machine, ignored -> createNode());
        }
    }

    @Override
    public void stopNode() {
        if (!nodeRunning) {
            return;
        }

        closeOpenMenus();
        captureState();
        nodeRunning = false;
        enabled = false;
        mainNode.destroy();
        rebuildAeState();
    }

    @Override
    public void onReady() {
        if (nodeRunning) {
            createNode();
        }
    }

    @Override
    public void invalidate() {
        closeOpenMenus();
        if (nodeRunning) {
            captureState();
            mainNode.destroy();
        }
        nodeRunning = false;
        rebuildAeState();
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        if (!tag.contains(TAG_ROOT)) {
            return;
        }

        CompoundTag providerTag = tag.getCompound(TAG_ROOT);
        savedState = providerTag.copy();
        enabled = providerTag.getBoolean(TAG_ENABLED);
        outputReturnEnabled = !providerTag.contains(TAG_OUTPUT_RETURN)
                || providerTag.getBoolean(TAG_OUTPUT_RETURN);
        applySavedState();
    }

    @Override
    public void writeToNBT(CompoundTag tag) {
        if (nodeRunning) {
            captureState();
        }
        if (savedState != null) {
            tag.put(TAG_ROOT, savedState.copy());
        }
    }

    @Override
    public void ejectAndClear() {
        closeOpenMenus();
        if (machine.getLevel() == null || machine.getLevel().isClientSide()) {
            logic.clearContent();
            return;
        }

        List<ItemStack> drops = new ArrayList<>();
        logic.addDrops(drops);
        logic.clearContent();
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                Block.popResource(machine.getLevel(), machine.getBlockPos(), drop);
            }
        }
        saveChanges();
    }

    @Override
    public PatternProviderLogic getLogic() {
        return logic;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return machine;
    }

    @Override
    public EnumSet<Direction> getTargets() {
        return EnumSet.noneOf(Direction.class);
    }

    @Override
    public void saveChanges() {
        machine.setChanged();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        AEItemKey icon = AEItemKey.of(new ItemStack(machine.getBlockState().getBlock()));
        return icon != null ? icon : AEItemKey.of(ATItems.getPatternProviderAugment());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return getTerminalIcon().toStack();
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        return logic.getTerminalGroup();
    }

    @Override
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    @Override
    public IGridNode getGridNode(Direction direction) {
        return enabled && nodeRunning ? mainNode.getNode() : null;
    }

    @Override
    public AECableType getCableConnectionType(Direction direction) {
        return enabled && nodeRunning ? AECableType.SMART : AECableType.NONE;
    }

    @Nullable
    public IGrid getGrid() {
        return mainNode.getGrid();
    }

    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    public boolean isEnabled() {
        return enabled && nodeRunning;
    }

    public boolean isOutputReturnEnabled() {
        return outputReturnEnabled;
    }

    public void setOutputReturnEnabled(boolean outputReturnEnabled) {
        if (this.outputReturnEnabled == outputReturnEnabled) {
            return;
        }
        this.outputReturnEnabled = outputReturnEnabled;
        saveChanges();
    }

    public boolean hasOutputItems() {
        for (ItemStorageCoFH storage : getOutputItemStorages()) {
            if (!storage.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOutputFluids() {
        for (FluidStorageCoFH storage : getOutputFluidStorages()) {
            if (!storage.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAnyInput() {
        for (ItemStorageCoFH storage : getInputItemStorages()) {
            if (!storage.isEmpty()) {
                return true;
            }
        }

        for (FluidStorageCoFH storage : getInputFluidStorages()) {
            if (!storage.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean containsInput(AEKey key) {
        if (key instanceof AEItemKey itemKey) {
            for (ItemStorageCoFH storage : getInputItemStorages()) {
                ItemStack stack = storage.getItemStack();
                if (!stack.isEmpty() && itemKey.matches(stack)) {
                    return true;
                }
            }
            return false;
        }

        if (key instanceof AEFluidKey fluidKey) {
            for (FluidStorageCoFH storage : getInputFluidStorages()) {
                FluidStack stack = storage.getFluidStack();
                if (!stack.isEmpty() && fluidKey.matches(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Simulates all selected inputs against virtual item slots and fluid tanks,
     * then commits them only if the complete set fits.
     */
    public boolean commitInputs(KeyCounter[] inputHolder) {
        List<InputRequest> requests;
        try {
            requests = collectInputs(inputHolder);
        } catch (ArithmeticException ex) {
            return false;
        }
        if (requests.isEmpty() || !simulateInputs(requests)) {
            return false;
        }

        List<? extends ItemStorageCoFH> itemStorages = getInputItemStorages();
        List<? extends FluidStorageCoFH> fluidStorages = getInputFluidStorages();
        ItemStack[] itemSnapshots = copyItemStorages(itemStorages);
        FluidStack[] fluidSnapshots = copyFluidStorages(fluidStorages);

        for (InputRequest request : requests) {
            if (insertInput(request.key(), request.amount(), Actionable.MODULATE) != request.amount()) {
                restoreInputStorages(itemStorages, itemSnapshots, fluidStorages, fluidSnapshots);
                return false;
            }
        }
        return true;
    }

    public long insertInput(AEKey key, long amount, Actionable action) {
        if (amount <= 0) {
            return 0;
        }

        if (key instanceof AEItemKey itemKey) {
            List<? extends ItemStorageCoFH> storages = getInputItemStorages();
            if (storages.isEmpty()) {
                return 0;
            }

            long inserted = 0;
            long remaining = amount;
            ItemStack sample = itemKey.toStack(1);
            while (remaining > 0) {
                int request = (int) Math.min(remaining, Integer.MAX_VALUE);
                ItemStack pending = itemKey.toStack(request);
                long movedThisRound = 0;
                for (ItemStorageCoFH storage : storages) {
                    if (pending.isEmpty()) {
                        break;
                    }
                    ItemStack current = storage.getItemStack();
                    if (!current.isEmpty() && itemKey.matches(current) && storage.isItemValid(0, sample)) {
                        ItemStack remainder = storage.insertItem(0, pending, action == Actionable.SIMULATE);
                        int moved = pending.getCount() - remainder.getCount();
                        if (moved > 0) {
                            inserted += moved;
                            movedThisRound += moved;
                            pending = remainder;
                        }
                    }
                }
                for (ItemStorageCoFH storage : storages) {
                    if (pending.isEmpty()) {
                        break;
                    }
                    if (!storage.getItemStack().isEmpty() || !storage.isItemValid(0, sample)) {
                        continue;
                    }
                    ItemStack remainder = storage.insertItem(0, pending, action == Actionable.SIMULATE);
                    int moved = pending.getCount() - remainder.getCount();
                    if (moved > 0) {
                        inserted += moved;
                        movedThisRound += moved;
                        pending = remainder;
                    }
                }
                if (movedThisRound == 0) {
                    break;
                }
                remaining -= movedThisRound;
            }
            return inserted;
        }

        if (key instanceof AEFluidKey fluidKey) {
            List<? extends FluidStorageCoFH> storages = getInputFluidStorages();
            if (storages.isEmpty()) {
                return 0;
            }

            long inserted = 0;
            long remaining = amount;
            while (remaining > 0) {
                int request = (int) Math.min(remaining, Integer.MAX_VALUE);
                FluidStack pending = fluidKey.toStack(request);
                long movedThisRound = 0;
                for (FluidStorageCoFH storage : storages) {
                    if (pending.isEmpty()) {
                        break;
                    }
                    FluidStack current = storage.getFluidStack();
                    if (!current.isEmpty() && fluidKey.matches(current)) {
                        int moved = storage.fill(pending,
                                action == Actionable.SIMULATE
                                        ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE
                                        : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                        if (moved > 0) {
                            inserted += moved;
                            movedThisRound += moved;
                            pending.setAmount(pending.getAmount() - moved);
                        }
                    }
                }
                for (FluidStorageCoFH storage : storages) {
                    if (pending.isEmpty()) {
                        break;
                    }
                    if (!storage.getFluidStack().isEmpty() || !storage.isFluidValid(pending)) {
                        continue;
                    }
                    int moved = storage.fill(pending,
                            action == Actionable.SIMULATE
                                    ? net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE
                                    : net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    if (moved > 0) {
                        inserted += moved;
                        movedThisRound += moved;
                        pending.setAmount(pending.getAmount() - moved);
                    }
                }
                if (movedThisRound == 0) {
                    break;
                }
                remaining -= movedThisRound;
            }
            return inserted;
        }

        return 0;
    }

    /**
     * Attempts to return item and fluid output stacks to the connected ME
     * inventory. The operation is intentionally simulate-then-modulate.
     */
    public boolean tryReturnOutputsToNetwork() {
        if (!outputReturnEnabled) {
            return false;
        }
        IGrid grid = getGrid();
        IGridNode node = getActionableNode();
        if (!isEnabled() || grid == null || node == null || !node.isActive()) {
            return false;
        }

        IStorageService storageService = grid.getStorageService();
        if (storageService == null) {
            return false;
        }

        MEStorage storage = storageService.getInventory();
        IActionSource source = IActionSource.ofMachine(this);
        boolean changed = tryReturnItemOutputs(grid, storage, source);
        changed |= tryReturnFluidOutputs(grid, storage, source);
        if (changed) {
            saveChanges();
        }
        return changed;
    }

    @Override
    public void tickServer() {
        tryReturnOutputsToNetwork();
    }

    public void refreshProviderState() {
        logic.updatePatterns();
        ICraftingProvider.requestUpdate(mainNode);
    }

    public void onMainNodeStateChanged() {
        logic.onMainNodeStateChanged();
    }

    private void rebuildAeState() {
        mainNode = GridHelper.createManagedNode(this, NodeListener.INSTANCE)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setInWorldNode(true)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .setVisualRepresentation(ATItems.getPatternProviderAugment())
                .setTagName("appliedthermal");
        logic = new ThermalMachinePatternProviderLogic(mainNode, this);
    }

    private void closeOpenMenus() {
        if (!(machine.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (var player : serverLevel.players()) {
            if (player.containerMenu instanceof AppliedThermalPatternProviderMenu menu
                    && menu.isFor(this)) {
                player.closeContainer();
            }
        }
    }

    private void createNode() {
        if (!nodeRunning || machine.getLevel() == null || machine.getLevel().isClientSide()
                || mainNode.isReady()) {
            return;
        }
        mainNode.create(machine.getLevel(), machine.getBlockPos());
        mainNode.setVisualRepresentation(getTerminalIcon());
        refreshProviderState();
    }

    private void captureState() {
        CompoundTag tag = new CompoundTag();
        logic.writeToNBT(tag);
        mainNode.saveToNBT(tag);
        tag.putBoolean(TAG_ENABLED, enabled);
        tag.putBoolean(TAG_OUTPUT_RETURN, outputReturnEnabled);
        savedState = tag;
    }

    private void applySavedState() {
        if (savedState == null) {
            return;
        }
        CompoundTag tag = savedState.copy();
        logic.readFromNBT(tag);
        mainNode.loadFromNBT(tag);
        enabled = tag.getBoolean(TAG_ENABLED);
        outputReturnEnabled = !tag.contains(TAG_OUTPUT_RETURN)
                || tag.getBoolean(TAG_OUTPUT_RETURN);
        refreshProviderState();
    }

    private void notifyStackReturnedToNetwork(AEKey key, long amount) {
        if (amount <= 0) {
            return;
        }
        logic.notifyStackReturnedToNetwork(new GenericStack(key, amount));
    }

    private List<? extends ItemStorageCoFH> getInputItemStorages() {
        if ((Object) machine instanceof AugmentableBlockEntityAccessor accessor) {
            return accessor.appliedthermal$getMachineInventory().getInputSlots();
        }
        return List.of();
    }

    private List<? extends FluidStorageCoFH> getInputFluidStorages() {
        if ((Object) machine instanceof AugmentableBlockEntityAccessor accessor) {
            return accessor.appliedthermal$getMachineTankInventory().getInputTanks();
        }
        return List.of();
    }

    private List<? extends ItemStorageCoFH> getOutputItemStorages() {
        if ((Object) machine instanceof AugmentableBlockEntityAccessor accessor) {
            return accessor.appliedthermal$getMachineInventory().getOutputSlots();
        }
        return List.of();
    }

    private List<? extends FluidStorageCoFH> getOutputFluidStorages() {
        if ((Object) machine instanceof AugmentableBlockEntityAccessor accessor) {
            return accessor.appliedthermal$getMachineTankInventory().getOutputTanks();
        }
        return List.of();
    }

    private void restoreItemOutput(ItemStorageCoFH storage, ItemStack extracted, long inserted) {
        long shortfall = extracted.getCount() - inserted;
        if (shortfall <= 0) {
            return;
        }

        ItemStack current = storage.getItemStack();
        if (current.isEmpty()) {
            ItemStack restored = extracted.copy();
            restored.setCount((int) Math.min(shortfall, Integer.MAX_VALUE));
            storage.setItemStack(restored);
        } else {
            current.grow((int) Math.min(shortfall, Integer.MAX_VALUE));
        }
    }

    private void restoreFluidOutput(FluidStorageCoFH storage, FluidStack drained, long inserted) {
        long shortfall = drained.getAmount() - inserted;
        if (shortfall <= 0) {
            return;
        }

        FluidStack current = storage.getFluidStack();
        if (current.isEmpty()) {
            FluidStack restored = drained.copy();
            restored.setAmount((int) Math.min(shortfall, Integer.MAX_VALUE));
            storage.setFluidStack(restored);
        } else {
            current.grow((int) Math.min(shortfall, Integer.MAX_VALUE));
        }
    }

    private boolean simulateInputs(List<InputRequest> requests) {
        ItemSimulation itemSimulation = null;
        FluidSimulation fluidSimulation = null;
        List<? extends ItemStorageCoFH> inputItems = getInputItemStorages();
        List<? extends FluidStorageCoFH> inputFluids = getInputFluidStorages();

        for (InputRequest request : requests) {
            if (request.key() instanceof AEItemKey itemKey) {
                if (itemSimulation == null) {
                    if (inputItems.isEmpty()) {
                        return false;
                    }
                    itemSimulation = new ItemSimulation(inputItems);
                }
                if (!itemSimulation.insert(itemKey, request.amount())) {
                    return false;
                }
            } else if (request.key() instanceof AEFluidKey fluidKey) {
                if (fluidSimulation == null) {
                    if (inputFluids.isEmpty()) {
                        return false;
                    }
                    fluidSimulation = new FluidSimulation(inputFluids);
                }
                if (!fluidSimulation.insert(fluidKey, request.amount())) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private static List<InputRequest> collectInputs(KeyCounter[] inputHolder) {
        Map<AEKey, Long> amounts = new LinkedHashMap<>();
        for (KeyCounter counter : inputHolder) {
            for (var entry : counter) {
                if (entry.getLongValue() <= 0) {
                    continue;
                }
                amounts.merge(entry.getKey(), entry.getLongValue(), AppliedThermalProviderAttachment::addExact);
            }
        }

        List<InputRequest> requests = new ArrayList<>(amounts.size());
        for (var entry : amounts.entrySet()) {
            requests.add(new InputRequest(entry.getKey(), entry.getValue()));
        }
        return requests;
    }

    private static long addExact(long left, long right) {
        return Math.addExact(left, right);
    }

    private static ItemStack[] copyItemStorages(List<? extends ItemStorageCoFH> storages) {
        ItemStack[] snapshots = new ItemStack[storages.size()];
        for (int slot = 0; slot < snapshots.length; slot++) {
            snapshots[slot] = storages.get(slot).getItemStack().copy();
        }
        return snapshots;
    }

    private static FluidStack[] copyFluidStorages(List<? extends FluidStorageCoFH> storages) {
        FluidStack[] snapshots = new FluidStack[storages.size()];
        for (int tank = 0; tank < snapshots.length; tank++) {
            snapshots[tank] = storages.get(tank).getFluidStack().copy();
        }
        return snapshots;
    }

    private void restoreInputStorages(List<? extends ItemStorageCoFH> itemStorages,
            ItemStack[] itemSnapshots,
            List<? extends FluidStorageCoFH> fluidStorages,
            FluidStack[] fluidSnapshots) {
        for (int slot = 0; slot < itemSnapshots.length; slot++) {
            itemStorages.get(slot).setItemStack(itemSnapshots[slot].copy());
        }
        for (int tank = 0; tank < fluidSnapshots.length; tank++) {
            fluidStorages.get(tank).setFluidStack(fluidSnapshots[tank].copy());
        }
        saveChanges();
    }

    private boolean tryReturnItemOutputs(IGrid grid, MEStorage storage, IActionSource source) {
        List<? extends ItemStorageCoFH> outputs = getOutputItemStorages();
        if (outputs.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (int slot = 0; slot < outputs.size(); slot++) {
            ItemStorageCoFH output = outputs.get(slot);
            ItemStack stack = output.getItemStack();
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack simulated = output.extractItem(0, stack.getCount(), true);
            if (simulated.isEmpty()) {
                continue;
            }

            AEItemKey key = AEItemKey.of(simulated);
            if (key == null) {
                continue;
            }
            long accepted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, simulated.getCount(), source,
                    Actionable.SIMULATE);
            if (accepted <= 0) {
                continue;
            }

            ItemStack extracted = output.extractItem(0, (int) Math.min(accepted, Integer.MAX_VALUE), false);
            if (extracted.isEmpty()) {
                continue;
            }

            long inserted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, extracted.getCount(), source,
                    Actionable.MODULATE);
            if (inserted <= 0) {
                restoreItemOutput(output, extracted, 0);
                continue;
            }

            if (inserted < extracted.getCount()) {
                restoreItemOutput(output, extracted, inserted);
            }

            notifyStackReturnedToNetwork(key, inserted);
            changed = true;
        }
        return changed;
    }

    private boolean tryReturnFluidOutputs(IGrid grid, MEStorage storage, IActionSource source) {
        List<? extends FluidStorageCoFH> outputs = getOutputFluidStorages();
        if (outputs.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (int tank = 0; tank < outputs.size(); tank++) {
            FluidStorageCoFH output = outputs.get(tank);
            FluidStack stack = output.getFluidStack();
            if (stack.isEmpty()) {
                continue;
            }

            FluidStack simulated = output.drain(stack.getAmount(),
                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) {
                continue;
            }

            AEFluidKey key = AEFluidKey.of(simulated);
            if (key == null) {
                continue;
            }

            long amount = simulated.getAmount();
            long accepted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, amount, source,
                    Actionable.SIMULATE);
            if (accepted <= 0) {
                continue;
            }

            FluidStack drained = output.drain((int) Math.min(accepted, Integer.MAX_VALUE),
                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }

            long inserted = StorageHelper.poweredInsert(grid.getEnergyService(), storage, key, drained.getAmount(), source,
                    Actionable.MODULATE);
            if (inserted <= 0) {
                restoreFluidOutput(output, drained, 0);
                continue;
            }

            if (inserted < drained.getAmount()) {
                restoreFluidOutput(output, drained, inserted);
            }

            notifyStackReturnedToNetwork(key, inserted);
            changed = true;
        }
        return changed;
    }

    private record InputRequest(AEKey key, long amount) {
    }

    private static final class ItemSimulation {
        private final List<? extends ItemStorageCoFH> storages;
        private final ItemStack[] slots;

        private ItemSimulation(List<? extends ItemStorageCoFH> storages) {
            this.storages = storages;
            this.slots = new ItemStack[storages.size()];
            for (int slot = 0; slot < slots.length; slot++) {
                slots[slot] = storages.get(slot).getItemStack().copy();
            }
        }

        private boolean insert(AEItemKey key, long amount) {
            if (amount <= 0) {
                return true;
            }

            ItemStack sample = key.toStack(1);
            long remaining = amount;
            for (int slot = 0; slot < slots.length && remaining > 0; slot++) {
                ItemStack current = slots[slot];
                ItemStorageCoFH storage = storages.get(slot);
                if (!current.isEmpty() && key.matches(current)
                        && storage.isItemValid(0, sample)) {
                    int capacity = Math.min(storage.getSlotLimit(0), current.getMaxStackSize())
                            - current.getCount();
                    int moved = (int) Math.min(Math.max(capacity, 0), remaining);
                    if (moved > 0) {
                        current.grow(moved);
                        remaining -= moved;
                    }
                }
            }

            for (int slot = 0; slot < slots.length && remaining > 0; slot++) {
                ItemStorageCoFH storage = storages.get(slot);
                if (!slots[slot].isEmpty() || !storage.isItemValid(0, sample)) {
                    continue;
                }
                int limit = Math.min(storage.getSlotLimit(0), sample.getMaxStackSize());
                int moved = (int) Math.min(Math.max(limit, 0), remaining);
                if (moved > 0) {
                    slots[slot] = sample.copy();
                    slots[slot].setCount(moved);
                    remaining -= moved;
                }
            }
            return remaining == 0;
        }
    }

    private static final class FluidSimulation {
        private final List<? extends FluidStorageCoFH> storages;
        private final FluidStack[] tanks;

        private FluidSimulation(List<? extends FluidStorageCoFH> storages) {
            this.storages = storages;
            this.tanks = new FluidStack[storages.size()];
            for (int tank = 0; tank < tanks.length; tank++) {
                tanks[tank] = storages.get(tank).getFluidStack().copy();
            }
        }

        private boolean insert(AEFluidKey key, long amount) {
            if (amount <= 0) {
                return true;
            }

            FluidStack sample = key.toStack(1);
            long remaining = amount;
            for (int tank = 0; tank < tanks.length && remaining > 0; tank++) {
                FluidStack current = tanks[tank];
                FluidStorageCoFH storage = storages.get(tank);
                if (!current.isEmpty() && key.matches(current)) {
                    int capacity = storage.getTankCapacity(0) - current.getAmount();
                    int moved = (int) Math.min(Math.max(capacity, 0), remaining);
                    if (moved > 0) {
                        current.grow(moved);
                        remaining -= moved;
                    }
                }
            }

            for (int tank = 0; tank < tanks.length && remaining > 0; tank++) {
                FluidStorageCoFH storage = storages.get(tank);
                if (!tanks[tank].isEmpty() || !storage.isFluidValid(sample)) {
                    continue;
                }
                int moved = (int) Math.min(Math.max(storage.getTankCapacity(0), 0), remaining);
                if (moved > 0) {
                    tanks[tank] = sample.copy();
                    tanks[tank].setAmount(moved);
                    remaining -= moved;
                }
            }
            return remaining == 0;
        }
    }

    private enum NodeListener implements IGridNodeListener<AppliedThermalProviderAttachment> {
        INSTANCE;

        @Override
        public void onSaveChanges(AppliedThermalProviderAttachment owner, IGridNode node) {
            owner.saveChanges();
        }

        @Override
        public void onStateChanged(AppliedThermalProviderAttachment owner, IGridNode node,
                IGridNodeListener.State reason) {
            owner.onMainNodeStateChanged();
        }
    }
}
