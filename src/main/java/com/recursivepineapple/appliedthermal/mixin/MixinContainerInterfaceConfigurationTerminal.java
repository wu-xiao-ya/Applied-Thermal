package com.recursivepineapple.appliedthermal.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IMachineSet;
import appeng.container.implementations.ContainerInterfaceConfigurationTerminal;
import com.recursivepineapple.appliedthermal.integration.ae2uel.InterfaceTerminalNodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ContainerInterfaceConfigurationTerminal.class, remap = false)
public abstract class MixinContainerInterfaceConfigurationTerminal {

    @Redirect(
        method = {"detectAndSendChanges", "regenList"},
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/networking/IGrid;getMachines(Ljava/lang/Class;)"
                + "Lappeng/api/networking/IMachineSet;"))
    private IMachineSet appliedthermal$includeMachines(
        IGrid grid, Class<? extends IGridHost> machineClass) {
        return InterfaceTerminalNodes.collect(grid, machineClass);
    }
}
