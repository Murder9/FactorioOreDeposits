package com.ssaml.advancedoredeposits.registry;

import java.util.function.Supplier;

import com.ssaml.advancedoredeposits.AdvancedOreDeposits;
import com.ssaml.advancedoredeposits.worldgen.DepositPatchFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, AdvancedOreDeposits.MOD_ID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> DEPOSIT_PATCH = FEATURES.register("deposit_patch",
            () -> new DepositPatchFeature(NoneFeatureConfiguration.CODEC));

    private ModWorldgen() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
