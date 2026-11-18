package com.arcanavaria.lattice;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaStorage;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Every lattice block's state: the motes (one essentia each) crossing it, and the few settings a device has. Motes move only on the
 * server; the client gets the list whenever it changes and moves them on by itself in between, for drawing.
 */
public class LatticeBlockEntity extends BlockEntity {
    /** Ticks two packets may wait at a junction for a partner before they spill into the aura as flux. */
    public static final int WINDOW = 8;
    /** Intake periods in ticks; 0 draws once per redstone pulse. */
    public static final int[] PERIODS = {20, 40, 10, 0};
    /** Extra ticks a synchronizer holds its packet before it counts as ready. */
    public static final int[] DELAYS = {0, 2, 4, 8, 16};

    final List<Mote> packets = new ArrayList<>();
    private int period;
    private int delay;
    private int turn;
    private boolean wasPowered;
    private long releaseAt = -1;
    private int cooldown;
    private boolean dirty;

    public LatticeBlockEntity(BlockPos pos, BlockState state) {
        super(LatticeRegistry.LATTICE_BE.get(), pos, state);
    }

    public LatticeKind kind() {
        return getBlockState().getBlock() instanceof LatticePart part ? part.kind() : LatticeKind.PIPE;
    }

    public Direction facing() {
        return LatticePart.facing(getBlockState());
    }

    public List<Mote> packets() {
        return packets;
    }

    public int periodTicks() {
        return PERIODS[period];
    }

    public int delayTicks() {
        return DELAYS[delay];
    }

    int cyclePeriod() {
        period = (period + 1) % PERIODS.length;
        setChanged();
        return PERIODS[period];
    }

    int cycleDelay() {
        delay = (delay + 1) % DELAYS.length;
        setChanged();
        return DELAYS[delay];
    }

    // ---- neighbours -------------------------------------------------------------------------------------------------

    private @Nullable LatticeBlockEntity neighbour(Direction d) {
        return level != null && level.getBlockEntity(worldPosition.relative(d)) instanceof LatticeBlockEntity be ? be : null;
    }

    /** Whether this block and its neighbour on {@code d} open to each other. */
    public boolean linked(Direction d) {
        LatticeBlockEntity n = neighbour(d);
        return n != null && kind().opensTo(d, facing()) && n.kind().opensTo(d.getOpposite(), n.facing());
    }

    /** Whether a packet sent out through {@code d} could ever be taken by the neighbour there. */
    private boolean leadsTo(Direction d) {
        LatticeBlockEntity n = neighbour(d);
        return linked(d) && n.kind().takesFrom(d.getOpposite(), n.facing());
    }

    /** Takes a packet handed over from the neighbour on {@code side}; false if it can't come in now. */
    boolean accept(Direction side, ResourceKey<IAspect> aspect, long now) {
        if (!kind().takesFrom(side, facing()) || !kind().opensTo(side, facing()) || packets.size() >= kind().capacity) {
            return false;
        }
        Mote p = new Mote(aspect, side, 0);
        p.movedAt = now;
        packets.add(p);
        dirty = true;
        return true;
    }

