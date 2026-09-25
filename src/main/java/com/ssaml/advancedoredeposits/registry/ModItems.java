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

    static {
        for (DepositType type : DepositType.values()) {
            DEPOSIT_ITEMS.put(type, ITEMS.register(type.depositBlockId(),
                    () -> new BlockItem(ModBlocks.deposit(type), new Item.Properties())));
        }
    }

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
