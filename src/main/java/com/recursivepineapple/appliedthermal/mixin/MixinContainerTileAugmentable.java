package com.recursivepineapple.appliedthermal.mixin;

import cofh.core.block.TileCore;
import cofh.core.gui.container.ContainerTileAugmentable;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.network.ATNetwork;
import com.recursivepineapple.appliedthermal.network.MessageSyncSettings;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerTileAugmentable.class, remap = false)
public abstract class MixinContainerTileAugmentable extends Container {

    @Shadow
    @Final
    protected TileCore baseTile;

    @Unique
    private boolean appliedthermal$lastReturnOutputsToNetwork;
    @Unique
    private boolean appliedthermal$sentSettings;

    @Inject(method = {"detectAndSendChanges", "func_75142_b"}, at = @At("RETURN"))
    private void appliedthermal$syncProviderSettings(CallbackInfo ci) {
        if (!(baseTile instanceof TileMachineBase) || !(baseTile instanceof AppliedThermalMachine)) {
            return;
        }
        AppliedThermalMachine machine = (AppliedThermalMachine) baseTile;
        if (!machine.appliedthermal$hasPatternProviderAugment()) {
            return;
        }
        boolean returnOutputs = machine.appliedthermal$getProviderAttachment().shouldReturnOutputsToNetwork();
        if (appliedthermal$sentSettings && appliedthermal$lastReturnOutputsToNetwork == returnOutputs) {
            return;
        }
        appliedthermal$sentSettings = true;
        appliedthermal$lastReturnOutputsToNetwork = returnOutputs;
        for (IContainerListener listener : this.listeners) {
            if (listener instanceof EntityPlayerMP) {
                ATNetwork.CHANNEL.sendTo(new MessageSyncSettings(baseTile.getPos(), returnOutputs),
                    (EntityPlayerMP) listener);
            }
        }
    }
}
