package com.recursivepineapple.appliedthermal.integration.appflux;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.attachment.MachineAttachment;
import com.recursivepineapple.appliedthermal.machine.MachineAttachmentLifecycle;
import com.recursivepineapple.appliedthermal.machine.MachineBlockEntityAccess;
import com.recursivepineapple.appliedthermal.provider.AppliedThermalProviderAttachment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class AppFluxCompat {

    private static final AtomicReference<AppFluxBridge> BRIDGE = new AtomicReference<AppFluxBridge>();
    private static final Set<BlockEntity> ACTIVE_MACHINES = Collections.newSetFromMap(new WeakHashMap<BlockEntity, Boolean>());
    private static volatile boolean bootstrapped;

    private AppFluxCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("appflux");
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        if (!isLoaded()) {
            bootstrapped = true;
            BRIDGE.compareAndSet(null, AppFluxNoopBridge.INSTANCE);
            return;
        }

        registerSupportedMachineIcons(getBridge());
        bootstrapped = true;
    }

    public static long tick(ServerLevel level) {
        if (!isLoaded() || level == null || level.isClientSide()) {
            return 0L;
        }

        List<BlockEntity> machines;
        synchronized (ACTIVE_MACHINES) {
            machines = new ArrayList<BlockEntity>(ACTIVE_MACHINES);
        }

        long moved = 0L;
        for (BlockEntity machine : machines) {
            if (machine == null || machine.getLevel() != level) {
                continue;
            }

            if (machine.isRemoved() || !machine.hasLevel() || !level.hasChunkAt(machine.getBlockPos())) {
                unregisterMachine(machine);
                continue;
            }

            moved += tickMachine(machine);
        }
        return moved;
    }

    public static void registerMachine(BlockEntity machine) {
        if (machine == null || !isLoaded()) {
            return;
        }

        if (!(machine instanceof MachineBlockEntityAccess)) {
            return;
        }

        if (machine.getLevel() == null || machine.getLevel().isClientSide()) {
            return;
        }

        synchronized (ACTIVE_MACHINES) {
            ACTIVE_MACHINES.add(machine);
        }
    }

    public static void unregisterMachine(BlockEntity machine) {
        if (machine == null) {
            return;
        }

        synchronized (ACTIVE_MACHINES) {
            ACTIVE_MACHINES.remove(machine);
        }
    }

    public static void registerChunkMachines(Iterable<BlockEntity> machines) {
        if (!isLoaded() || machines == null) {
            return;
        }

        for (BlockEntity machine : machines) {
            registerMachine(machine);
        }
    }

    public static void unregisterChunkMachines(Iterable<BlockEntity> machines) {
        if (machines == null) {
            return;
        }

        for (BlockEntity machine : machines) {
            unregisterMachine(machine);
        }
    }

    public static long tickMachine(BlockEntity machine) {
        if (machine == null || !isLoaded()) {
            return 0L;
        }

        if (!(machine instanceof MachineBlockEntityAccess access)) {
            return 0L;
        }

        MachineAttachmentLifecycle lifecycle = access.appliedthermal$getAttachmentLifecycle();
        if (lifecycle == null) {
            return 0L;
        }

        long moved = 0L;
        for (MachineAttachment attachment : lifecycle.getAttachments()) {
            if (!(attachment instanceof AppliedThermalProviderAttachment provider)) {
                continue;
            }
            moved += tickAttachment(provider);
        }
        return moved;
    }

    public static long tickAttachment(AppliedThermalProviderAttachment attachment) {
        if (attachment == null || !attachment.isNodeRunning() || !attachment.isEnabled()) {
            return 0L;
        }
        AppFluxBridge bridge = getBridge();
        if (!bridge.hasInductionCard(attachment.getLogic())) {
            return 0L;
        }

        IGrid grid = attachment.getGrid();
        IGridNode node = attachment.getActionableNode();
        if (grid == null || node == null || !node.isActive()) {
            return 0L;
        }

        var storageService = grid.getStorageService();
        if (storageService == null) {
            return 0L;
        }

        MEStorage storage = storageService.getInventory();
        if (storage == null) {
            return 0L;
        }

        long moved = bridge.transferEnergy(attachment.getMachine(), storage, IActionSource.ofMachine(attachment));
        if (moved > 0L) {
            attachment.saveChanges();
        }
        return moved;
    }

    static AppFluxBridge getBridge() {
        AppFluxBridge bridge = BRIDGE.get();
        if (bridge != null) {
            return bridge;
        }

        AppFluxBridge created = createBridge();
        if (BRIDGE.compareAndSet(null, created)) {
            return created;
        }
        return BRIDGE.get();
    }

    private static void registerSupportedMachineIcons(AppFluxBridge bridge) {
        if (bridge == null || !bridge.isPresent()) {
            return;
        }

        for (Item machineIcon : getSupportedThermalMachineItems()) {
            bridge.registerUpgrade(machineIcon);
        }
    }

    private static List<Item> getSupportedThermalMachineItems() {
        Set<Item> machineItems = new LinkedHashSet<Item>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
            if (key == null) {
                continue;
            }

            if (!"thermal".equals(key.getNamespace()) || !key.getPath().startsWith("machine_")) {
                continue;
            }

            if (item instanceof BlockItem) {
                machineItems.add(item);
            }
        }
        return new ArrayList<Item>(machineItems);
    }

    private static AppFluxBridge createBridge() {
        if (!isLoaded()) {
            return AppFluxNoopBridge.INSTANCE;
        }

        Optional<AppFluxBridge> bridge = ReflectiveAppFluxBridge.create();
        if (bridge.isPresent()) {
            return bridge.get();
        }

        AppliedThermal.LOG.warn("AppFlux is installed, but the reflective integration bridge could not be created.");
        return AppFluxNoopBridge.INSTANCE;
    }
}
