package com.ssaml.advancedoredeposits.registry;

import java.util.function.Supplier;

import com.ssaml.advancedoredeposits.AdvancedOreDeposits;
import com.ssaml.advancedoredeposits.DepositType;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdvancedOreDeposits.MOD_ID);

    public static final Supplier<CreativeModeTab> ADVANCED_ORE_DEPOSITS = CREATIVE_TABS.register("advanced_ore_deposits",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.advanced_ore_deposits"))
                    .icon(() -> ModItems.RAW_CHUNKS.get(DepositType.IRON).get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DepositType type : DepositType.values()) {
                            if (type == DepositType.ZINC && !AdvancedOreDeposits.isCreateLoaded()) {
                                continue;
                            }
                            output.accept(ModItems.DEPOSIT_ITEMS.get(type).get());
                        }
                        for (DepositType type : DepositType.values()) {
                            if (type == DepositType.ZINC && !AdvancedOreDeposits.isCreateLoaded()) {
                                continue;
                            }
                            output.accept(ModItems.RAW_CHUNKS.get(type).get());
                            if (type.usesSmeltedChunks()) {
                                output.accept(ModItems.SMELTED_CHUNKS.get(type).get());
                            }
                        }
                    }).build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
