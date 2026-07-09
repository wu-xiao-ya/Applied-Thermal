package com.recursivepineapple.appliedthermal.mixin;

import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IInWorldGridNodeHost;
import ae2.api.networking.security.IActionHost;
import ae2.api.stacks.AEItemKey;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.util.AECableType;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import cofh.api.core.IAugmentable;
import cofh.core.util.helpers.AugmentHelper;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalFluxAttachment;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.integration.thermal.ThermalMachineAugments;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileMachineBase.class, remap = false)
public abstract class MixinTileMachineBase implements AppliedThermalMachine, PatternProviderLogicHost, IActionHost,
    IInWorldGridNodeHost {

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
    public PatternProviderLogic getLogic() {
        return appliedthermal$provider.getLogic();
    }

    @Override
    public TileEntity getTileEntity() {
        return (TileEntity) (Object) this;
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        return appliedthermal$provider.getTargets();
    }

    @Override
    public boolean hasCustomName() {
        return appliedthermal$provider.hasCustomName();
    }

    @Override
    @Nullable
    public String getCustomName() {
        return appliedthermal$provider.getCustomName();
    }

    @Override
    public void setCustomName(@Nullable String name) {
        appliedthermal$provider.setCustomName(name);
    }

    @Override
    public void saveChanges() {
        appliedthermal$provider.saveChanges();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return appliedthermal$provider.getTerminalIcon();
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return appliedthermal$provider.getMainContainerIcon();
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
