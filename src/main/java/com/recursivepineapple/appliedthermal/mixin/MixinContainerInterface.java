package com.recursivepineapple.appliedthermal.mixin;

import appeng.container.implementations.ContainerInterface;
import appeng.helpers.IInterfaceHost;
import com.recursivepineapple.appliedthermal.container.AppliedThermalInterfaceContainer;
import com.recursivepineapple.appliedthermal.container.SlotFluxInductionCard;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.network.ATNetwork;
import com.recursivepineapple.appliedthermal.network.MessageSyncSettings;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerInterface.class, remap = false)
public abstract class MixinContainerInterface extends Container implements AppliedThermalInterfaceContainer {

    @Unique
    @Nullable
    private AppliedThermalMachine appliedthermal$machine;
    @Unique
    private boolean appliedthermal$lastReturnOutputs;
    @Unique
    private boolean appliedthermal$sentSettings;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void appliedthermal$addFluxCardSlot(InventoryPlayer player, IInterfaceHost host, CallbackInfo ci) {
        if (host instanceof AppliedThermalMachine) {
            appliedthermal$machine = (AppliedThermalMachine) host;
            addSlotToContainer(new SlotFluxInductionCard(
                appliedthermal$machine.appliedthermal$getProviderAttachment().getFluxCardInventory(), 0, 187, 80));
        }
    }

    @Inject(method = "detectAndSendChanges", at = @At("RETURN"))
    private void appliedthermal$syncSettings(CallbackInfo ci) {
        if (appliedthermal$machine == null) {
            return;
        }
        boolean enabled =
            appliedthermal$machine.appliedthermal$getProviderAttachment().shouldReturnOutputsToNetwork();
        if (appliedthermal$sentSettings && enabled == appliedthermal$lastReturnOutputs) {
            return;
        }
        appliedthermal$sentSettings = true;
        appliedthermal$lastReturnOutputs = enabled;
        for (IContainerListener listener : listeners) {
            if (listener instanceof EntityPlayerMP) {
                ATNetwork.CHANNEL.sendTo(new MessageSyncSettings(
                    appliedthermal$machine.appliedthermal$getProviderAttachment().getTile().getPos(), enabled),
                    (EntityPlayerMP) listener);
            }
        }
    }

    @Nullable
    @Override
    public AppliedThermalMachine appliedthermal$getMachine() {
        return appliedthermal$machine;
    }
}
