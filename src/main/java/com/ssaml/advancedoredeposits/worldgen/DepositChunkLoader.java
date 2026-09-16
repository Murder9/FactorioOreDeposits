package com.ssaml.advancedoredeposits.worldgen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ssaml.advancedoredeposits.ModConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DepositChunkLoader {
    private static final int TICKS_PER_SECOND = 20;
    private static final int FORCE_LOAD_SECONDS = 15;
    private static final Map<ResourceKey<Level>, Map<Long, ForcedChunk>> FORCED_CHUNKS = new HashMap<>();

    private DepositChunkLoader() {
    }

    public static void forceAround(ServerLevel level, BlockPos pos) {
        if (!ModConfig.LOAD_CHUNKS_WHEN_MINED.getAsBoolean()) {
            return;
        }

        long expiresAt = level.getGameTime() + (long) FORCE_LOAD_SECONDS * TICKS_PER_SECOND;
        ChunkPos center = new ChunkPos(pos);
        Map<Long, ForcedChunk> dimensionChunks = FORCED_CHUNKS.computeIfAbsent(level.dimension(), ignored -> new HashMap<>());
        int extent = Math.max(0, ModConfig.LOADED_CHUNK_RADIUS.getAsInt() - 1);
        for (int dx = -extent; dx <= extent; dx++) {
            for (int dz = -extent; dz <= extent; dz++) {
                ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
                long chunkKey = chunk.toLong();
                dimensionChunks.compute(chunkKey, (ignored, previous) -> {
                    if (previous == null) {
                        boolean alreadyForced = level.getForcedChunks().contains(chunkKey);
                        if (!alreadyForced) {
                            level.setChunkForced(chunk.x, chunk.z, true);
                        }
                        return new ForcedChunk(expiresAt, alreadyForced);
                    }
                    return new ForcedChunk(Math.max(previous.expiresAt(), expiresAt), previous.alreadyForced());
                });
            }
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<ResourceKey<Level>, Map<Long, ForcedChunk>>> dimensions = FORCED_CHUNKS.entrySet().iterator();
        while (dimensions.hasNext()) {
            Map.Entry<ResourceKey<Level>, Map<Long, ForcedChunk>> dimensionEntry = dimensions.next();
            ServerLevel level = event.getServer().getLevel(dimensionEntry.getKey());
            if (level == null) {
                dimensions.remove();
                continue;
            }

            long now = level.getGameTime();
            Iterator<Map.Entry<Long, ForcedChunk>> chunks = dimensionEntry.getValue().entrySet().iterator();
            while (chunks.hasNext()) {
                Map.Entry<Long, ForcedChunk> chunkEntry = chunks.next();
                ForcedChunk forcedChunk = chunkEntry.getValue();
                if (forcedChunk.expiresAt() > now) {
                    continue;
                }

                ChunkPos chunk = new ChunkPos(chunkEntry.getKey());
                if (!forcedChunk.alreadyForced()) {
                    level.setChunkForced(chunk.x, chunk.z, false);
                }
                chunks.remove();
            }

            if (dimensionEntry.getValue().isEmpty()) {
                dimensions.remove();
            }
        }
    }

    private record ForcedChunk(long expiresAt, boolean alreadyForced) {
    }
}