    // ---- ticking ----------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, LatticeBlockEntity be) {
        be.tick((ServerLevel) level);
    }

    /** The client moves packets on between updates, so they glide instead of jumping block to block. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, LatticeBlockEntity be) {
        for (Mote p : be.packets) {
            if (p.age < Mote.CENTRE || p.to != null && p.age < Mote.TRAVEL) {
                p.age++;
            } else if (p.to == null) {
                p.held++;
            }
        }
    }

    private void tick(ServerLevel level) {
        long now = level.getGameTime();
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (kind() == LatticeKind.INTAKE) {
            intake(level, now, powered);
        }
        wasPowered = powered;
        for (Mote p : List.copyOf(packets)) {
            if (p.movedAt == now || !packets.contains(p)) {
                continue;
            }
            p.movedAt = now;
            if (p.age < Mote.CENTRE) {
                p.age++;
            } else if (p.to == null) {
                p.held++;
                atCentre(level, p, now, powered);
            } else if (p.age < Mote.TRAVEL) {
                p.age++;
            } else if (!leadsTo(p.to)) {
                // the way it was sent is gone (a pipe broken, nothing at a prism's side): it spills
                vent(level, p);
            } else {
                LatticeBlockEntity n = neighbour(p.to);
                if (n != null && n.accept(p.to.getOpposite(), p.aspect, now)) {
                    packets.remove(p);
                    dirty = true;
                    n.flush();
                }
            }
        }
        if (kind() == LatticeKind.JUNCTION) {
            junction(level, now);
        }
        flush();
    }

    /** Sends the packet list to clients if it changed this tick. */
    private void flush() {
        if (dirty && level != null) {
            dirty = false;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private void send(Mote p, @Nullable Direction to) {
        p.to = to;
        dirty = true;
    }

    private void atCentre(ServerLevel level, Mote p, long now, boolean powered) {
        switch (kind()) {
            case PIPE -> {
                Direction out = route(p.from);
                if (out == null) {
                    vent(level, p);
                } else {
                    send(p, out);
                }
            }
            case INTAKE -> send(p, route(facing()));
            case OUTLET -> {
                if (fill(level, p.aspect)) {
                    packets.remove(p);
                    dirty = true;
                }
            }
            case JUNCTION -> {
                // paired up in junction(), after every packet has moved this tick
            }
            case PRISM -> {
                List<ResourceKey<IAspect>> parents = AspectChemistry.split(level, p.aspect);
                if (parents.isEmpty()) {
                    send(p, facing());
                } else {
                    packets.remove(p);
                    packets.add(Mote.born(parents.get(0), LatticeKind.left(facing()), now));
                    packets.add(Mote.born(parents.get(1), LatticeKind.right(facing()), now));
                    dirty = true;
                    level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.4f);
                }
            }
            case SYNCHRONIZER -> synchronize(level, p, now, powered);
        }
    }

    /**
     * The face a packet that came in through {@code from} leaves by: straight on if it can, else the next exit in turn.
     * Null if there is none.
     */
    private @Nullable Direction route(@Nullable Direction from) {
        if (from != null && leadsTo(from.getOpposite())) {
            return from.getOpposite();
        }
        List<Direction> exits = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (d != from && leadsTo(d)) {
                exits.add(d);
            }
        }
        if (exits.isEmpty()) {
            return null;
        }
        turn = (turn + 1) % exits.size();
        return exits.get(turn);
    }

    /** A packet with nowhere to go spills into the aura as flux. */
    private void vent(ServerLevel level, Mote p) {
        packets.remove(p);
        dirty = true;
        AuraHelper.polluteAura(level, worldPosition, 1f, true);
        level.sendParticles(ParticleTypes.WITCH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5,
                6, 0.15, 0.15, 0.15, 0.02);
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.6f);
    }

    // ---- devices ----------------------------------------------------------------------------------------------------

    private void intake(ServerLevel level, long now, boolean powered) {
        if (cooldown > 0) {
            cooldown--;
        }
        int every = PERIODS[period];
        boolean pulse = every == 0 ? powered && !wasPowered : cooldown <= 0 && !powered;
        if (!pulse || packets.size() >= kind().capacity) {
            return;
        }
        Direction out = route(facing());
        ResourceKey<IAspect> drawn = out == null ? null : draw(level);
        if (drawn != null) {
            Mote p = new Mote(drawn, facing(), Mote.CENTRE);
            p.movedAt = now;
            p.to = out;
            packets.add(p);
            dirty = true;
            cooldown = every;
        }
    }

    /** Takes one essentia from the store the intake faces: through its storage, else as a tube would. */
    private @Nullable ResourceKey<IAspect> draw(ServerLevel level) {
        BlockPos at = worldPosition.relative(facing());
        Direction side = facing().getOpposite();
        IEssentiaStorage storage = level.getCapability(EssentiaCapabilities.STORAGE, at, side);
        if (storage != null) {
            for (AspectInstance a : storage.contents().entries()) {
                try (Transaction tx = Transaction.openRoot()) {
                    if (storage.extract(a.aspect(), 1, tx) == 1) {
                        tx.commit();
                        return a.aspect().unwrapKey().orElse(null);
                    }
                }
            }
            return null;
        }
        IEssentiaTransport tube = level.getCapability(EssentiaCapabilities.TRANSPORT, at, side);
        if (tube != null && tube.canOutputTo(side)) {
            Holder<IAspect> type = tube.getEssentiaType(side);
            if (type != null && tube.getEssentiaAmount(side) > 0 && tube.takeEssentia(type, 1, side) == 1) {
                return type.unwrapKey().orElse(null);
            }
        }
        return null;
    }

    /** Puts one essentia into the store the outlet faces; false if it won't take it now. */
    private boolean fill(ServerLevel level, ResourceKey<IAspect> aspect) {
        BlockPos at = worldPosition.relative(facing());
        Direction side = facing().getOpposite();
        Holder<IAspect> h = AspectChemistry.registry(level).get(aspect).orElse(null);
        if (h == null) {
            return false;
        }
        IEssentiaStorage storage = level.getCapability(EssentiaCapabilities.STORAGE, at, side);
        if (storage != null) {
            try (Transaction tx = Transaction.openRoot()) {
                if (storage.insert(h, 1, tx) == 1) {
                    tx.commit();
                    return true;
                }
            }
            return false;
        }
        IEssentiaTransport tube = level.getCapability(EssentiaCapabilities.TRANSPORT, at, side);
        return tube != null && tube.canInputFrom(side) && tube.addEssentia(h, 1, side) == 1;
    }

    /**
     * Pairs packets waiting at the centre that came in through different faces and have a compound; the first pair
     * found (oldest first) combines and leaves by the output face. Packets past the window spill as flux.
     */
    private void junction(ServerLevel level, long now) {
        List<Mote> waiting = packets.stream().filter(Mote::waiting).toList();
        for (int i = 0; i < waiting.size(); i++) {
            Mote a = waiting.get(i);
            for (int j = i + 1; j < waiting.size(); j++) {
                Mote b = waiting.get(j);
                if (a.from == b.from || !packets.contains(a) || !packets.contains(b)) {
                    continue;
                }
                ResourceKey<IAspect> made = AspectChemistry.combine(level, a.aspect, b.aspect);
                if (made != null && leadsTo(facing())) {
                    packets.remove(a);
                    packets.remove(b);
                    packets.add(Mote.born(made, facing(), now));
                    dirty = true;
                    level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5f, 1.5f);
                    level.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                            worldPosition.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.01);
                    break;
                }
            }
        }
        for (Mote p : List.copyOf(packets)) {
            if (p.waiting() && p.held > WINDOW) {
                vent(level, p);
            }
        }
    }

    /**
     * A synchronizer's packet is ready once it has waited its delay. When every synchronizer in its row is ready (and
     * none is powered), all of them let go together on the next tick.
     */
    private void synchronize(ServerLevel level, Mote p, long now, boolean powered) {
        if (releaseAt >= 0 && now >= releaseAt) {
            releaseAt = -1;
            send(p, facing());
            return;
        }
        if (powered || releaseAt >= 0 || p.held < DELAYS[delay]) {
            return;
        }
        List<LatticeBlockEntity> row = row(level);
        for (LatticeBlockEntity s : row) {
            if (s.packets.stream().noneMatch(q -> q.waiting() && q.held >= DELAYS[s.delay])
                    || level.hasNeighborSignal(s.worldPosition)) {
                return;
            }
        }
        for (LatticeBlockEntity s : row) {
            s.releaseAt = now + 1;
        }
    }

    /** Synchronizers facing the same way, side by side with this one (up to 16). */
    private List<LatticeBlockEntity> row(Level level) {
        List<LatticeBlockEntity> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>(List.of(worldPosition));
        while (!todo.isEmpty() && out.size() < 16) {
            BlockPos at = todo.poll();
            if (!seen.add(at) || !(level.getBlockEntity(at) instanceof LatticeBlockEntity s)
                    || s.kind() != LatticeKind.SYNCHRONIZER || s.facing() != facing()) {
                continue;
            }
            out.add(s);
            for (Direction d : Direction.values()) {
                if (d.getAxis() != facing().getAxis()) {
                    todo.add(at.relative(d));
                }
            }
        }
        return out;
    }

    /** Motes still inside when the block breaks spill as flux. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && !packets.isEmpty()) {
            AuraHelper.polluteAura(server, pos, packets.size(), true);
            packets.clear();
        }
    }

    // ---- saving and syncing -----------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("packets", Mote.CODEC.listOf(), packets);
        out.putInt("period", period);
        out.putInt("delay", delay);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        packets.clear();
        in.read("packets", Mote.CODEC.listOf()).ifPresent(packets::addAll);
        period = Math.floorMod(in.getIntOr("period", 0), PERIODS.length);
        delay = Math.floorMod(in.getIntOr("delay", 0), DELAYS.length);
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
