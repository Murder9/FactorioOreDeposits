package com.ssaml.advancedoredeposits.worldgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ssaml.advancedoredeposits.AdvancedOreDeposits;
import com.ssaml.advancedoredeposits.DepositType;
import com.ssaml.advancedoredeposits.ModConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record DepositPatchPlan(DepositType type, ResourceLocation sourceBlockId, int anchorChunkX, int anchorChunkZ,
        int centerX, int centerZ, int diameter, double radiusX, double radiusZ, double wobble, long patchId) {

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

        DepositSelection selection = chooseType(random);
        if (selection == null) return null;

        int minDiameter = ModConfig.minDiameter(selection.type());
        int maxDiameter = ModConfig.maxDiameter(selection.type());
        int diameter = minDiameter + random.nextInt(maxDiameter - minDiameter + 1);
        double radiusX = diameter * (0.48D + random.nextDouble() * 0.22D);
        double radiusZ = diameter * (0.48D + random.nextDouble() * 0.22D);
        double wobble = 0.90D + random.nextDouble() * 0.32D;
        long patchId = cellSeed ^ (selection.type().ordinal() * 0x9e3779b97f4a7c15L);
        return new DepositPatchPlan(selection.type(), selection.sourceBlockId(), anchorChunkX, anchorChunkZ, centerX,
                centerZ, diameter, radiusX, radiusZ, wobble, patchId);
    }

    public boolean intersectsChunk(int chunkX, int chunkZ) {
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;
        return centerX + radiusX >= chunkMinX && centerX - radiusX <= chunkMaxX
                && centerZ + radiusZ >= chunkMinZ && centerZ - radiusZ <= chunkMaxZ;
    }

    private static DepositSelection chooseType(RandomSource random) {
        List<ResourceLocation> moddedOres = discoverModdedOres();
        int total = 0;
        for (DepositType type : DepositType.values()) {
            if (type == DepositType.MODDED) {
                if (!moddedOres.isEmpty() && ModConfig.isTypeEnabled(type) && ModConfig.typeWeight(type) > 0) {
                    total += ModConfig.typeWeight(type);
                }
            } else if (canGenerate(type)) {
                total += ModConfig.typeWeight(type);
            }
        }
        if (total <= 0) return null;

        int roll = random.nextInt(total);
        for (DepositType type : DepositType.values()) {
            if (type == DepositType.MODDED) {
                if (moddedOres.isEmpty() || !ModConfig.isTypeEnabled(type) || ModConfig.typeWeight(type) <= 0) {
                    continue;
                }
            } else if (!canGenerate(type)) {
                continue;
            }

            roll -= ModConfig.typeWeight(type);
            if (roll < 0) {
                if (type == DepositType.MODDED) {
                    return new DepositSelection(type, moddedOres.get(random.nextInt(moddedOres.size())));
                }
                return new DepositSelection(type, null);
            }
        }
        return null;
    }

    private static boolean canGenerate(DepositType type) {
        if (!ModConfig.isTypeEnabled(type) || ModConfig.typeWeight(type) <= 0) return false;
        return type != DepositType.ZINC || !ModConfig.ZINC_REQUIRES_CREATE.getAsBoolean()
                || AdvancedOreDeposits.isCreateLoaded();
    }

    /**
     * Finds common-tagged modded ores at runtime. Deepslate/stone variants of
     * the same material are collapsed so a mod does not get multiple entries
     * for one resource.
     */
    private static List<ResourceLocation> discoverModdedOres() {
        Map<String, ResourceLocation> byMaterial = new LinkedHashMap<>();
        var oreTag = BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores"));

        for (Map.Entry<ResourceLocation, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation id = entry.getKey();
            Block block = entry.getValue();

            if ("minecraft".equals(id.getNamespace()) || AdvancedOreDeposits.MOD_ID.equals(id.getNamespace())) continue;
            if (id.getNamespace().equals("create") && "zinc_ore".equals(id.getPath())) continue;
            boolean taggedOre = block.defaultBlockState().is(oreTag);
            boolean namedOre = id.getPath().endsWith("_ore");
            if (!taggedOre && !namedOre) continue;
            if (block == Blocks.AIR || block.asItem() == net.minecraft.world.item.Items.AIR) continue;

            String material = id.getPath();
            material = material.replaceFirst("^(deepslate|stone|netherrack|blackstone)_", "");
            if (material.endsWith("_ore")) {
                material = material.substring(0, material.length() - 4);
            }
            byMaterial.putIfAbsent(id.getNamespace() + ":" + material, id);
        }
        return List.copyOf(byMaterial.values());
    }

    private record DepositSelection(DepositType type, ResourceLocation sourceBlockId) {}

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
