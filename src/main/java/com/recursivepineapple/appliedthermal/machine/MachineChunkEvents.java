package com.recursivepineapple.appliedthermal.machine;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import com.recursivepineapple.appliedthermal.integration.appflux.AppFluxCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AppliedThermal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MachineChunkEvents {

    private MachineChunkEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        AppFluxCompat.registerChunkMachines(chunk.getBlockEntities().values());
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        for (var blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof MachineBlockEntityAccess access) {
                access.appliedthermal$invalidateHost();
            }
        }
        AppFluxCompat.unregisterChunkMachines(chunk.getBlockEntities().values());
    }
}
