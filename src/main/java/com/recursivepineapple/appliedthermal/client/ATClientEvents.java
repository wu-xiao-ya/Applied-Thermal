package com.recursivepineapple.appliedthermal.client;

import ae2.api.client.PatternProviderGuiInitEvent;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.client.gui.GuiOutputReturnButton;
import com.recursivepineapple.appliedthermal.init.ATItems;
import com.recursivepineapple.appliedthermal.integration.thermal.AppliedThermalMachine;
import com.recursivepineapple.appliedthermal.network.ATNetwork;
import com.recursivepineapple.appliedthermal.network.MessageToggleOutputReturn;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
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
        if (!(event.getHost() instanceof AppliedThermalMachine)) {
            return;
        }

        AppliedThermalProviderAttachment attachment =
            ((AppliedThermalMachine) event.getHost()).appliedthermal$getProviderAttachment();
        event.addToLeftToolbar(new GuiOutputReturnButton(
            attachment::shouldReturnOutputsToNetwork,
            () -> toggleOutputReturn(attachment)));
    }

    private static void toggleOutputReturn(AppliedThermalProviderAttachment attachment) {
        boolean next = !attachment.shouldReturnOutputsToNetwork();
        attachment.setReturnOutputsToNetwork(next);
        ATNetwork.CHANNEL.sendToServer(new MessageToggleOutputReturn(
            attachment.getTileEntity().getPos(), next));
    }
}
