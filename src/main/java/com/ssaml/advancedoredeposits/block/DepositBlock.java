package com.ssaml.advancedoredeposits.block;

import java.util.List;
import java.util.function.BiConsumer;

import com.mojang.serialization.MapCodec;
import com.ssaml.advancedoredeposits.AdvancedOreDeposits;
import com.ssaml.advancedoredeposits.DepositType;
import com.ssaml.advancedoredeposits.ModConfig;
import com.ssaml.advancedoredeposits.registry.ModBlocks;
import com.ssaml.advancedoredeposits.registry.ModItems;
import com.ssaml.advancedoredeposits.worldgen.DepositChunkLoader;
import com.ssaml.advancedoredeposits.worldgen.DepositSyncEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class DepositBlock extends BaseEntityBlock {
    private static final ThreadLocal<BreakKind> BREAK_KIND = ThreadLocal.withInitial(() -> BreakKind.AUTOMATION);
    private static final TagKey<Item> WRENCHES =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    private final DepositType type;
    private final MapCodec<DepositBlock> codec;

    public DepositBlock(DepositType type, BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
        this.codec = simpleCodec(props -> new DepositBlock(type, props));
    }

    public DepositType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return AdvancedOreDeposits.createDepositBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && !movedByPiston) {
            BreakKind breakKind = BREAK_KIND.get();
            BREAK_KIND.set(BreakKind.AUTOMATION);
            if (breakKind == BreakKind.CREATIVE) {
                super.onRemove(state, level, pos, newState, movedByPiston);
                return;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DepositBlockEntity deposit) {
                boolean canDrop = deposit.remainingBlocks() > 0;
                if (canDrop && level instanceof ServerLevel serverLevel) {
                    DepositChunkLoader.forceAround(serverLevel, pos);
                }
                DepositBlockEntity.DepositSnapshot snapshotBeforeMining = deposit.snapshot();
                if (canDrop && breakKind == BreakKind.AUTOMATION
                        && ModConfig.ONLY_DROP_IF_MINED_BY_PLAYER.getAsBoolean()
                        && !tryDeliverAutomationDrop(level, pos, chunkStack())) {
                    restoreDeposit(level, pos, state, snapshotBeforeMining, breakKind);
                    return;
                }

                if (canDrop && breakKind == BreakKind.PLAYER) {
                    Block.popResource(level, pos.above(), chunkStack());
                } else if (canDrop && breakKind == BreakKind.AUTOMATION
                        && !ModConfig.ONLY_DROP_IF_MINED_BY_PLAYER.getAsBoolean()) {
                    deliverAutomationDrop(level, pos, chunkStack());
                }

                boolean shouldRestore = deposit.consumeMineCycle();
                if (shouldRestore) {
                    DepositBlockEntity.DepositSnapshot snapshot = deposit.snapshot();
                    restoreDeposit(level, pos, state, snapshot, breakKind);
                    return;
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BREAK_KIND.set(player.isCreative() ? BreakKind.CREATIVE : BreakKind.PLAYER);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown() || !isWrench(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            removeByWrench(level, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion,
            BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (!state.isAir() && explosion.getBlockInteraction() != Explosion.BlockInteraction.TRIGGER_BLOCK) {
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            if (blockEntity instanceof DepositBlockEntity deposit && deposit.remainingBlocks() > 0
                    && state.canDropFromExplosion(level, pos, explosion)) {
                dropConsumer.accept(chunkStack(), pos.above());
            }

            BREAK_KIND.set(BreakKind.EXPLOSION);
            state.onBlockExploded(level, pos, explosion);
            BREAK_KIND.set(BreakKind.AUTOMATION);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.advanced_ore_deposits.deposit"));
    }

    private ItemStack chunkStack() {
        return new ItemStack(ModItems.rawChunk(type), ModConfig.CHUNK_DROP_COUNT.getAsInt());
    }

    private ItemStack sourceOreStack() {
        return new ItemStack(ModBlocks.sourceBlock(type));
    }

    public void removeByWrench(Level level, BlockPos pos) {
        try {
            BREAK_KIND.set(BreakKind.CREATIVE);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL | UPDATE_SUPPRESS_DROPS);
        } finally {
            BREAK_KIND.set(BreakKind.AUTOMATION);
        }
        Block.popResource(level, pos.above(), sourceOreStack());
    }

    public static boolean isWrench(ItemStack stack) {
        if (stack.is(WRENCHES)) {
            return true;
        }
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ResourceLocation.fromNamespaceAndPath("create", "wrench").equals(itemId)
                || stack.getItem().getClass().getName().endsWith(".WrenchItem");
    }

    private static void deliverAutomationDrop(Level level, BlockPos pos, ItemStack stack) {
        ItemStack remainder = tryDeliverAutomationRemainder(level, pos, stack);
        if (!remainder.isEmpty() && level instanceof ServerLevel serverLevel) {
            Block.popResource(serverLevel, pos.above(), remainder);
        }
    }

    private static boolean tryDeliverAutomationDrop(Level level, BlockPos pos, ItemStack stack) {
        return tryDeliverAutomationRemainder(level, pos, stack).isEmpty();
    }

    private static ItemStack tryDeliverAutomationRemainder(Level level, BlockPos pos, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return stack;
        }

        ItemStack remainder = stack;
        for (int i = 1; i <= 3 && !remainder.isEmpty(); i++) {
            remainder = tryInsert(serverLevel, pos.above(i), remainder, Direction.DOWN);
        }
        if (!remainder.isEmpty()) {
            remainder = tryInsert(serverLevel, pos.below(), remainder, Direction.UP);
        }
        return remainder;
    }

    private static void restoreDeposit(Level level, BlockPos pos, BlockState state,
            DepositBlockEntity.DepositSnapshot snapshot, BreakKind breakKind) {
        level.setBlock(pos, state, UPDATE_ALL | UPDATE_SUPPRESS_DROPS);
        BlockEntity restored = level.getBlockEntity(pos);
        if (restored instanceof DepositBlockEntity restoredDeposit) {
            restoredDeposit.applySnapshot(snapshot);
            if (level instanceof ServerLevel serverLevel) {
                DepositSyncEvents.queue(serverLevel, pos, breakKind == BreakKind.PLAYER);
            }
        }
    }

    private static ItemStack tryInsert(ServerLevel level, BlockPos pos, ItemStack stack, Direction preferredSide) {
        ItemStack remainder = insertInto(level, pos, preferredSide, stack);
        if (remainder.isEmpty()) {
            return remainder;
        }
        for (Direction direction : Direction.values()) {
            if (direction == preferredSide) {
                continue;
            }
            remainder = insertInto(level, pos, direction, remainder);
            if (remainder.isEmpty()) {
                return remainder;
            }
        }
        return insertInto(level, pos, null, remainder);
    }

    private static ItemStack insertInto(ServerLevel level, BlockPos pos, @Nullable Direction side, ItemStack stack) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (handler == null) {
            return stack;
        }

        ItemStack remainder = stack;
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    private enum BreakKind {
        PLAYER,
        CREATIVE,
        EXPLOSION,
        AUTOMATION
    }
}
