package com.recursivepineapple.appliedthermal.mixin;

import ae2.api.config.YesNo;
import ae2.container.AEBaseContainer;
import ae2.container.guisync.GuiSync;
import ae2.container.implementations.ContainerPatternProvider;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import cofh.thermalexpansion.block.machine.TileMachineBase;
import com.recursivepineapple.appliedthermal.integration.ae2.OutputReturnContainer;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerPatternProvider.class, remap = false)
public abstract class MixinContainerPatternProvider extends AEBaseContainer implements OutputReturnContainer {

    @Unique
    private static final String APPLIEDTHERMAL_SET_OUTPUT_RETURN = "appliedthermal:set_output_return";

    @Shadow @Final private PatternProviderLogicHost host;
    @Shadow @Final private PatternProviderLogic logic;

    @Unique
    @GuiSync(17001)
    private YesNo appliedthermal$outputReturn = YesNo.UNDECIDED;

    protected MixinContainerPatternProvider(InventoryPlayer inventory, Object host) {
        super(inventory, host);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void appliedthermal$initOutputReturn(InventoryPlayer inventory, PatternProviderLogicHost host,
                                                 CallbackInfo ci) {
        if (!(host instanceof AppliedThermalMachine)) {
            return;
        }
        registerClientAction(APPLIEDTHERMAL_SET_OUTPUT_RETURN, Boolean.class, this::appliedthermal$setOutputReturn);
        appliedthermal$refreshOutputReturn();
    }

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void appliedthermal$updateOutputReturn(CallbackInfo ci) {
        appliedthermal$refreshOutputReturn();
    }

    @Override
    public boolean appliedthermal$isOutputReturnEnabled() {
        return appliedthermal$outputReturn != YesNo.NO;
    }

    @Override
    public void appliedthermal$toggleOutputReturn() {
        if (isClientSide() && getPlayer().openContainer == this && appliedthermal$outputReturn != YesNo.UNDECIDED) {
            sendClientAction(APPLIEDTHERMAL_SET_OUTPUT_RETURN, appliedthermal$outputReturn == YesNo.NO);
        }
    }

    @Unique
    private void appliedthermal$setOutputReturn(Boolean enabled) {
        EntityPlayer player = getPlayer();
        if (enabled == null || !isServerSide() || player.openContainer != this || !canInteractWith(player)) {
            return;
        }
        AppliedThermalProviderAttachment attachment = appliedthermal$getOutputReturnHost();
        if (attachment != null && ((TileMachineBase) attachment.getTileEntity()).canPlayerAccess(player)) {
            attachment.setReturnOutputsToNetwork(enabled);
        }
    }

    @Unique
    private void appliedthermal$refreshOutputReturn() {
        if (!isServerSide() || !(host instanceof AppliedThermalMachine)) {
            return;
        }
        AppliedThermalProviderAttachment attachment = appliedthermal$getOutputReturnHost();
        appliedthermal$outputReturn = attachment == null ? YesNo.UNDECIDED
            : attachment.shouldReturnOutputsToNetwork() ? YesNo.YES : YesNo.NO;
    }

    @Unique
    @Nullable
    private AppliedThermalProviderAttachment appliedthermal$getOutputReturnHost() {
        if (!(host instanceof AppliedThermalMachine)) {
            return null;
        }
        TileEntity tile = host.getTileEntity();
        World world = tile.getWorld();
        if (!(tile instanceof TileMachineBase) || tile.isInvalid() || world == null || !world.isBlockLoaded(tile.getPos())
            || world.getTileEntity(tile.getPos()) != tile) {
            return null;
        }
        AppliedThermalMachine machine = (AppliedThermalMachine) host;
        AppliedThermalProviderAttachment attachment = machine.appliedthermal$getProviderAttachment();
        return machine.appliedthermal$hasPatternProviderAugment() && attachment.isEnabled()
            && attachment.getLogic() == logic ? attachment : null;
    }
}
