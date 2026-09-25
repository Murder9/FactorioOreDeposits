package com.ssaml.advancedoredeposits.compat;

import com.ssaml.advancedoredeposits.AdvancedOreDeposits;
import com.ssaml.advancedoredeposits.ModConfig;
import com.ssaml.advancedoredeposits.block.DepositBlock;
import com.ssaml.advancedoredeposits.block.DepositBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class AdvancedOreDepositsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DepositProvider.INSTANCE, DepositBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DepositProvider.INSTANCE, DepositBlock.class);
    }

    private enum DepositProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final String ORE_TYPE = "AODOreType";
        private static final String REMAINING = "AODRemainingBlocks";
        private static final String MAX = "AODMaxBlocks";

        @Override
        public ResourceLocation getUid() {
            return AdvancedOreDeposits.id("deposit");
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof DepositBlockEntity deposit) {
                data.putString(ORE_TYPE, deposit.oreType().id());
                data.putInt(REMAINING, deposit.remainingBlocks());
                data.putInt(MAX, deposit.maxBlocks());
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!ModConfig.SHOW_HUD_INFO.getAsBoolean()) {
                return;
            }
            CompoundTag data = accessor.getServerData();
            if (!data.contains(REMAINING)) {
                return;
            }
            String oreType = data.getString(ORE_TYPE);
            Component name = Component.translatable("deposit_type.advanced_ore_deposits." + oreType);
            if ("modded".equals(oreType) && accessor.getBlockEntity() instanceof DepositBlockEntity deposit) {
                name = deposit.sourceBlock().asItem().getName();
            }
            tooltip.add(Component.translatable("jade.advanced_ore_deposits.deposit", name));
            tooltip.add(Component.translatable("jade.advanced_ore_deposits.remaining", data.getInt(REMAINING),
                    data.getInt(MAX)));
        }
    }
}
