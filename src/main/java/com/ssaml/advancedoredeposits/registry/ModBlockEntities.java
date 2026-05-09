package com.ssaml.advancedoredeposits.registry;

import java.util.function.Supplier;

import com.ssaml.advancedoredeposits.AdvancedOreDeposits;
import com.ssaml.advancedoredeposits.block.DepositBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AdvancedOreDeposits.MOD_ID);

    public static final Supplier<BlockEntityType<DepositBlockEntity>> DEPOSIT = BLOCK_ENTITY_TYPES.register("deposit",
            () -> BlockEntityType.Builder.of(AdvancedOreDeposits::createDepositBlockEntity,
                    ModBlocks.depositBlockArray()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
