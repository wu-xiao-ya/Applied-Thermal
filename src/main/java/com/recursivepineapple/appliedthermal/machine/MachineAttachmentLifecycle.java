package com.recursivepineapple.appliedthermal.machine;

import com.recursivepineapple.appliedthermal.attachment.MachineAttachment;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Owns attachment ordering for one modern Thermal machine block entity.
 */
public final class MachineAttachmentLifecycle {

    private final BlockEntity machine;
    private final List<MachineAttachment> attachments = new ArrayList<MachineAttachment>();
    private boolean loaded;

    public MachineAttachmentLifecycle(BlockEntity machine) {
        this.machine = machine;
    }

    public BlockEntity getMachine() {
        return machine;
    }

    public List<MachineAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    /**
     * Registers an attachment supplied by another integration layer.
     *
     * <p>Registration is intentionally explicit so the host does not depend on
     * an attachment implementation that may be supplied by another module.</p>
     */
    public void register(MachineAttachment attachment) {
        if (attachment == null || attachments.contains(attachment)) {
            return;
        }
        attachments.add(attachment);
        if (loaded) {
            attachment.onReady();
            syncAugments(machine);
        }
    }

    @Nullable
    public AppliedThermalProviderAttachment getProviderAttachment() {
        for (MachineAttachment attachment : attachments) {
            if (attachment instanceof AppliedThermalProviderAttachment provider) {
                return provider;
            }
        }
        return null;
    }

    public void readFromNBT(CompoundTag tag) {
        for (MachineAttachment attachment : attachments) {
            attachment.readFromNBT(tag);
        }
    }

    public void writeToNBT(CompoundTag tag) {
        for (MachineAttachment attachment : attachments) {
            attachment.writeToNBT(tag);
        }
    }

    public void onReady() {
        if (loaded) {
            return;
        }
        loaded = true;
        for (MachineAttachment attachment : attachments) {
            attachment.onReady();
        }
        syncAugments(machine);
    }

    public void invalidate() {
        for (MachineAttachment attachment : attachments) {
            if (attachment.isNodeRunning()) {
                attachment.stopNode();
            }
            attachment.invalidate();
        }
        loaded = false;
    }

    public void tickServer() {
        for (MachineAttachment attachment : attachments) {
            attachment.tickServer();
        }
    }

    public void ejectAndStop() {
        for (MachineAttachment attachment : attachments) {
            if (!attachment.isNodeRunning()) {
                continue;
            }
            attachment.ejectAndClear();
            attachment.stopNode();
        }
    }

    /**
     * Reconciles every registered attachment with the current Thermal augment
     * inventory. Removal is eject/clear before node shutdown.
     */
    public void syncAugments(BlockEntity host) {
        if (host.getLevel() == null || host.getLevel().isClientSide()) {
            return;
        }

        for (MachineAttachment attachment : attachments) {
            boolean installed = attachment.isAugmentInstalled(host);
            if (installed) {
                if (!attachment.isNodeRunning() && loaded) {
                    attachment.startNode();
                }
                if (attachment instanceof AppliedThermalProviderAttachment provider
                        && provider.getMainNode().isReady()) {
                    provider.refreshProviderState();
                }
                continue;
            }

            if (attachment.isNodeRunning()) {
                attachment.ejectAndClear();
                attachment.stopNode();
            }
        }
    }
}
