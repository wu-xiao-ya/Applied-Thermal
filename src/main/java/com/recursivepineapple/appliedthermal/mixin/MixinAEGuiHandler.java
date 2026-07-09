package com.recursivepineapple.appliedthermal.mixin;

import ae2.client.gui.implementations.GuiPatternProvider;
import ae2.client.gui.style.GuiStyleManager;
import ae2.container.GuiIds;
import ae2.container.implementations.ContainerPatternProvider;
import ae2.core.gui.AEGuiHandler;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AEGuiHandler.class, remap = false)
public abstract class MixinAEGuiHandler {

    @Inject(method = "getServerGuiElement", at = @At("HEAD"), cancellable = true)
    private void appliedthermal$getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z,
                                                    CallbackInfoReturnable<Object> cir) {
        PatternProviderLogicHost host = appliedthermal$getProviderHost(id, world, x, y, z);
        if (host != null) {
            cir.setReturnValue(new ContainerPatternProvider(player.inventory, host));
        }
    }

    @Inject(method = "getClientGuiElement", at = @At("HEAD"), cancellable = true)
    private void appliedthermal$getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z,
                                                    CallbackInfoReturnable<Object> cir) {
        PatternProviderLogicHost host = appliedthermal$getProviderHost(id, world, x, y, z);
        if (host != null) {
            ContainerPatternProvider container = new ContainerPatternProvider(player.inventory, host);
            cir.setReturnValue(new GuiPatternProvider(container, player.inventory, null,
                GuiStyleManager.loadStyleDoc("/screens/pattern_provider.json")));
        }
    }

    private static PatternProviderLogicHost appliedthermal$getProviderHost(int id, World world, int x, int y, int z) {
        if (GuiIds.GuiKey.fromId(id) != GuiIds.GuiKey.PATTERN_PROVIDER) {
            return null;
        }
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!appliedthermal$isEnabledThermalProvider(tile)) {
            return null;
        }
        if (tile instanceof PatternProviderLogicHost) {
            return (PatternProviderLogicHost) tile;
        }
        return null;
    }

    private static boolean appliedthermal$isEnabledThermalProvider(TileEntity tile) {
        if (tile == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(tile.getClass()
                .getMethod("appliedthermal$hasPatternProviderAugment")
                .invoke(tile));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
