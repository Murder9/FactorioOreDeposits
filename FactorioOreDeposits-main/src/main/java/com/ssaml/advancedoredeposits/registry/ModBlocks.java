package com.ssaml.advancedoredeposits.registry;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import com.ssaml.advancedoredeposits.AdvancedOreDeposits;
import com.ssaml.advancedoredeposits.DepositType;
import com.ssaml.advancedoredeposits.block.DepositBlock;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AdvancedOreDeposits.MOD_ID);
    public static final Map<DepositType, DeferredBlock<DepositBlock>> DEPOSITS = new EnumMap<>(DepositType.class);

    static {
        for (DepositType type : DepositType.values()) {
            DEPOSITS.put(type, BLOCKS.register(type.depositBlockId(),
                    () -> new DepositBlock(type, propertiesFor(type))));
        }
    }

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) { BLOCKS.register(modEventBus); }

    public static DepositBlock deposit(DepositType type) { return DEPOSITS.get(type).get(); }

    public static Block[] depositBlockArray() {
        Collection<DeferredBlock<DepositBlock>> values = DEPOSITS.values();
        return values.stream().map(DeferredBlock::get).toArray(Block[]::new);
    }

    public static Block sourceBlock(DepositType type) {
        return switch (type) {
            case IRON -> Blocks.IRON_ORE;
            case COPPER -> Blocks.COPPER_ORE;
            case GOLD -> Blocks.GOLD_ORE;
            case DIAMOND -> Blocks.DIAMOND_ORE;
            case COAL -> Blocks.COAL_ORE;
            case ANDESITE -> Blocks.ANDESITE;
            case REDSTONE -> Blocks.REDSTONE_ORE;
            case LAPIS -> Blocks.LAPIS_ORE;
            case QUARTZ -> Blocks.NETHER_QUARTZ_ORE;
            case CALCITE -> Blocks.CALCITE;
            case ZINC -> zincOreBlock();
            case MODDED -> Blocks.STONE;
        };
    }

    public static Block block(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        return block == null ? Blocks.AIR : block;
    }

    private static BlockBehaviour.Properties propertiesFor(DepositType type) {
        Block source = sourceBlock(type);
        if (type == DepositType.MODDED) {
            source = Blocks.STONE;
        }
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(source);
        if (type == DepositType.REDSTONE) {
            properties.lightLevel(state -> 0);
        }
        return properties;
    }

    private static Block zincOreBlock() {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "zinc_ore"));
        return block == Blocks.AIR ? Blocks.IRON_ORE : block;
    }
}
