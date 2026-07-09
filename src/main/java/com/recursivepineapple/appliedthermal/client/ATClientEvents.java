package com.recursivepineapple.appliedthermal.client;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.init.ATItems;
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
        ModelLoader.setCustomModelResourceLocation(
            ATItems.getFluxInductionCard(),
            0,
            new ModelResourceLocation(ATItems.getFluxInductionCard().getRegistryName(), "inventory"));
    }
}
