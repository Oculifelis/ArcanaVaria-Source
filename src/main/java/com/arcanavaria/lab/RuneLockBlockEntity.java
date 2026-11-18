package com.arcanavaria.lab;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A rune lock: a sequence of glyphs (set by the structure, the clue is in the room). Every rune stone pressed nearby
 * tells it; the right one moves it on (it shows how far as lit pips), a wrong one starts it over. Done, it opens and
 * gives a redstone signal until the ward resets.
 */
public class RuneLockBlockEntity extends BlockEntity implements Resettable {
    /** Where the lock stands, derived from {@code progress}: nothing right yet, part of the sequence, or open. */
    public enum LockState { LOCKED, OPENING, OPEN }

    private List<Integer> sequence = List.of(0, 1, 2, 3);
    /** Runes pressed right so far, always 0..sequence.size(); changed only through {@link #setProgress}. */
    private int progress;

    public RuneLockBlockEntity(BlockPos pos, BlockState state) {
        super(LabRegistry.RUNE_LOCK_BE.get(), pos, state);
    }

    public LockState state() {
        return progress >= sequence.size() ? LockState.OPEN : progress == 0 ? LockState.LOCKED : LockState.OPENING;
    }

    /** The one place progress changes; clamps it to the sequence. */
    private void setProgress(int value) {
        progress = Math.max(0, Math.min(sequence.size(), value));
    }

    public void press(ServerLevel level, int rune) {
        if (state() == LockState.OPEN) {
            return;
        }
        Vec3 c = worldPosition.getCenter();
        if (sequence.get(progress) == rune) {
            setProgress(progress + 1);
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F,
                    0.7F + 0.15F * progress);
            if (state() == LockState.OPEN) {
                level.playSound(null, worldPosition, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
                level.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 0.6, c.z, 50, 0.4, 0.4, 0.4, 0.6);
            }
        } else {
            setProgress(0);
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
            level.sendParticles(ParticleTypes.SMOKE, c.x, c.y + 0.6, c.z, 12, 0.3, 0.2, 0.3, 0.01);
        }
        show(level);
    }

    private void show(ServerLevel level) {
        int pips = sequence.isEmpty() ? 0 : Math.round(4F * progress / sequence.size());
        BlockState s = getBlockState().setValue(RuneLockBlock.PIPS, pips).setValue(RuneLockBlock.LIT, state() == LockState.OPEN);
        if (s != getBlockState()) {
            level.setBlock(worldPosition, s, Block.UPDATE_ALL);
        }
        setChanged();
    }

    @Override
    public void labReset() {
        setProgress(0);
        if (level instanceof ServerLevel server) {
            show(server);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("sequence", Codec.intRange(0, 7).listOf(), sequence);
        out.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        sequence = in.read("sequence", Codec.intRange(0, 7).listOf()).filter(l -> !l.isEmpty()).orElse(List.of(0, 1, 2, 3));
        setProgress(in.getIntOr("progress", 0));
    }

    /** Saved fields in one line, for the save/load round-trip self-test. */
    public String snapshot() {
        return "sequence=" + sequence + " progress=" + progress + " state=" + state();
    }

    public void selfTestSet(List<Integer> sequence, int progress) {
        this.sequence = List.copyOf(sequence);
        setProgress(progress);
    }
}
