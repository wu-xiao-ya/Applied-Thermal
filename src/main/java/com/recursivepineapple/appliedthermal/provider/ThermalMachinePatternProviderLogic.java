package com.recursivepineapple.appliedthermal.provider;

import ae2.api.config.Actionable;
import ae2.api.config.BlockingMode;
import ae2.api.config.LockCraftingMode;
import ae2.api.config.PatternProviderBlockingType;
import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.inventories.BaseInternalInventory;
import ae2.api.inventories.InternalInventory;
import ae2.api.inventories.VersionedInternalInventory;
import ae2.api.networking.IGrid;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.StorageHelper;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.api.upgrades.Upgrades;
import ae2.api.util.IConfigManager;
import ae2.core.AEConfig;
import ae2.core.definitions.AEItems;
import ae2.crafting.pattern.AEProcessingPattern;
import ae2.helpers.patternprovider.PatternProviderCapacity;
import ae2.helpers.patternprovider.PatternProviderReturnInventory;
import ae2.helpers.patternprovider.PseudoPatternDetails;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import ae2.util.inv.filter.IAEItemFilter;
import com.recursivepineapple.appliedthermal.init.ATItems;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

public final class ThermalMachinePatternProviderLogic implements ICraftingProvider, InternalInventoryHost {

    private static final String TAG_PATTERNS = "patterns";
    private static final String TAG_UPGRADES = "upgrades";
    private static final String TAG_PRIORITY = "priority";
    private static final String TAG_RETURN_INVENTORY = "returnInv";
    private static final String TAG_UNLOCK_EVENT = "unlockEvent";
    private static final String TAG_UNLOCK_STACK = "unlockStack";
    private static final String TAG_RECOVERED_INPUTS = "recoveredInputs";
    private static final byte RESULT_UNLOCK_EVENT = 2;

    private final IManagedGridNode mainNode;
    private final AppliedThermalProviderAttachment attachment;
    private final IConfigManager configManager;
    private final AppEngInternalInventory patternInventory;
    private final InternalInventory terminalInventory = new TerminalInventory();
    private final IUpgradeInventory upgrades;
    private final PatternProviderReturnInventory returnInventory;
    private final IActionSource returnSource;
    private final List<GenericStack> recoveredInputs = new ArrayList<>();

    private List<IPatternDetails> availablePatterns = Collections.emptyList();
    private Set<AEItemKey> activePatternKeys = Collections.emptySet();
    private long terminalContentsVersion;
    private int priority;
    private boolean wasOnline;
    @Nullable
    private AEItemKey lastPushedPattern;
    @Nullable
    private GenericStack unlockStack;
    @Nullable
    private AEKey unlockKey;

    public ThermalMachinePatternProviderLogic(IManagedGridNode mainNode, int patternInventorySize,
                                              AppliedThermalProviderAttachment attachment) {
        this.mainNode = mainNode;
        this.attachment = attachment;
        this.configManager = IConfigManager.builder(this::onSettingChanged)
            .registerSetting(Settings.BLOCKING_MODE, BlockingMode.NO)
            .registerSetting(Settings.PATTERN_PROVIDER_BLOCKING_TYPE, PatternProviderBlockingType.NORMAL)
            .registerSetting(Settings.PATTERN_ACCESS_TERMINAL, YesNo.YES)
            .registerSetting(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE)
            .build();
        this.patternInventory = new AppEngInternalInventory(this, patternInventorySize, 1, new PatternFilter());
        Item machineType = ATItems.getPatternProviderAugment();
        this.upgrades = new UpgradeInventory(machineType,
            UpgradeInventories.getMachineUpgradeSlots(machineType, 1 + getExpansionLimit()));
        this.returnInventory = new PatternProviderReturnInventory(attachment::saveChanges);
        this.returnSource = IActionSource.ofMachine(attachment);
        mainNode.addService(ICraftingProvider.class, this);
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return attachment.isEnabled() && mainNode.isOnline() ? availablePatterns : Collections.emptyList();
    }

