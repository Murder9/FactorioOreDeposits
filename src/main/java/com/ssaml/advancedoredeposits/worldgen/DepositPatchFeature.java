package com.ssaml.advancedoredeposits.worldgen;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.ssaml.advancedoredeposits.DepositType;
import com.ssaml.advancedoredeposits.ModConfig;
import com.ssaml.advancedoredeposits.block.DepositBlock;
import com.ssaml.advancedoredeposits.block.DepositBlockEntity;
import com.ssaml.advancedoredeposits.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class DepositPatchFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SURFACE_SCAN_DEPTH = 48;
    private static final int OBSTRUCTION_CLEAR_HEIGHT = 48;
    private static final int TREE_CLEAR_RADIUS = 16;
    private static final int TREE_CLEAR_VERTICAL_RANGE = 48;
    private static final int TREE_CLEAR_BLOCK_LIMIT = 1024;

    public DepositPatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!ModConfig.GENERATION_ENABLED.getAsBoolean()) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;

        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;
        int spacingBlocks = ModConfig.DEPOSIT_SPACING_CHUNKS.getAsInt() * 16;
        int maxInfluence = Mth.ceil(ModConfig.MAX_PATCH_DIAMETER.getAsInt() * 0.75D) + 4;
        long minCellX = Math.floorDiv(chunkMinX - maxInfluence, spacingBlocks);
        long maxCellX = Math.floorDiv(chunkMaxX + maxInfluence, spacingBlocks);
        long minCellZ = Math.floorDiv(chunkMinZ - maxInfluence, spacingBlocks);
        long maxCellZ = Math.floorDiv(chunkMaxZ + maxInfluence, spacingBlocks);

        boolean placedAny = false;
        for (long cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (long cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                DepositPatchPlan plan = DepositPatchPlan.forCell(level.getSeed(), cellX, cellZ);
                if (plan == null || !plan.intersectsChunk(chunkX, chunkZ)) {
                    continue;
                }
                if (placeSlice(level, plan, chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ)) {
                    placedAny = true;
                }
            }
        }
        return placedAny;
    }

    private static boolean placeSlice(WorldGenLevel level, DepositPatchPlan plan, int chunkMinX, int chunkMinZ,
            int chunkMaxX, int chunkMaxZ) {
        int minX = Math.max(chunkMinX, Mth.floor(plan.centerX() - plan.radiusX()));
        int maxX = Math.min(chunkMaxX, Mth.ceil(plan.centerX() + plan.radiusX()));
        int minZ = Math.max(chunkMinZ, Mth.floor(plan.centerZ() - plan.radiusZ()));
        int maxZ = Math.min(chunkMaxZ, Mth.ceil(plan.centerZ() + plan.radiusZ()));
        BlockState depositState = ModBlocks.deposit(plan.type()).defaultBlockState();
        int maxForPatch = maxRemainingForDiameter(plan.diameter());
        boolean placedAny = false;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double normalized = normalizedDistance(plan, x, z);
                double threshold = threshold(plan, x, z);
                if (normalized > threshold) {
                    continue;
                }

                BlockPos surface = surfacePos(level, x, z);
                if (surface == null || !isValidSurface(level, surface, plan.type(), plan.patchId())) {
                    continue;
                }
                if (!isAllowedBiome(level, surface)) {
                    continue;
                }

                double distance = Math.sqrt(Math.max(0.0D, normalized));
                int remaining = remainingForDistance(distance, plan.diameter(), maxForPatch);
                clearObstructionsAbove(level, surface);
                level.setBlock(surface, depositState, Block.UPDATE_ALL);
                BlockEntity blockEntity = level.getBlockEntity(surface);
                if (blockEntity instanceof DepositBlockEntity deposit) {
                    deposit.initialize(plan.type(), remaining, maxForPatch, plan.diameter(), plan.patchId());
                }
                placedAny = true;
            }
        }
        return placedAny;
    }

    private static boolean isAllowedBiome(WorldGenLevel level, BlockPos pos) {
        ResourceLocation biomeId = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getKey(level.getBiome(pos).value());
        return biomeId != null && ModConfig.isAllowedBiome(biomeId);
    }

    private static BlockPos surfacePos(WorldGenLevel level, int x, int z) {
        int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        if (level.isOutsideBuildHeight(topY)) {
            return null;
        }

        int minY = Math.max(level.getMinBuildHeight(), topY - SURFACE_SCAN_DEPTH);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, topY, z);
        for (int y = topY; y >= minY; y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (isSurfaceMaterial(state)) {
                return cursor.immutable();
            }
            if (!canScanThrough(level, cursor, state)) {
                return null;
            }
        }
        return null;
    }

    private static boolean isValidSurface(WorldGenLevel level, BlockPos pos, DepositType type, long patchId) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty() || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
            return false;
        }
        if (state.getBlock() instanceof DepositBlock depositBlock) {
            return depositBlock.type() == type || randomNoise(pos.getX(), pos.getZ(), patchId ^ 0x6d2b79f5L) > 0.50D;
        }
        return isSurfaceMaterial(state);
    }

    private static boolean isSurfaceMaterial(BlockState state) {
        return state.getBlock() instanceof DepositBlock
                || state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.STONE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.GRAVEL);
    }

    private static boolean canScanThrough(WorldGenLevel level, BlockPos pos, BlockState state) {
        return state.isAir()
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES)
                || canClearAbove(level, pos, state)
                || isPlantObstruction(state);
    }

    private static void clearObstructionsAbove(WorldGenLevel level, BlockPos surface) {
        BlockPos.MutableBlockPos cursor = surface.mutable();
        int maxY = Math.min(level.getMaxBuildHeight() - 1, surface.getY() + OBSTRUCTION_CLEAR_HEIGHT);
        for (int y = surface.getY() + 1; y <= maxY; y++) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                removeConnectedTree(level, cursor.immutable());
                continue;
            }
            if (canClearAbove(level, cursor, state) || isPlantObstruction(state)) {
                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS);
                continue;
            }
            if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, cursor).isEmpty()) {
                return;
            }
        }
    }

    private static void removeConnectedTree(WorldGenLevel level, BlockPos seed) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(seed);

        int minY = Math.max(level.getMinBuildHeight(), seed.getY() - TREE_CLEAR_VERTICAL_RANGE);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, seed.getY() + TREE_CLEAR_VERTICAL_RANGE);

        while (!queue.isEmpty() && visited.size() < TREE_CLEAR_BLOCK_LIMIT) {
            BlockPos pos = queue.remove();
            if (!visited.add(pos) || !isInsideTreeClearBounds(seed, pos, minY, maxY)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
                continue;
            }

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS);
            for (Direction direction : Direction.values()) {
                queue.add(pos.relative(direction));
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        queue.add(pos.offset(dx, 0, dz));
                    }
                }
            }
        }
    }

    private static boolean isInsideTreeClearBounds(BlockPos seed, BlockPos pos, int minY, int maxY) {
        return pos.getY() >= minY
                && pos.getY() <= maxY
                && Math.abs(pos.getX() - seed.getX()) <= TREE_CLEAR_RADIUS
                && Math.abs(pos.getZ() - seed.getZ()) <= TREE_CLEAR_RADIUS;
    }

    private static boolean isPlantObstruction(BlockState state) {
        return state.is(Blocks.CACTUS)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.BAMBOO)
                || state.is(Blocks.BAMBOO_SAPLING);
    }

    private static double normalizedDistance(DepositPatchPlan plan, int x, int z) {
        double dx = x - plan.centerX();
        double dz = z - plan.centerZ();
        return (dx * dx) / (plan.radiusX() * plan.radiusX())
                + (dz * dz) / (plan.radiusZ() * plan.radiusZ());
    }

    private static double threshold(DepositPatchPlan plan, int x, int z) {
        double dx = x - plan.centerX();
        double dz = z - plan.centerZ();
        double largeNoise = smoothNoise(x, z, plan.patchId() ^ 0x51f15eL, 18.0D);
        double smallNoise = smoothNoise(x, z, plan.patchId(), 5.0D);
        double angle = Math.atan2(dz / plan.radiusZ(), dx / plan.radiusX());
        double lobes = 0.12D * Math.sin(angle * 3.0D + largeNoise * Math.PI * 2.0D)
                + 0.08D * Math.sin(angle * 5.0D + smallNoise * Math.PI * 2.0D);
        return plan.wobble() * (0.78D + 0.34D * largeNoise + 0.22D * smallNoise + lobes);
    }

    private static boolean canClearAbove(WorldGenLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)
                && state.getCollisionShape(level, pos).isEmpty();
    }

    private static int maxRemainingForDiameter(int diameter) {
        int min = ModConfig.MIN_REMAINING_BLOCKS.getAsInt();
        int max = Math.max(min, ModConfig.MAX_REMAINING_BLOCKS.getAsInt());
        double sizeFactor = Mth.clamp((diameter - 16.0D) / 48.0D, 0.0D, 1.0D);
        return min + Mth.floor((max - min) * (0.35D + 0.65D * sizeFactor));
    }

    private static int remainingForDistance(double distance, int diameter, int maxForPatch) {
        int min = ModConfig.MIN_REMAINING_BLOCKS.getAsInt();
        double centerFactor = Math.pow(Mth.clamp(1.0D - distance, 0.0D, 1.0D), 1.6D);
        return min + Mth.floor((maxForPatch - min) * centerFactor);
    }

    private static double randomNoise(int x, int z, long seed) {
        long value = mix(seed ^ (x * 3129871L) ^ (z * 116129781L));
        return ((value >>> 11) & 0xFFFFFF) / (double) 0xFFFFFF;
    }

    private static double smoothNoise(double x, double z, long seed, double scale) {
        double sampleX = x / scale;
        double sampleZ = z / scale;
        int x0 = Mth.floor(sampleX);
        int z0 = Mth.floor(sampleZ);
        double tx = smooth(sampleX - x0);
        double tz = smooth(sampleZ - z0);
        double n00 = randomNoise(x0, z0, seed);
        double n10 = randomNoise(x0 + 1, z0, seed);
        double n01 = randomNoise(x0, z0 + 1, seed);
        double n11 = randomNoise(x0 + 1, z0 + 1, seed);
        return Mth.lerp(tz, Mth.lerp(tx, n00, n10), Mth.lerp(tx, n01, n11));
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
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
