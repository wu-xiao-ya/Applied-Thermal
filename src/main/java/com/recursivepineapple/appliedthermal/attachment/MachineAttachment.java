package com.recursivepineapple.appliedthermal.attachment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Lifecycle contract for one attachment owned by a Thermal machine.
 *
 * <p>The attachment supplies its own augment detector and node implementation.
 * The machine layer only coordinates ordering and persistence.</p>
 */
public interface MachineAttachment {

    boolean isAugmentInstalled(BlockEntity host);

    boolean isNodeRunning();

    void startNode();

    void stopNode();

    void onReady();

    void invalidate();

    void tickServer();

    void readFromNBT(CompoundTag tag);

    void writeToNBT(CompoundTag tag);

    /**
     * Ejects and clears attachment-owned contents before its node is stopped.
     */
    void ejectAndClear();
}
