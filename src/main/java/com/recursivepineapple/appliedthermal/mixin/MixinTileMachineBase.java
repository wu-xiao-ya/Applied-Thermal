package com.recursivepineapple.appliedthermal.mixin;

import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IConfigManager;
import appeng.core.sync.GuiBridge;
import appeng.helpers.DualityInterface;
import appeng.helpers.ICustomNameObject;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.IPriorityHost;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.inv.IInventoryDestination;
import cofh.api.core.IAugmentable;
import cofh.core.util.helpers.AugmentHelper;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.google.common.collect.ImmutableSet;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.integration.thermal.ThermalMachineAugments;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import java.util.EnumSet;
import javax.annotation.Nonnull;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileMachineBase.class, remap = false)
public abstract class MixinTileMachineBase implements AppliedThermalMachine, IInterfaceHost, IGridProxyable,
    IActionHost, IGridTickable, IInventoryDestination, IPriorityHost, ICustomNameObject {

    @Unique
    private final AppliedThermalProviderAttachment appliedthermal$provider =
        new AppliedThermalProviderAttachment((TileMachineBase) (Object) this);

    @Override
    public AppliedThermalProviderAttachment appliedthermal$getProviderAttachment() {
        return appliedthermal$provider;
    }

    @Override
    public boolean appliedthermal$hasPatternProviderAugment() {
        ItemStack[] augments = ((IAugmentable) this).getAugmentSlots();
        if (augments == null) {
            return false;
        }
        for (ItemStack augment : augments) {
            if (augment != null && !augment.isEmpty()
                && ThermalMachineAugments.PATTERN_PROVIDER_AUGMENT.equals(AugmentHelper.getAugmentIdentifier(augment))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DualityInterface getInterfaceDuality() {
        return appliedthermal$provider.getDuality();
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        return EnumSet.allOf(EnumFacing.class);
    }

    @Override
    public TileEntity getTileEntity() {
        return (TileEntity) (Object) this;
    }

    @Override
    public void saveChanges() {
        appliedthermal$provider.saveChanges();
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        return appliedthermal$provider.getDuality().pushPattern(patternDetails, table);
    }

    @Override
    public boolean isBusy() {
        return appliedthermal$provider.getDuality().isBusy();
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        appliedthermal$provider.getDuality().provideCrafting(craftingTracker);
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrade) {
        return appliedthermal$provider.getDuality().getInstalledUpgrades(upgrade);
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return appliedthermal$provider.getDuality().getRequestedJobs();
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack items, Actionable mode) {
        return appliedthermal$provider.getDuality().injectCraftedItems(link, items, mode);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        appliedthermal$provider.getDuality().jobStateChange(link);
    }

    @Override
    public IConfigManager getConfigManager() {
        return appliedthermal$provider.getDuality().getConfigManager();
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        return appliedthermal$provider.getDuality().getInventoryByName(name);
    }

    @Override
    public TileEntity getTile() {
        return (TileEntity) (Object) this;
    }

    @Override
    public AENetworkProxy getProxy() {
        return appliedthermal$provider.getProxy();
    }

    @Override
    public DimensionalCoord getLocation() {
        return appliedthermal$provider.getLocation();
    }

    @Override
    public void gridChanged() {
        appliedthermal$provider.getDuality().gridChanged();
    }

    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation side) {
        return appliedthermal$provider.getGridNode(side);
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation side) {
        return appliedthermal$provider.getCableConnectionType(side);
    }

    @Override
    public void securityBreak() {
        TileMachineBase tile = (TileMachineBase) (Object) this;
        if (tile.getWorld() != null && !tile.getWorld().isRemote) {
            tile.getWorld().destroyBlock(tile.getPos(), true);
        }
    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        return appliedthermal$provider.getActionableNode();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return appliedthermal$provider.getDuality().getTickingRequest(node);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return appliedthermal$provider.getDuality().tickingRequest(node, ticksSinceLastCall);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return appliedthermal$provider.getDuality().canInsert(stack);
    }

    @Override
    public int getPriority() {
        return appliedthermal$provider.getDuality().getPriority();
    }

    @Override
    public void setPriority(int priority) {
        appliedthermal$provider.getDuality().setPriority(priority);
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return appliedthermal$provider.getMachineRepresentation();
    }

    @Override
    public GuiBridge getGuiBridge() {
        return GuiBridge.GUI_INTERFACE;
    }

    @Override
    public String getCustomInventoryName() {
        return appliedthermal$provider.getCustomInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return appliedthermal$provider.hasCustomInventoryName();
    }

    @Override
    public void setCustomName(String name) {
        appliedthermal$provider.setCustomName(name);
    }

    @Inject(method = {"readFromNBT", "func_145839_a"}, at = @At("RETURN"))
    private void appliedthermal$readProvider(NBTTagCompound nbt, CallbackInfo ci) {
        appliedthermal$provider.readFromNBT(nbt);
        appliedthermal$provider.setEnabled(appliedthermal$hasPatternProviderAugment());
    }

    @Inject(method = {"writeToNBT", "func_189515_b"}, at = @At("RETURN"))
    private void appliedthermal$writeProvider(NBTTagCompound nbt, CallbackInfoReturnable<NBTTagCompound> cir) {
        appliedthermal$provider.writeToNBT(cir.getReturnValue());
    }

    @Inject(method = "postAugmentInstall", at = @At("RETURN"))
    private void appliedthermal$syncProviderAfterAugments(CallbackInfo ci) {
        boolean hasProviderAugment = appliedthermal$hasPatternProviderAugment();
        if (appliedthermal$provider.isEnabled() && !hasProviderAugment) {
            appliedthermal$provider.ejectContentsOnAugmentRemoval();
        }
        appliedthermal$provider.setEnabled(hasProviderAugment);
        if (hasProviderAugment) {
            appliedthermal$provider.onReady();
        }
    }

    @Inject(method = {"update", "func_73660_a"}, at = @At("RETURN"))
    private void appliedthermal$tickProvider(CallbackInfo ci) {
        appliedthermal$provider.tickServer();
    }
}
