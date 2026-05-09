package com.ssaml.advancedoredeposits.worldgen;

import com.ssaml.advancedoredeposits.AdvancedOreDeposits;
import com.ssaml.advancedoredeposits.DepositType;
import com.ssaml.advancedoredeposits.ModConfig;

import net.minecraft.util.RandomSource;

public record DepositPatchPlan(DepositType type, int anchorChunkX, int anchorChunkZ, int centerX, int centerZ,
        int diameter, double radiusX, double radiusZ, double wobble, long patchId) {
    public static DepositPatchPlan forChunk(long seed, int chunkX, int chunkZ) {
        int spacing = ModConfig.DEPOSIT_SPACING_CHUNKS.getAsInt();
        long cellX = Math.floorDiv(chunkX, spacing);
        long cellZ = Math.floorDiv(chunkZ, spacing);
        DepositPatchPlan plan = forCell(seed, cellX, cellZ);
        return plan != null && plan.anchorChunkX() == chunkX && plan.anchorChunkZ() == chunkZ ? plan : null;
    }

    public static DepositPatchPlan forCell(long seed, long cellX, long cellZ) {
        int spacing = ModConfig.DEPOSIT_SPACING_CHUNKS.getAsInt();
        long cellSeed = mix(seed ^ (cellX * 341873128712L) ^ (cellZ * 132897987541L));
        int anchorX = Math.floorMod((int) (cellSeed >>> 17), spacing);
        int anchorZ = Math.floorMod((int) (cellSeed >>> 33), spacing);
        int anchorChunkX = Math.toIntExact(cellX * spacing + anchorX);
        int anchorChunkZ = Math.toIntExact(cellZ * spacing + anchorZ);

        RandomSource random = RandomSource.create(cellSeed);
        if (random.nextDouble() > ModConfig.DEPOSIT_CHANCE.getAsDouble()) {
            return null;
        }

        int centerX = (anchorChunkX << 4) + random.nextInt(16);
        int centerZ = (anchorChunkZ << 4) + random.nextInt(16);
        DepositType type = chooseType(random);
        if (type == null) {
            return null;
        }

        int minDiameter = ModConfig.minDiameter(type);
        int maxDiameter = ModConfig.maxDiameter(type);
        int diameter = minDiameter + random.nextInt(maxDiameter - minDiameter + 1);
        double radiusX = diameter * (0.48D + random.nextDouble() * 0.22D);
        double radiusZ = diameter * (0.48D + random.nextDouble() * 0.22D);
        double wobble = 0.90D + random.nextDouble() * 0.32D;
        long patchId = cellSeed ^ (type.ordinal() * 0x9e3779b97f4a7c15L);
        return new DepositPatchPlan(type, anchorChunkX, anchorChunkZ, centerX, centerZ, diameter, radiusX, radiusZ,
                wobble, patchId);
    }

    public boolean intersectsChunk(int chunkX, int chunkZ) {
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;
        return centerX + radiusX >= chunkMinX && centerX - radiusX <= chunkMaxX
                && centerZ + radiusZ >= chunkMinZ && centerZ - radiusZ <= chunkMaxZ;
    }

    private static DepositType chooseType(RandomSource random) {
        int total = 0;
        for (DepositType type : DepositType.values()) {
            if (!canGenerate(type)) {
                continue;
            }
            total += ModConfig.typeWeight(type);
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (DepositType type : DepositType.values()) {
            if (!canGenerate(type)) {
                continue;
            }
            roll -= ModConfig.typeWeight(type);
            if (roll < 0) {
                return type;
            }
        }
        return null;
    }

    private static boolean canGenerate(DepositType type) {
        if (!ModConfig.isTypeEnabled(type) || ModConfig.typeWeight(type) <= 0) {
            return false;
        }
        return type != DepositType.ZINC || !ModConfig.ZINC_REQUIRES_CREATE.getAsBoolean()
                || AdvancedOreDeposits.isCreateLoaded();
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
