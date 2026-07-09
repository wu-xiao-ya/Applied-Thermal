package com.recursivepineapple.appliedthermal.container;

import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import javax.annotation.Nullable;

public interface AppliedThermalInterfaceContainer {

    @Nullable
    AppliedThermalMachine appliedthermal$getMachine();
}
