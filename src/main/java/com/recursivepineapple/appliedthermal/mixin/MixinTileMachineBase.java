package com.recursivepineapple.appliedthermal.mixin;

import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.inventories.InternalInventory;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IInWorldGridNodeHost;
import ae2.api.networking.security.IActionHost;
import ae2.api.stacks.AEItemKey;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.util.AECableType;
import ae2.helpers.patternprovider.PatternContainer;
import cofh.api.core.IAugmentable;
import cofh.core.util.helpers.AugmentHelper;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalFluxAttachment;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.integration.thermal.ThermalMachineAugments;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileMachineBase.class, remap = false)
public abstract class MixinTileMachineBase implements AppliedThermalMachine, PatternContainer, IActionHost,
    IInWorldGridNodeHost, IUpgradeableObject {

    @Unique
    private final AppliedThermalProviderAttachment appliedthermal$provider =
        new AppliedThermalProviderAttachment((TileMachineBase) (Object) this);

    @Unique
    private final AppliedThermalFluxAttachment appliedthermal$flux =
        new AppliedThermalFluxAttachment((TileMachineBase) (Object) this);

    @Override
    public AppliedThermalProviderAttachment appliedthermal$getProviderAttachment() {
        return appliedthermal$provider;
    }

    @Override
    public AppliedThermalFluxAttachment appliedthermal$getFluxAttachment() {
        return appliedthermal$flux;
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
    public boolean appliedthermal$hasFluxInductionSupport() {
        return appliedthermal$hasPatternProviderAugment() && appliedthermal$flux.hasFluxInductionSupport();
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return appliedthermal$provider.getLogic().getTerminalPatternInv();
    }

    @Override
    public boolean containsPattern(AEItemKey pattern) {
        return appliedthermal$provider.getLogic().containsPattern(pattern);
    }

    @Override
    public boolean isVisibleInTerminal() {
        return appliedthermal$provider.isEnabled()
            && appliedthermal$provider.getLogic().getConfigManager().getSetting(Settings.PATTERN_ACCESS_TERMINAL)
                == YesNo.YES;
    }

    @Override
    public long getTerminalSortOrder() {
        return appliedthermal$provider.getTileEntity().getPos().toLong();
    }

    @Override
    public boolean canEditTerminalName() {
        return true;
    }

    @Override
    public void setTerminalCustomName(@Nullable String name) {
        appliedthermal$provider.setCustomName(name);
    }

    @Override
    public boolean canModifyTerminalVisibility() {
        return true;
    }

    @Override
    public void setTerminalVisibility(boolean visible) {
        appliedthermal$provider.getLogic().getConfigManager().putSetting(Settings.PATTERN_ACCESS_TERMINAL,
            visible ? YesNo.YES : YesNo.NO);
    }

    @Override
    public void openTerminalPatternContainerGui(EntityPlayer player) {
        appliedthermal$provider.openGui(player);
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        return appliedthermal$provider.getTerminalGroup();
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return appliedthermal$provider.getUpgrades();
    }

    @Override
    @Nullable
    public IGrid getGrid() {
        return appliedthermal$provider.getGrid();
    }

    @Override
    @Nullable
    public IGridNode getActionableNode() {
        return appliedthermal$provider.getActionableNode();
    }

    @Override
    @Nullable
    public IGridNode getGridNode(EnumFacing dir) {
        return appliedthermal$provider.getGridNode(dir);
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return appliedthermal$provider.getCableConnectionType(dir);
    }

    @Inject(method = "func_145839_a", at = @At("RETURN"))
    private void appliedthermal$readProvider(NBTTagCompound nbt, CallbackInfo ci) {
        appliedthermal$provider.readFromNBT(nbt);
        appliedthermal$flux.readFromNBT(nbt);
        appliedthermal$provider.setEnabled(appliedthermal$hasPatternProviderAugment());
    }

    @Inject(method = "func_189515_b", at = @At("RETURN"))
    private void appliedthermal$writeProvider(NBTTagCompound nbt, CallbackInfoReturnable<NBTTagCompound> cir) {
        appliedthermal$provider.writeToNBT(cir.getReturnValue());
        appliedthermal$flux.writeToNBT(cir.getReturnValue());
    }

    @Inject(method = "postAugmentInstall", at = @At("RETURN"))
    private void appliedthermal$syncProviderAfterAugments(CallbackInfo ci) {
        boolean hasProviderAugment = appliedthermal$hasPatternProviderAugment();
        if (appliedthermal$provider.isEnabled() && !hasProviderAugment) {
            appliedthermal$provider.ejectContentsOnAugmentRemoval();
        }
        appliedthermal$provider.setEnabled(hasProviderAugment);
        if (appliedthermal$provider.isEnabled()) {
            appliedthermal$provider.onReady();
        }
    }

    @Inject(method = "func_73660_a", at = @At("RETURN"))
    private void appliedthermal$returnOutputsToNetwork(CallbackInfo ci) {
        appliedthermal$provider.tryReturnOutputsToNetwork();
        appliedthermal$provider.tryChargeMachineFromFluxNetwork();
    }

}
