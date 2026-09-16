package com.ssaml.advancedoredeposits.worldgen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ssaml.advancedoredeposits.block.DepositBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DepositSyncEvents {
    private static final int STANDARD_SYNC_TICKS = 2;
    private static final int PLAYER_SYNC_TICKS = 20;
    private static final Map<ResourceKey<Level>, Map<BlockPos, PendingSync>> PENDING_SYNCS = new HashMap<>();

    private DepositSyncEvents() {
    }

    public static void queue(ServerLevel level, BlockPos pos) {
        queue(level, pos, false);
    }

    public static void queue(ServerLevel level, BlockPos pos, boolean playerMined) {
        PENDING_SYNCS.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .put(pos.immutable(), new PendingSync(playerMined ? PLAYER_SYNC_TICKS : STANDARD_SYNC_TICKS));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<ResourceKey<Level>, Map<BlockPos, PendingSync>>> dimensions = PENDING_SYNCS.entrySet()
                .iterator();
        while (dimensions.hasNext()) {
            Map.Entry<ResourceKey<Level>, Map<BlockPos, PendingSync>> dimensionEntry = dimensions.next();
            ServerLevel level = event.getServer().getLevel(dimensionEntry.getKey());
            if (level == null) {
                dimensions.remove();
                continue;
            }

            Iterator<Map.Entry<BlockPos, PendingSync>> positions = dimensionEntry.getValue().entrySet().iterator();
            while (positions.hasNext()) {
                Map.Entry<BlockPos, PendingSync> positionEntry = positions.next();
                BlockEntity blockEntity = level.getBlockEntity(positionEntry.getKey());
                if (blockEntity instanceof DepositBlockEntity deposit) {
                    deposit.sync();
                }

                PendingSync pendingSync = positionEntry.getValue();
                int ticksLeft = pendingSync.ticksLeft() - 1;
                if (ticksLeft <= 0) {
                    positions.remove();
                } else {
                    positionEntry.setValue(new PendingSync(ticksLeft));
                }
            }

            if (dimensionEntry.getValue().isEmpty()) {
                dimensions.remove();
            }
        }
    }

    private record PendingSync(int ticksLeft) {
    }
}
