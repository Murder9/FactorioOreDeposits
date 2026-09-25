package com.ssaml.advancedoredeposits.compat;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.ssaml.advancedoredeposits.ModConfig;
import com.ssaml.advancedoredeposits.block.DepositBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class CreateDepositBlockEntity extends DepositBlockEntity implements IHaveGoggleInformation {
    public CreateDepositBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!ModConfig.SHOW_HUD_INFO.getAsBoolean()) {
            return false;
        }

        Component name = Component.translatable("deposit_type.advanced_ore_deposits." + oreType().id());
        if (oreType() == com.ssaml.advancedoredeposits.DepositType.MODDED) {
            name = sourceBlock().asItem().getName();
        }
        tooltip.add(Component.translatable("create_goggles.advanced_ore_deposits.deposit", name)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("create_goggles.advanced_ore_deposits.remaining", remainingBlocks(),
                maxBlocks()).withStyle(ChatFormatting.GOLD));
        return true;
    }

    @Override
    public ItemStack getIcon(boolean isPlayerSneaking) {
        return ItemStack.EMPTY;
    }
}
