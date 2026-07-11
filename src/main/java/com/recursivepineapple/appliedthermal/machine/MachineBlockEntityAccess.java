package com.recursivepineapple.appliedthermal.machine;

import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import appeng.api.networking.IInWorldGridNodeHost;
import javax.annotation.Nullable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Access surface injected into supported Thermal MachineBlockEntity instances.
 */
public interface MachineBlockEntityAccess {

    MachineAttachmentLifecycle appliedthermal$getAttachmentLifecycle();

    @Nullable
    AppliedThermalProviderAttachment appliedthermal$getProviderAttachment();

    LazyOptional<IInWorldGridNodeHost> appliedthermal$getGridHostCapability();

    void appliedthermal$invalidateHost();
}
