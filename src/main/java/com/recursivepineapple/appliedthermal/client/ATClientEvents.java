package com.recursivepineapple.appliedthermal.client;

import appeng.init.client.InitScreens;
import cofh.thermal.lib.client.gui.MachineScreen;
import cofh.thermal.lib.common.block.entity.MachineBlockEntity;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.client.gui.AppliedThermalPatternProviderScreen;
import com.recursivepineapple.appliedthermal.client.gui.ThermalPatternProviderButton;
import com.recursivepineapple.appliedthermal.init.ATItems;
import com.recursivepineapple.appliedthermal.menu.ATMenus;
import com.recursivepineapple.appliedthermal.network.ATNetwork;
import java.lang.reflect.Field;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ATClientEvents {

    private static final Field MACHINE_TILE = findMachineTileField();

    private ATClientEvents() {
    }

    @Mod.EventBusSubscriber(modid = AppliedThermal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> InitScreens.register(
                    ATMenus.PATTERN_PROVIDER,
                    AppliedThermalPatternProviderScreen::new,
                    "/screens/appliedthermal_pattern_provider.json"));
        }
    }

    @Mod.EventBusSubscriber(modid = AppliedThermal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
            value = Dist.CLIENT)
    public static final class ForgeBus {

        private ForgeBus() {
        }

        @SubscribeEvent
        public static void addMachineButton(ScreenEvent.Init.Post event) {
            Screen screen = event.getScreen();
            if (!(screen instanceof MachineScreen<?> machineScreen)) {
                return;
            }
            BlockEntity tile = getMachineTile(machineScreen);
            if (!(tile instanceof MachineBlockEntity)) {
                return;
            }

            var menu = machineScreen.getMenu();
            var button = new ThermalPatternProviderButton(
                    machineScreen.guiLeft() + 178,
                    machineScreen.guiTop() + 30,
                    () -> menu.getAugmentSlots().stream()
                            .anyMatch(slot -> slot.getItem().is(ATItems.getPatternProviderAugment())),
                    ignored -> ATNetwork.openPatternProvider(tile.getBlockPos()));
            event.addListener(button);
        }
    }

    private static Field findMachineTileField() {
        try {
            Field field = MachineScreen.class.getDeclaredField("tile");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access Thermal machine screen host", ex);
        }
    }

    private static BlockEntity getMachineTile(MachineScreen<?> screen) {
        try {
            return (BlockEntity) MACHINE_TILE.get(screen);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Unable to read Thermal machine screen host", ex);
        }
    }
}
