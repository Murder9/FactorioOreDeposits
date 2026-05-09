package com.ssaml.advancedoredeposits;

import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final Set<ResourceLocation> ALLOWED_GENERATION_BIOMES = Set.of(
            minecraft("plains"),
            minecraft("sunflower_plains"),
            minecraft("desert"),
            minecraft("jungle"),
            minecraft("sparse_jungle"),
            minecraft("savanna"));

    public static final ModConfigSpec.BooleanValue GENERATION_ENABLED = BUILDER
            .comment("Master switch for Advanced Ore Deposits world generation.")
            .translation("advanced_ore_deposits.configuration.generateDeposits")
            .define("generateDeposits", true);

    public static final ModConfigSpec.IntValue DEPOSIT_SPACING_CHUNKS = BUILDER
            .comment("Deposit candidate cells are this many chunks wide. Larger values spread patches farther apart.")
            .translation("advanced_ore_deposits.configuration.depositSpacingChunks")
            .defineInRange("depositSpacingChunks", 8, 2, 128);

    public static final ModConfigSpec.DoubleValue DEPOSIT_CHANCE = BUILDER
            .comment("Chance for an eligible cell anchor chunk to actually generate a deposit.")
            .translation("advanced_ore_deposits.configuration.depositChance")
            .defineInRange("depositChance", 0.35D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue MIN_REMAINING_BLOCKS = BUILDER
            .comment("Lowest remaining mine cycles assigned to deposit edge blocks.")
            .translation("advanced_ore_deposits.configuration.minRemainingBlocks")
            .defineInRange("minRemainingBlocks", 4096, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_REMAINING_BLOCKS = BUILDER
            .comment("Highest remaining mine cycles assigned near the center of large deposits.")
            .translation("advanced_ore_deposits.configuration.maxRemainingBlocks")
            .defineInRange("maxRemainingBlocks", 32768, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MIN_PATCH_DIAMETER = BUILDER
            .comment("Global lower bound for deposit patch diameter.")
            .translation("advanced_ore_deposits.configuration.minPatchDiameter")
            .defineInRange("minPatchDiameter", 16, 4, 256);

    public static final ModConfigSpec.IntValue MAX_PATCH_DIAMETER = BUILDER
            .comment("Global upper bound for deposit patch diameter.")
            .translation("advanced_ore_deposits.configuration.maxPatchDiameter")
            .defineInRange("maxPatchDiameter", 64, 4, 256);

    public static final ModConfigSpec.IntValue CHUNK_DROP_COUNT = BUILDER
            .comment("Raw chunks dropped per mined deposit block cycle.")
            .translation("advanced_ore_deposits.configuration.chunkDropCount")
            .defineInRange("chunkDropCount", 1, 1, 64);

    public static final ModConfigSpec.BooleanValue ONLY_DROP_IF_MINED_BY_PLAYER = BUILDER
            .comment("When true, automation only consumes a deposit cycle if the chunk can be inserted into an inventory above or below the deposit. Player mining and explosions still consume and drop normally. When false, automation drops loose chunk items if no inventory accepts them.")
            .translation("advanced_ore_deposits.configuration.onlyDropIfMinedByPlayer")
            .define("onlyDropIfMinedByPlayer", true);

    public static final ModConfigSpec.BooleanValue LOAD_CHUNKS_WHEN_MINED = BUILDER
            .comment("Keep chunks around mined deposits loaded for 15 seconds.")
            .translation("advanced_ore_deposits.configuration.loadChunksWhenMined")
            .define("loadChunksWhenMined", true);

    public static final ModConfigSpec.IntValue LOADED_CHUNK_RADIUS = BUILDER
            .comment("Square radius of chunks to keep loaded. 1 loads only the mined block's chunk, 2 loads a 3x3 area, 3 loads a 5x5 area.")
            .translation("advanced_ore_deposits.configuration.loadedChunkRadius")
            .defineInRange("loadedChunkRadius", 2, 1, 16);

    public static final ModConfigSpec.BooleanValue SHOW_HUD_INFO = BUILDER
            .comment("If true, Jade and Create goggles show deposit remaining blocks when those mods are installed.")
            .translation("advanced_ore_deposits.configuration.showHudInfo")
            .define("showHudInfo", true);

    public static final ModConfigSpec.BooleanValue ZINC_REQUIRES_CREATE = BUILDER
            .comment("If true, zinc deposits only generate when Create is installed.")
            .translation("advanced_ore_deposits.configuration.zincRequiresCreate")
            .define("zincRequiresCreate", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ModConfig() {
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    public static boolean isAllowedBiome(ResourceLocation biomeId) {
        return ALLOWED_GENERATION_BIOMES.contains(biomeId);
    }

    public static boolean isTypeEnabled(DepositType type) {
        return true;
    }

    public static int typeWeight(DepositType type) {
        return type.defaultWeight();
    }

    public static int minDiameter(DepositType type) {
        int globalMin = MIN_PATCH_DIAMETER.getAsInt();
        int globalMax = MAX_PATCH_DIAMETER.getAsInt();
        return Math.min(Math.max(type.defaultMinDiameter(), globalMin), globalMax);
    }

    public static int maxDiameter(DepositType type) {
        int min = minDiameter(type);
        int globalMax = MAX_PATCH_DIAMETER.getAsInt();
        return Math.max(min, Math.min(type.defaultMaxDiameter(), globalMax));
    }
}
