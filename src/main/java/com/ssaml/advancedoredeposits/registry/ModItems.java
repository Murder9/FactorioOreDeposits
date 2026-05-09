package com.ssaml.advancedoredeposits.registry;

import java.util.EnumMap;
import java.util.Map;

import com.ssaml.advancedoredeposits.AdvancedOreDeposits;
import com.ssaml.advancedoredeposits.DepositType;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AdvancedOreDeposits.MOD_ID);
    public static final Map<DepositType, DeferredItem<BlockItem>> DEPOSIT_ITEMS = new EnumMap<>(DepositType.class);
    public static final Map<DepositType, DeferredItem<Item>> RAW_CHUNKS = new EnumMap<>(DepositType.class);
    public static final Map<DepositType, DeferredItem<Item>> SMELTED_CHUNKS = new EnumMap<>(DepositType.class);

    static {
        for (DepositType type : DepositType.values()) {
            DEPOSIT_ITEMS.put(type, ITEMS.register(type.depositBlockId(),
                    () -> new BlockItem(ModBlocks.deposit(type), new Item.Properties())));
            RAW_CHUNKS.put(type, ITEMS.registerSimpleItem(type.rawChunkId(), new Item.Properties()));
            if (type.usesSmeltedChunks()) {
                SMELTED_CHUNKS.put(type, ITEMS.registerSimpleItem(type.smeltedChunkId(), new Item.Properties()));
            }
        }
    }

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    public static Item rawChunk(DepositType type) {
        return RAW_CHUNKS.get(type).get();
    }

    public static Item smeltedChunk(DepositType type) {
        return SMELTED_CHUNKS.get(type).get();
    }
}
