package com.recursivepineapple.appliedthermal.init;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import appeng.api.AEApi;
import com.recursivepineapple.appliedthermal.integration.fluxapplied.FluxAppliedCompat;
import com.recursivepineapple.appliedthermal.item.ItemFluxInductionCard;
import com.recursivepineapple.appliedthermal.item.ItemPatternProviderAugment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapelessOreRecipe;

@Mod.EventBusSubscriber(modid = AppliedThermal.MOD_ID)
public final class ATItems {

    public static ItemPatternProviderAugment patternProviderAugment;
    public static ItemFluxInductionCard fluxInductionCard;

    private ATItems() {
    }

    public static void preInit() {
        patternProviderAugment = new ItemPatternProviderAugment();
        ForgeRegistries.ITEMS.register(patternProviderAugment.setRegistryName(AppliedThermal.MOD_ID, "pattern_provider_augment"));
        fluxInductionCard = new ItemFluxInductionCard();
        ForgeRegistries.ITEMS.register(fluxInductionCard.setRegistryName(AppliedThermal.MOD_ID, "flux_induction_card"));
    }

    public static Item getPatternProviderAugment() {
        return patternProviderAugment;
    }

    public static Item getFluxInductionCard() {
        return fluxInductionCard;
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!FluxAppliedCompat.isLoaded()) {
            return;
        }
        Item energyPort = ForgeRegistries.ITEMS.getValue(new ResourceLocation("flux_applied", "energy_port"));
        ItemStack basicCard = AEApi.instance().definitions().materials().basicCard().maybeStack(1)
            .orElse(ItemStack.EMPTY);
        if (energyPort == null || basicCard.isEmpty()) {
            AppliedThermal.LOG.error("Flux induction card recipe was not registered because a dependency item is missing.");
            return;
        }
        ShapelessOreRecipe recipe = new ShapelessOreRecipe(
            new ResourceLocation(AppliedThermal.MOD_ID, "flux_induction_card"),
            new ItemStack(fluxInductionCard),
            new ItemStack(energyPort),
            basicCard);
        recipe.setRegistryName(AppliedThermal.MOD_ID, "flux_induction_card");
        event.getRegistry().register(recipe);
    }
}
