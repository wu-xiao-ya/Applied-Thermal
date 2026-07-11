package com.recursivepineapple.appliedthermal.integration.appflux;

import com.recursivepineapple.appliedthermal.AppliedThermal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AppliedThermal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AppFluxChunkEvents {

    private AppFluxChunkEvents() {
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

        AppFluxCompat.unregisterChunkMachines(chunk.getBlockEntities().values());
    }
}
