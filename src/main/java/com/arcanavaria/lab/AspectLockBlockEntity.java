package com.arcanavaria.lab;

import com.arcanavaria.thurible.ThuribleEffects;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * An aspect lock: wants {@code need} essentia of one aspect (both set by the structure), from a lattice outlet, a tube,
 * a phial or a Thurible. Full, it opens (lit) and gives a redstone signal on every side until the ward resets.
 */
public class AspectLockBlockEntity extends BlockEntity implements Resettable {
    public static final ResourceKey<IAspect> DEFAULT = ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", "ordo"));

    private ResourceKey<IAspect> aspect = DEFAULT;
    private int need = 10;
    private int have;
    private final Journal journal = new Journal();
    public final IEssentiaStorage storage = new Storage();

    public AspectLockBlockEntity(BlockPos pos, BlockState state) {
        super(LabRegistry.ASPECT_LOCK_BE.get(), pos, state);
    }

    public ResourceKey<IAspect> aspect() {
        return aspect;
    }

    public int need() {
        return need;
    }

    public int have() {
        return have;
    }

    public boolean open() {
        return have >= need;
    }

    /** Pours in up to {@code amount} of {@code key}; returns how much it took. */
    public int pour(ResourceKey<IAspect> key, int amount) {
        if (!key.equals(aspect) || open()) {
            return 0;
        }
        int took = Math.min(amount, need - have);
        have += took;
        changed();
        return took;
    }

    private void changed() {
        setChanged();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockState s = getBlockState();
        boolean lit = open();
        if (s.getValue(AspectLockBlock.LIT) != lit) {
            server.setBlock(worldPosition, s.setValue(AspectLockBlock.LIT, lit), Block.UPDATE_ALL);
            if (lit) {
                Vec3 c = worldPosition.getCenter();
                server.playSound(null, worldPosition, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
                server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 0.8F);
                server.sendParticles(new DustParticleOptions(ThuribleEffects.colour(server.registryAccess(), aspect), 1.4F),
                        c.x, c.y, c.z, 40, 0.45, 0.45, 0.45, 0.02);
            }
        } else {
            server.sendBlockUpdated(worldPosition, s, s, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void labReset() {
        have = 0;
        changed();
    }

    private final class Journal extends SnapshotJournal<Integer> {
        @Override
        protected Integer createSnapshot() {
            return have;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            have = snapshot;
        }

        @Override
        protected void onRootCommit(Integer original) {
            if (original != have) {
                changed();
            }
        }
    }

    private final class Storage implements IEssentiaStorage {
        @Override
        public AspectList contents() {
            if (have == 0 || level == null) {
                return AspectList.EMPTY;
            }
            return level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).get(aspect)
                    .map(h -> AspectList.of(new AspectInstance(h, have))).orElse(AspectList.EMPTY);
        }

        @Override
        public int insert(Holder<IAspect> h, int amount, TransactionContext tx) {
            if (!h.is(aspect) || open()) {
                return 0;
            }
            int took = Math.min(amount, need - have);
            journal.updateSnapshots(tx);
            have += took;
            return took;
        }

        @Override
        public int extract(Holder<IAspect> h, int amount, TransactionContext tx) {
            return 0;
        }

        @Override
        public long contentRevision() {
            return have;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("aspect", ResourceKey.codec(IAspect.REGISTRY_KEY), aspect);
        out.putInt("need", need);
        out.putInt("have", have);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        aspect = in.read("aspect", ResourceKey.codec(IAspect.REGISTRY_KEY)).orElse(DEFAULT);
        need = Math.max(1, in.getIntOr("need", 10));
        have = in.getIntOr("have", 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
