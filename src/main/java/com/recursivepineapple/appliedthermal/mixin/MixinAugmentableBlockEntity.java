package com.recursivepineapple.appliedthermal.mixin;

import appeng.capabilities.Capabilities;
import cofh.thermal.core.common.config.ThermalCoreConfig;
import com.recursivepineapple.appliedthermal.machine.MachineAttachmentLifecycle;
import com.recursivepineapple.appliedthermal.machine.MachineBlockEntityAccess;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static cofh.lib.util.constants.NBTTags.TAG_BLOCK_ENTITY;

/**
 * Reconciles registered attachments after Thermal's augment inventory changes.
 */
@Pseudo
@Mixin(targets = "cofh.thermal.lib.common.block.entity.AugmentableBlockEntity", remap = false)
public abstract class MixinAugmentableBlockEntity {

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true)
    private <T> void appliedthermal$getGridHostCapability(Capability<T> capability,
            @Nullable Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (capability != Capabilities.IN_WORLD_GRID_NODE_HOST
                || !((Object) this instanceof MachineBlockEntityAccess access)) {
            return;
        }
        cir.setReturnValue(access.appliedthermal$getGridHostCapability().cast());
    }

    @Inject(method = "createItemStackTag", at = @At("RETURN"))
    private void appliedthermal$writeAttachmentToDrop(ItemStack original,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof BlockEntity host)
                || !((Object) this instanceof MachineBlockEntityAccess access)
                || !ThermalCoreConfig.keepAugments.get()) {
            return;
        }
        AppliedThermalProviderAttachment provider = access.appliedthermal$getProviderAttachment();
        if (provider == null || !provider.isAugmentInstalled(host)) {
            return;
        }
        ItemStack result = cir.getReturnValue();
        CompoundTag blockEntityTag = result.getOrCreateTagElement(TAG_BLOCK_ENTITY);
        access.appliedthermal$getAttachmentLifecycle().writeToNBT(blockEntityTag);
    }

    @Inject(method = "onReplaced", at = @At("HEAD"))
    private void appliedthermal$ejectAttachmentWhenAugmentsAreNotKept(BlockState state,
            Level level, net.minecraft.core.BlockPos pos, BlockState newState, CallbackInfo ci) {
        if (level.isClientSide() || ThermalCoreConfig.keepAugments.get()
                || !((Object) this instanceof MachineBlockEntityAccess access)) {
            return;
        }
        access.appliedthermal$getAttachmentLifecycle().ejectAndStop();
    }

    @Inject(method = "onInventoryChanged", at = @At("RETURN"))
    private void appliedthermal$syncAttachments(int slot, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof BlockEntity) || !(self instanceof MachineBlockEntityAccess)) {
            return;
        }

        BlockEntity host = (BlockEntity) self;
        if (host.getLevel() == null || host.getLevel().isClientSide()) {
            return;
        }

        MachineAttachmentLifecycle lifecycle =
            ((MachineBlockEntityAccess) self).appliedthermal$getAttachmentLifecycle();
        lifecycle.syncAugments(host);
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void appliedthermal$setRemoved(CallbackInfo ci) {
        if ((Object) this instanceof MachineBlockEntityAccess access) {
            access.appliedthermal$invalidateHost();
        }
    }
}
