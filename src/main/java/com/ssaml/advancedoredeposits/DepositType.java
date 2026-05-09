package com.ssaml.advancedoredeposits;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

public enum DepositType {
    IRON("iron", "Iron", false, true, 30, 28, 64),
    COPPER("copper", "Copper", false, true, 28, 28, 64),
    GOLD("gold", "Gold", false, true, 4, 16, 36),
    DIAMOND("diamond", "Diamond", false, true, 1, 12, 24),
    COAL("coal", "Coal", false, true, 26, 24, 64),
    ANDESITE("andesite", "Andesite", false, true, 24, 24, 64),
    REDSTONE("redstone", "Redstone", false, true, 10, 20, 48),
    LAPIS("lapis", "Lapis Lazuli", false, true, 9, 20, 44),
    QUARTZ("nether_quartz", "Nether Quartz", false, true, 8, 20, 44),
    ZINC("zinc", "Zinc", false, true, 16, 28, 64);

    private final String id;
    private final String displayName;
    private final boolean smeltedChain;
    private final boolean directChain;
    private final int defaultWeight;
    private final int defaultMinDiameter;
    private final int defaultMaxDiameter;

    DepositType(String id, String displayName, boolean smeltedChain, boolean directChain, int defaultWeight,
            int defaultMinDiameter, int defaultMaxDiameter) {
        this.id = id;
        this.displayName = displayName;
        this.smeltedChain = smeltedChain;
        this.directChain = directChain;
        this.defaultWeight = defaultWeight;
        this.defaultMinDiameter = defaultMinDiameter;
        this.defaultMaxDiameter = defaultMaxDiameter;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean usesSmeltedChunks() {
        return smeltedChain;
    }

    public boolean usesDirectRecipe() {
        return directChain;
    }

    public int defaultWeight() {
        return defaultWeight;
    }

    public int defaultMinDiameter() {
        return defaultMinDiameter;
    }

    public int defaultMaxDiameter() {
        return defaultMaxDiameter;
    }

    public String depositBlockId() {
        return id + "_deposit";
    }

    public String rawChunkId() {
        return "raw_" + id + "_chunk";
    }

    public String smeltedChunkId() {
        return "smelted_" + id + "_chunk";
    }

    public String configKey() {
        return id.toLowerCase(Locale.ROOT);
    }

    public ResourceLocation resourceLocation() {
        return AdvancedOreDeposits.id(id);
    }

    public static Optional<DepositType> byId(String id) {
        return Arrays.stream(values()).filter(type -> type.id.equals(id)).findFirst();
    }
}