    @Override
    public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs, int multiplier) {
        if (multiplier != 1 || !mainNode.isActive() || !getAvailablePatterns().contains(pattern)
            || getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }
        IPatternDetails processing = PseudoPatternDetails.unwrap(pattern);
        if (isBlocked(processing, inputs) || !attachment.pushInputsAtomically(processing, inputs)) {
            return false;
        }
        lastPushedPattern = processing.getDefinition();
        if (configManager.getSetting(Settings.LOCK_CRAFTING_MODE) == LockCraftingMode.LOCK_UNTIL_RESULT) {
            setUnlockStack(processing.getPrimaryOutput());
        }
        attachment.saveChanges();
        return true;
    }

    @Override
    public boolean isBusy() {
        return unlockStack != null;
    }

    private boolean isBlocked(IPatternDetails pattern, KeyCounter[] inputs) {
        BlockingMode mode = configManager.getSetting(Settings.BLOCKING_MODE);
        if (mode == BlockingMode.NO
            || configManager.getSetting(Settings.PATTERN_PROVIDER_BLOCKING_TYPE) == PatternProviderBlockingType.SMART
                && pattern.getDefinition().equals(lastPushedPattern)) {
            return false;
        }
        if (mode == BlockingMode.STRONG) {
            for (KeyCounter counter : inputs) {
                for (Object2LongMap.Entry<AEKey> input : counter) {
                    if (attachment.containsInput(input.getKey())) {
                        return true;
                    }
                }
            }
            return false;
        }
        return attachment.containsAnyInput();
    }

    private void onSettingChanged(IConfigManager manager, Setting<?> setting) {
        if (setting == Settings.LOCK_CRAFTING_MODE) {
            LockCraftingMode mode = manager.getSetting(Settings.LOCK_CRAFTING_MODE);
            if (mode != LockCraftingMode.NONE && mode != LockCraftingMode.LOCK_UNTIL_RESULT) {
                manager.putSetting(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE);
                return;
            }
            if (mode == LockCraftingMode.NONE) {
                setUnlockStack(null);
            }
        }
        attachment.saveChanges();
    }

    public LockCraftingMode getCraftingLockedReason() {
        return unlockStack == null ? LockCraftingMode.NONE : LockCraftingMode.LOCK_UNTIL_RESULT;
    }

    @Nullable
    public GenericStack getUnlockStack() {
        return unlockStack;
    }

    private void setUnlockStack(@Nullable GenericStack stack) {
        unlockStack = stack;
        unlockKey = stack == null ? null : stack.what();
    }

    public void notifyStackReturnedToNetwork(AEKey key, long amount) {
        if (amount <= 0 || !key.equals(unlockKey)) {
            return;
        }
        long remaining = GenericStack.getStackSizeOrZero(unlockStack) - amount;
        if (remaining > 0) {
            unlockStack = new GenericStack(key, remaining);
        } else {
            setUnlockStack(null);
        }
        attachment.saveChanges();
    }

    public void flushReturnInventory() {
        if (!attachment.isEnabled() || !mainNode.isActive()
            || (returnInventory.isEmpty() && recoveredInputs.isEmpty())) {
            return;
        }
        IGrid grid = getGrid();
        if (grid != null) {
            returnInventory.injectIntoNetwork(grid.getStorageService().getInventory(), returnSource,
                returned -> notifyStackReturnedToNetwork(returned.what(), returned.amount()));
            restoreRecoveredInputs(grid);
        }
    }

    private void restoreRecoveredInputs(IGrid grid) {
        ListIterator<GenericStack> entries = recoveredInputs.listIterator();
        while (entries.hasNext()) {
            GenericStack stack = entries.next();
            AEKey key = stack.what();
            long amount = stack.amount();
            long inserted = StorageHelper.poweredInsert(grid.getEnergyService(),
                grid.getStorageService().getInventory(), key, amount, returnSource, Actionable.MODULATE);
            if (inserted > 0) {
                if (inserted >= amount) {
                    entries.remove();
                } else {
                    entries.set(new GenericStack(key, amount - inserted));
                }
                attachment.saveChanges();
            }
        }
    }

    public void writeToNBT(NBTTagCompound tag) {
        configManager.writeToNBT(tag);
        patternInventory.writeToNBT(tag, TAG_PATTERNS);
        upgrades.writeToNBT(tag, TAG_UPGRADES);
        tag.setInteger(TAG_PRIORITY, priority);
        tag.setTag(TAG_RETURN_INVENTORY, returnInventory.writeToTag());
        NBTTagList recovered = new NBTTagList();
        for (GenericStack stack : recoveredInputs) {
            recovered.appendTag(GenericStack.writeTag(stack));
        }
        tag.setTag(TAG_RECOVERED_INPUTS, recovered);
        if (unlockStack != null) {
            tag.setByte(TAG_UNLOCK_EVENT, RESULT_UNLOCK_EVENT);
            tag.setTag(TAG_UNLOCK_STACK, GenericStack.writeTag(unlockStack));
        } else {
            tag.removeTag(TAG_UNLOCK_EVENT);
            tag.removeTag(TAG_UNLOCK_STACK);
        }
    }

    public void readFromNBT(NBTTagCompound tag) {
        configManager.readFromNBT(tag);
        patternInventory.readFromNBT(tag, TAG_PATTERNS);
        upgrades.readFromNBT(tag, TAG_UPGRADES);
        priority = tag.getInteger(TAG_PRIORITY);
        returnInventory.readFromChildTag(tag, TAG_RETURN_INVENTORY);
        setUnlockStack(tag.getByte(TAG_UNLOCK_EVENT) == RESULT_UNLOCK_EVENT
            && configManager.getSetting(Settings.LOCK_CRAFTING_MODE) == LockCraftingMode.LOCK_UNTIL_RESULT
            ? GenericStack.readTag(tag.getCompoundTag(TAG_UNLOCK_STACK)) : null);
        if (GenericStack.getStackSizeOrZero(unlockStack) <= 0) {
            setUnlockStack(null);
        }
        readRecoveredInputs(tag);
        updatePatterns();
    }

    private void readRecoveredInputs(NBTTagCompound tag) {
        recoveredInputs.clear();
        if (tag.hasKey(TAG_RECOVERED_INPUTS, Constants.NBT.TAG_LIST)) {
            readRecoveredInputList(tag.getTagList(TAG_RECOVERED_INPUTS, Constants.NBT.TAG_COMPOUND), false);
        } else if (tag.hasKey("pendingSendList", Constants.NBT.TAG_LIST)) {
            readRecoveredInputList(tag.getTagList("pendingSendList", Constants.NBT.TAG_COMPOUND), true);
        } else {
            readRecoveredInputList(tag.getTagList("sendList", Constants.NBT.TAG_COMPOUND), false);
        }
    }

    private void readRecoveredInputList(NBTTagList list, boolean nested) {
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            GenericStack stack = GenericStack.readTag(nested ? entry.getCompoundTag("stack") : entry);
            if (GenericStack.getStackSizeOrZero(stack) > 0) {
                recoveredInputs.add(stack);
            }
        }
    }

    public void updatePatterns() {
        terminalContentsVersion++;
        World world = attachment.getTileEntity().getWorld();
        List<IPatternDetails> patterns = new ArrayList<>();
        Set<AEItemKey> keys = new HashSet<>();
        if (world != null) {
            boolean pseudoCrafting = upgrades.isInstalled(AEItems.PSEUDO_CRAFTING_CARD.item());
            for (int slot = 0; slot < getActivePatternSlots(); slot++) {
                ItemStack stack = patternInventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    IPatternDetails details = PatternDetailsHelper.decodePattern(stack, world);
                    if (details instanceof AEProcessingPattern) {
                        patterns.add(pseudoCrafting ? PseudoPatternDetails.wrap(details) : details);
                        keys.add(AEItemKey.of(stack));
                    }
                }
            }
        }
        availablePatterns = Collections.unmodifiableList(patterns);
        activePatternKeys = Collections.unmodifiableSet(keys);
        if (lastPushedPattern != null && !keys.contains(lastPushedPattern)) {
            lastPushedPattern = null;
        }
        ICraftingProvider.requestUpdate(mainNode);
    }

    public void onMainNodeStateChanged() {
        boolean online = mainNode.isOnline();
        if (wasOnline != online) {
            wasOnline = online;
            ICraftingProvider.requestUpdate(mainNode);
        }
    }

    @Nullable
    public IGrid getGrid() {
        return mainNode.getGrid();
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        return attachment.getTerminalGroup();
    }

    @Override
    public int getPatternPriority() {
        return priority;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (this.priority != priority) {
            this.priority = priority;
            attachment.saveChanges();
            ICraftingProvider.requestUpdate(mainNode);
        }
    }

    public IConfigManager getConfigManager() {
        return configManager;
    }

    public AppEngInternalInventory getPatternInv() {
        return patternInventory;
    }

    public InternalInventory getTerminalPatternInv() {
        return terminalInventory;
    }

    public PatternProviderReturnInventory getReturnInv() {
        return returnInventory;
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    public boolean containsPattern(AEItemKey pattern) {
        return activePatternKeys.contains(pattern);
    }

    public int getActivePatternSlots() {
        return Math.min(patternInventory.size(),
            PatternProviderCapacity.getActivePatternSlots(getInstalledCapacityCards(), getExpansionLimit()));
    }

    public int getPatternPageCount() {
        return PatternProviderCapacity.getPageCount(getActivePatternSlots());
    }

    public boolean isPatternSlotEnabled(int slot) {
        return slot >= 0 && slot < getActivePatternSlots();
    }

    public int getInstalledCapacityCards() {
        return upgrades.getInstalledUpgrades(AEItems.PATTERN_EXPANSION_CARD.item());
    }

    private static int getExpansionLimit() {
        return AEConfig.instance().getPatternProviderExpansionCardLimit();
    }

    public void addDrops(List<ItemStack> drops) {
        for (ItemStack stack : patternInventory) {
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        TileEntity tile = attachment.getTileEntity();
        if (tile.getWorld() != null) {
            returnInventory.addDrops(drops, tile.getWorld(), tile.getPos());
            for (GenericStack stack : recoveredInputs) {
                stack.what().addDrops(stack.amount(), drops, tile.getWorld(), tile.getPos());
            }
        }
    }

    public void clearContent() {
        patternInventory.clear();
        upgrades.clear();
        returnInventory.clear();
        recoveredInputs.clear();
        setUnlockStack(null);
        lastPushedPattern = null;
        updatePatterns();
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inventory) {
        attachment.saveChanges();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inventory, int slot) {
        attachment.saveChanges();
        updatePatterns();
    }

    @Override
    public boolean isClientSide() {
        World world = attachment.getTileEntity().getWorld();
        return world == null || world.isRemote;
    }

    private final class PatternFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
            World world = attachment.getTileEntity().getWorld();
            if (stack.isEmpty() || world == null || !isPatternSlotEnabled(slot)
                || !(PatternDetailsHelper.decodePattern(stack, world) instanceof AEProcessingPattern)) {
                return false;
            }
            for (int other = 0; other < getActivePatternSlots(); other++) {
                ItemStack existing = inventory.getStackInSlot(other);
                if (other != slot && ItemStack.areItemsEqual(stack, existing)
                    && ItemStack.areItemStackTagsEqual(stack, existing)) {
                    return false;
                }
            }
            return true;
        }
    }

    private final class TerminalInventory extends BaseInternalInventory implements VersionedInternalInventory {
        @Override
        public long getContentsVersion() {
            return terminalContentsVersion;
        }

        @Override
        public int size() {
            return getActivePatternSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return isPatternSlotEnabled(slot) ? patternInventory.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public void setItemDirect(int slot, ItemStack stack) {
            if (isPatternSlotEnabled(slot)) {
                patternInventory.setItemDirect(slot, stack);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return patternInventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isPatternSlotEnabled(slot) && patternInventory.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isPatternSlotEnabled(slot) ? patternInventory.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isPatternSlotEnabled(slot) ? patternInventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    }

    private final class UpgradeInventory extends AppEngInternalInventory implements IUpgradeInventory {
        private final Item machineType;

        private UpgradeInventory(Item machineType, int slots) {
            super(ThermalMachinePatternProviderLogic.this, slots, 1);
            this.machineType = machineType;
            setFilter(new IAEItemFilter() {
                @Override
                public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
                    Item item = stack.getItem();
                    return getInstalledUpgrades(item) < getMaxInstalled(item);
                }

                @Override
                public boolean allowExtract(InternalInventory inventory, int slot, int amount) {
                    return canRemoveCard(inventory.getStackInSlot(slot), amount);
                }
            });
        }

        @Override
        public Item getUpgradableItem() {
            return machineType;
        }

        @Override
        public int getInstalledUpgrades(Item card) {
            int count = 0;
            for (ItemStack stack : this) {
                if (!stack.isEmpty() && stack.getItem() == card) {
                    count++;
                }
            }
            return Math.min(count, getMaxInstalled(card));
        }

        @Override
        public int getMaxInstalled(Item card) {
            return Upgrades.getMaxInstallable(card, machineType);
        }

        @Override
        public void setItemDirect(int slot, ItemStack stack) {
            ItemStack previous = getStackInSlot(slot);
            if (isExpansionCard(stack) || canRemoveCard(previous, previous.getCount())) {
                super.setItemDirect(slot, stack);
            }
        }

        private boolean canRemoveCard(ItemStack stack, int amount) {
            return !isExpansionCard(stack) || PatternProviderCapacity.canRemoveCapacityCards(
                getInstalledCapacityCards(), Math.min(amount, stack.getCount()), getExpansionLimit(),
                slot -> slot < patternInventory.size() && !patternInventory.getStackInSlot(slot).isEmpty());
        }

        private boolean isExpansionCard(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == AEItems.PATTERN_EXPANSION_CARD.item();
        }
    }
}
