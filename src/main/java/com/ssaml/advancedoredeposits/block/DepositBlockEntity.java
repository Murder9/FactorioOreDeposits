package com.ssaml.advancedoredeposits.block;

import com.ssaml.advancedoredeposits.DepositType;
import com.ssaml.advancedoredeposits.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DepositBlockEntity extends BlockEntity {
    private DepositType oreType;
    private int remainingBlocks = 4096;
    private int maxBlocks = 4096;
    private int patchDiameter = 16;
    private long patchId;

    public DepositBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEPOSIT.get(), pos, state);
        if (state.getBlock() instanceof DepositBlock depositBlock) {
            this.oreType = depositBlock.type();
        } else {
            this.oreType = DepositType.IRON;
        }
    }

    public DepositType oreType() {
        return oreType;
    }

    public int remainingBlocks() {
        return remainingBlocks;
    }

    public int maxBlocks() {
        return maxBlocks;
    }

    public int patchDiameter() {
        return patchDiameter;
    }

    public long patchId() {
        return patchId;
    }

    public void initialize(DepositType oreType, int remainingBlocks, int maxBlocks, int patchDiameter, long patchId) {
        this.oreType = oreType;
        this.remainingBlocks = Math.max(0, remainingBlocks);
        this.maxBlocks = Math.max(this.remainingBlocks, maxBlocks);
        this.patchDiameter = Math.max(1, patchDiameter);
        this.patchId = patchId;
        setChanged();
    }

    public boolean consumeMineCycle() {
        if (remainingBlocks <= 0) {
            return false;
        }
        remainingBlocks--;
        setChanged();
        sync();
        return remainingBlocks > 0;
    }

    public DepositSnapshot snapshot() {
        return new DepositSnapshot(oreType, remainingBlocks, maxBlocks, patchDiameter, patchId);
    }

    public void applySnapshot(DepositSnapshot snapshot) {
        this.oreType = snapshot.oreType();
        this.remainingBlocks = snapshot.remainingBlocks();
        this.maxBlocks = snapshot.maxBlocks();
        this.patchDiameter = snapshot.patchDiameter();
        this.patchId = snapshot.patchId();
        setChanged();
        sync();
    }

    public void sync() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_ALL);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(getBlockPos());
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("OreType", oreType.id());
        tag.putInt("RemainingBlocks", remainingBlocks);
        tag.putInt("MaxBlocks", maxBlocks);
        tag.putInt("PatchDiameter", patchDiameter);
        tag.putLong("PatchId", patchId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        DepositType.byId(tag.getString("OreType")).ifPresent(type -> this.oreType = type);
        this.remainingBlocks = tag.getInt("RemainingBlocks");
        this.maxBlocks = tag.getInt("MaxBlocks");
        this.patchDiameter = tag.getInt("PatchDiameter");
        this.patchId = tag.getLong("PatchId");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
    }

    public record DepositSnapshot(DepositType oreType, int remainingBlocks, int maxBlocks, int patchDiameter,
            long patchId) {
    }
}
