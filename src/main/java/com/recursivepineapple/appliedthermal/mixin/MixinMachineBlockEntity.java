package com.recursivepineapple.appliedthermal.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import com.recursivepineapple.appliedthermal.machine.MachineAttachmentLifecycle;
import com.recursivepineapple.appliedthermal.machine.MachineBlockEntityAccess;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hosts the attachment lifecycle on the modern Thermal machine base class.
 */
@Pseudo
@Mixin(targets = "cofh.thermal.lib.common.block.entity.MachineBlockEntity", remap = false)
public abstract class MixinMachineBlockEntity implements MachineBlockEntityAccess, IInWorldGridNodeHost, IActionHost {

    @Unique
    private final MachineAttachmentLifecycle appliedthermal$attachmentLifecycle =
        appliedthermal$createLifecycle();

    @Unique
    private LazyOptional<IInWorldGridNodeHost> appliedthermal$gridHostCapability =
        LazyOptional.of(() -> (IInWorldGridNodeHost) (Object) this);

    @Unique
    private MachineAttachmentLifecycle appliedthermal$createLifecycle() {
        BlockEntity machine = (BlockEntity) (Object) this;
        MachineAttachmentLifecycle lifecycle = new MachineAttachmentLifecycle(machine);
        lifecycle.register(new AppliedThermalProviderAttachment(machine));
        return lifecycle;
    }

    @Override
    public MachineAttachmentLifecycle appliedthermal$getAttachmentLifecycle() {
        return appliedthermal$attachmentLifecycle;
    }

    @Override
    @Nullable
    public AppliedThermalProviderAttachment appliedthermal$getProviderAttachment() {
        return appliedthermal$attachmentLifecycle.getProviderAttachment();
    }

    @Override
    public LazyOptional<IInWorldGridNodeHost> appliedthermal$getGridHostCapability() {
        return appliedthermal$gridHostCapability;
    }

    @Override
    public void appliedthermal$invalidateHost() {
        appliedthermal$gridHostCapability.invalidate();
        appliedthermal$attachmentLifecycle.invalidate();
    }

    @Override
    @Nullable
    public IGridNode getGridNode(Direction direction) {
        AppliedThermalProviderAttachment provider = appliedthermal$getProviderAttachment();
        return provider != null ? provider.getGridNode(direction) : null;
    }

    @Override
    public AECableType getCableConnectionType(Direction direction) {
        AppliedThermalProviderAttachment provider = appliedthermal$getProviderAttachment();
        return provider != null ? provider.getCableConnectionType(direction) : AECableType.NONE;
    }

    @Override
    @Nullable
    public IGridNode getActionableNode() {
        AppliedThermalProviderAttachment provider = appliedthermal$getProviderAttachment();
        return provider != null ? provider.getActionableNode() : null;
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void appliedthermal$readAttachments(CompoundTag tag, CallbackInfo ci) {
        appliedthermal$attachmentLifecycle.readFromNBT(tag);
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void appliedthermal$writeAttachments(CompoundTag tag, CallbackInfo ci) {
        appliedthermal$attachmentLifecycle.writeToNBT(tag);
    }

    @Unique
    private void appliedthermal$ensureReady() {
        if (!appliedthermal$gridHostCapability.isPresent()) {
            appliedthermal$gridHostCapability =
                LazyOptional.of(() -> (IInWorldGridNodeHost) (Object) this);
        }
        appliedthermal$attachmentLifecycle.onReady();
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void appliedthermal$tickServer(CallbackInfo ci) {
        appliedthermal$ensureReady();
        appliedthermal$attachmentLifecycle.tickServer();
    }
}
