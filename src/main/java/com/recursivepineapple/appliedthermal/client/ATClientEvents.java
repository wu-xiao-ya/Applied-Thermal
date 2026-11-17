package com.recursivepineapple.appliedthermal.client;

import ae2.api.client.PatternProviderGuiInitEvent;
import ae2.container.implementations.ContainerPatternProvider;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.client.gui.GuiOutputReturnButton;
import com.recursivepineapple.appliedthermal.init.ATItems;
import com.recursivepineapple.appliedthermal.integration.ae2.OutputReturnContainer;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = AppliedThermal.MOD_ID, value = Side.CLIENT)
public final class ATClientEvents {

    private ATClientEvents() {
    }

    @SubscribeEvent
    public static void registerItemModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
            ATItems.getPatternProviderAugment(),
            0,
            new ModelResourceLocation(ATItems.getPatternProviderAugment().getRegistryName(), "inventory"));
    }

    @SubscribeEvent
    public static void initializePatternProviderGui(PatternProviderGuiInitEvent event) {
        PatternProviderLogicHost host = event.getHost();
        if (!(host instanceof AppliedThermalMachine)) {
            return;
        }

        event.addToLeftToolbar(new GuiOutputReturnButton(
            () -> {
                OutputReturnContainer container = getOutputReturnContainer(host);
                return container == null || container.appliedthermal$isOutputReturnEnabled();
            },
            () -> {
                OutputReturnContainer container = getOutputReturnContainer(host);
                if (container != null) {
                    container.appliedthermal$toggleOutputReturn();
                }
            }));
    }

    private static OutputReturnContainer getOutputReturnContainer(PatternProviderLogicHost host) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        Container container = player == null ? null : player.openContainer;
        if (container instanceof ContainerPatternProvider && container instanceof OutputReturnContainer
            && ((ContainerPatternProvider) container).getHost() == host) {
            return (OutputReturnContainer) container;
        }
        return null;
    }
}
