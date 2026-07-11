package com.recursivepineapple.appliedthermal.menu;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.MenuTypeBuilder;
import com.recursivepineapple.appliedthermal.machine.MachineBlockEntityAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;

public final class ATMenus {

    public static final MenuType<AppliedThermalPatternProviderMenu> PATTERN_PROVIDER =
            MenuTypeBuilder.create(
                    (id, inventory, access) -> {
                        PatternProviderLogicHost host = access.appliedthermal$getProviderAttachment();
                        return new AppliedThermalPatternProviderMenu(id, inventory, host);
                    },
                    MachineBlockEntityAccess.class)
                    .withMenuTitle(access -> {
                        var provider = access.appliedthermal$getProviderAttachment();
                        return provider == null
                                ? Component.translatable("screen.appliedthermal.pattern_provider.title")
                                : provider.getMachine().getBlockState().getBlock().getName();
                    })
                    .build("appliedthermal_pattern_provider");

    private ATMenus() {
    }

    public static void init() {
    }
}
