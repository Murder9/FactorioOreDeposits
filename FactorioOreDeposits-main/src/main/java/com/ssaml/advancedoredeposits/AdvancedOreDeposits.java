package com.ssaml.advancedoredeposits;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.ssaml.advancedoredeposits.block.DepositBlockEntity;
import com.ssaml.advancedoredeposits.client.AdvancedOreDepositsClient;
import com.ssaml.advancedoredeposits.registry.ModBlockEntities;
import com.ssaml.advancedoredeposits.registry.ModBlocks;
import com.ssaml.advancedoredeposits.registry.ModCreativeTabs;
import com.ssaml.advancedoredeposits.registry.ModItems;
import com.ssaml.advancedoredeposits.registry.ModWorldgen;
import com.ssaml.advancedoredeposits.worldgen.DepositChunkLoader;
import com.ssaml.advancedoredeposits.worldgen.DepositInteractionEvents;
import com.ssaml.advancedoredeposits.worldgen.DepositSyncEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AdvancedOreDeposits.MOD_ID)
public final class AdvancedOreDeposits {
    public static final String MOD_ID = "advanced_ore_deposits";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdvancedOreDeposits(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModWorldgen.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(DepositChunkLoader::onServerTick);
        NeoForge.EVENT_BUS.addListener(DepositSyncEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, DepositInteractionEvents::onRightClickBlock);

        modContainer.registerConfig(Type.COMMON, ModConfig.SPEC);
        if (FMLEnvironment.dist.isClient()) {
            AdvancedOreDepositsClient.registerConfigScreen(modContainer);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded("create");
    }

    public static boolean isJadeLoaded() {
        return ModList.get().isLoaded("jade");
    }

    public static DepositBlockEntity createDepositBlockEntity(BlockPos pos, BlockState state) {
        if (isCreateLoaded()) {
            try {
                Class<?> compatClass =
                        Class.forName("com.ssaml.advancedoredeposits.compat.CreateDepositBlockEntity");
                return (DepositBlockEntity) compatClass.getConstructor(BlockPos.class, BlockState.class)
                        .newInstance(pos, state);
            } catch (ReflectiveOperationException | LinkageError exception) {
                LOGGER.warn("Failed to create Create-compatible deposit block entity. Using base deposit block entity.",
                        exception);
            }
        }
        return new DepositBlockEntity(pos, state);
    }
}
