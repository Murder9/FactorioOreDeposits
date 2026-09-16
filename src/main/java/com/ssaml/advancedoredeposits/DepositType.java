package com.ssaml.advancedoredeposits;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum DepositType {
    IRON("iron", "Iron", 30, 28, 64),
    COPPER("copper", "Copper", 28, 28, 64),
    GOLD("gold", "Gold", 4, 16, 36),
    DIAMOND("diamond", "Diamond", 1, 12, 24),
    COAL("coal", "Coal", 26, 24, 64),
    ANDESITE("andesite", "Andesite", 24, 24, 64),
    REDSTONE("redstone", "Redstone", 10, 20, 48),
    LAPIS("lapis", "Lapis Lazuli", 9, 20, 44),
    QUARTZ("nether_quartz", "Nether Quartz", 8, 20, 44),
    CALCITE("calcite", "Calcite", 14, 20, 52),
    ZINC("zinc", "Zinc", 16, 28, 64),
    MODDED("modded", "Modded Ore", 18, 20, 56);

    private final String id;
    private final String displayName;
    private final int defaultWeight;
    private final int defaultMinDiameter;
    private final int defaultMaxDiameter;

    DepositType(String id, String displayName, int defaultWeight, int defaultMinDiameter, int defaultMaxDiameter) {
        this.id = id;
        this.displayName = displayName;
        this.defaultWeight = defaultWeight;
        this.defaultMinDiameter = defaultMinDiameter;
        this.defaultMaxDiameter = defaultMaxDiameter;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public boolean usesSmeltedChunks() { return false; }
    public boolean usesDirectRecipe() { return true; }
    public int defaultWeight() { return defaultWeight; }
    public int defaultMinDiameter() { return defaultMinDiameter; }
    public int defaultMaxDiameter() { return defaultMaxDiameter; }
    public String depositBlockId() { return id + "_deposit"; }
    public String configKey() { return id.toLowerCase(Locale.ROOT); }

    public static Optional<DepositType> byId(String id) {
        return Arrays.stream(values()).filter(type -> type.id.equals(id)).findFirst();
    }
}
