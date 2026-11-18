package com.arcanavaria.lab;

import static com.arcanavaria.ArcanaVaria.MODID;

import com.arcanavaria.lab.boss.GuardianAltarBlockEntity;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The heart of a sealed tower. Its box (min/max, relative to the core) is set by the structure that places it, with the
 * spot a pulled-back player lands on (entry) and where the dead respawn (exit). The first survival player to step
 * inside seals it: nobody sealed can leave, fly, glide or teleport, and the shell cannot be broken or built on (blocks
 * in #arcanavaria:lab_mutable excepted) until the guardian at its altar dies. If everyone sealed dies or stays away for
 * a minute, the tower resets: puzzles re-arm, the guardian goes back to sleep, the ward is dormant again.
 */
public class WardCoreBlockEntity extends BlockEntity {
    private static final Map<ResourceKey<Level>, Set<WardCoreBlockEntity>> LOADED = new ConcurrentHashMap<>();
    private static final int AWAY_RESET = 1200;
    private static final DustParticleOptions SHIMMER = new DustParticleOptions(0x9A7BFF, 1.0F);

    private BlockPos min = new BlockPos(-8, -1, -8);
    private BlockPos max = new BlockPos(8, 24, 8);
    private BlockPos entry = new BlockPos(0, 1, 2);
    private BlockPos exit = new BlockPos(0, 0, -12);
    private final Set<UUID> sealed = new LinkedHashSet<>();
    private int away;

    public WardCoreBlockEntity(BlockPos pos, BlockState state) {
        super(LabRegistry.WARD_CORE_BE.get(), pos, state);
    }

    // ---- lookups ----------------------------------------------------------------------------------------------------

    /** The ward still standing (dormant or sealed) whose box holds {@code pos}, if its core is loaded. */
    public static @Nullable WardCoreBlockEntity guarding(Level level, BlockPos pos) {
        for (WardCoreBlockEntity w : LOADED.getOrDefault(level.dimension(), Set.of())) {
            if (w.state() != WardState.CLEARED && w.box().contains(Vec3.atCenterOf(pos))) {
                return w;
            }
        }
        return null;
    }

    /** Any ward (in any state) whose box holds {@code pos}. */
    public static @Nullable WardCoreBlockEntity around(Level level, BlockPos pos) {
        for (WardCoreBlockEntity w : LOADED.getOrDefault(level.dimension(), Set.of())) {
            if (w.box().contains(Vec3.atCenterOf(pos))) {
                return w;
            }
        }
        return null;
    }

    public static @Nullable WardCoreBlockEntity of(ServerPlayer p) {
        WardLink link = p.getExistingDataOrNull(LabRegistry.WARD_LINK.get());
        if (link == null) {
            return null;
        }
        ServerLevel level = p.level().getServer().getLevel(link.core().dimension());
        return level != null && level.getBlockEntity(link.core().pos()) instanceof WardCoreBlockEntity w ? w : null;
    }

    public static boolean isSealed(ServerPlayer p) {
        WardCoreBlockEntity w = of(p);
        return w != null && w.state() == WardState.SEALED && w.sealed.contains(p.getUUID());
    }

    public WardState state() {
        return getBlockState().getValue(WardCoreBlock.WARD);
    }

    public AABB box() {
        return new AABB(Vec3.atLowerCornerOf(worldPosition.offset(min)), Vec3.atLowerCornerOf(worldPosition.offset(max).offset(1, 1, 1)));
    }

    public Vec3 entry() {
        return Vec3.atBottomCenterOf(worldPosition.offset(entry));
    }

    public Vec3 exit() {
        return Vec3.atBottomCenterOf(worldPosition.offset(exit));
    }

    // ---- life -------------------------------------------------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            LOADED.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        forget();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        forget();
    }

    private void forget() {
        if (level != null && !level.isClientSide()) {
            LOADED.getOrDefault(level.dimension(), Set.of()).remove(this);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WardCoreBlockEntity be) {
        if (level.getGameTime() % 10 == 0 && level instanceof ServerLevel server) {
            be.watch(server);
        }
    }

    private void watch(ServerLevel level) {
        WardState s = state();
        if (s == WardState.CLEARED) {
            return;
        }
        AABB box = box();
        List<ServerPlayer> inside = level.getPlayers(p -> !p.isSpectator() && !p.isCreative() && p.isAlive() && box.contains(p.position()));
        if (s == WardState.DORMANT) {
            if (!inside.isEmpty()) {
                seal(level, inside);
            }
            return;
        }
        for (ServerPlayer p : inside) {
            if (!sealed.contains(p.getUUID())) {
                join(level, p);
            }
        }
        int here = 0;
        for (UUID id : sealed) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p == null || p.level() != level || !p.isAlive()) {
                continue;
            }
            here++;
            hold(level, p, box);
        }
        away = here > 0 ? 0 : away + 10;
        if (away >= AWAY_RESET) {
            reset(level);
        }
    }

    /** Keeps a sealed player in: back inside if they got out, out of the air if they flew or glided. */
    private void hold(ServerLevel level, ServerPlayer p, AABB box) {
        if (!box.contains(p.position())) {
            Vec3 at = entry();
            p.teleportTo(at.x, at.y, at.z);
            p.sendOverlayMessage(Component.translatable("message.%s.ward.held".formatted(MODID)));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SHULKER_TELEPORT, SoundSource.BLOCKS, 0.8F, 0.7F);
        }
        if (p.isFallFlying()) {
            p.stopFallFlying();
        }
        if (p.getAbilities().flying && !p.isCreative()) {
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
    }

    private void seal(ServerLevel level, List<ServerPlayer> players) {
        level.setBlock(worldPosition, getBlockState().setValue(WardCoreBlock.WARD, WardState.SEALED), Block.UPDATE_ALL);
        away = 0;
        players.forEach(p -> join(level, p));
        Vec3 c = worldPosition.getCenter();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 0.5, c.z, 60, 0.6, 0.6, 0.6, 0.1);
        LabScan.in(level, box(), GuardianAltarBlockEntity.class).forEach(GuardianAltarBlockEntity::arm);
        setChanged();
    }

    private void join(ServerLevel level, ServerPlayer p) {
        sealed.add(p.getUUID());
        p.setData(LabRegistry.WARD_LINK.get(), new WardLink(GlobalPos.of(level.dimension(), worldPosition)));
        p.sendSystemMessage(Component.translatable("message.%s.ward.sealed".formatted(MODID)));
        setChanged();
    }

    /** A sealed player died (or came back to a ward that reset): they are out, and the ward resets if no one is left. */
    public void leave(ServerLevel level, UUID id) {
        if (sealed.remove(id) && sealed.isEmpty() && state() == WardState.SEALED) {
            reset(level);
        }
        setChanged();
    }

    public boolean holds(UUID id) {
        return state() == WardState.SEALED && sealed.contains(id);
    }

    /** The guardian died: the ward falls for good and everyone sealed is free. */
    public void clear(ServerLevel level) {
        level.setBlock(worldPosition, getBlockState().setValue(WardCoreBlock.WARD, WardState.CLEARED), Block.UPDATE_ALL);
        for (UUID id : new HashSet<>(sealed)) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                p.removeData(LabRegistry.WARD_LINK.get());
                p.sendSystemMessage(Component.translatable("message.%s.ward.cleared".formatted(MODID)));
            }
        }
        sealed.clear();
        Vec3 c = worldPosition.getCenter();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0F, 0.8F);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.5, c.z, 80, 0.4, 1.5, 0.4, 0.12);
        setChanged();
    }

    private void reset(ServerLevel level) {
        sealed.clear();
        away = 0;
        level.setBlock(worldPosition, getBlockState().setValue(WardCoreBlock.WARD, WardState.DORMANT), Block.UPDATE_ALL);
        LabScan.in(level, box(), Resettable.class).forEach(Resettable::labReset);
        setChanged();
    }

    // ---- looks ------------------------------------------------------------------------------------------------------

    /** Sealed: the walls shimmer where a player comes near them, and motes rise off the core. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, WardCoreBlockEntity be) {
        if (state.getValue(WardCoreBlock.WARD) != WardState.SEALED) {
            return;
        }
        var r = level.getRandom();
        Vec3 c = pos.getCenter();
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, c.x + r.nextGaussian() * 0.3, c.y + 0.6, c.z + r.nextGaussian() * 0.3, 0, 0.05, 0);
        }
        AABB box = be.box();
        for (Player p : level.players()) {
            if (!box.inflate(3).contains(p.position())) {
                continue;
            }
            Vec3 e = p.getEyePosition();
            shimmer(level, box.minX, e, 0, r);
            shimmer(level, box.maxX, e, 0, r);
            shimmer(level, box.minZ, e, 2, r);
            shimmer(level, box.maxZ, e, 2, r);
            shimmer(level, box.minY, e, 1, r);
            shimmer(level, box.maxY, e, 1, r);
        }
    }

    private static void shimmer(Level level, double wall, Vec3 eye, int axis, net.minecraft.util.RandomSource r) {
        double d = Math.abs((axis == 0 ? eye.x : axis == 1 ? eye.y : eye.z) - wall);
        if (d > 3 || r.nextFloat() > (3 - d) / 3) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            double u = r.nextGaussian() * 1.4, v = r.nextGaussian() * 1.4;
            double x = axis == 0 ? wall : eye.x + u, y = axis == 1 ? wall : eye.y + (axis == 0 ? u : v), z = axis == 2 ? wall : eye.z + v;
            level.addParticle(SHIMMER, x, y, z, 0, 0, 0);
        }
    }

    // ---- saving and syncing -----------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("min", BlockPos.CODEC, min);
        out.store("max", BlockPos.CODEC, max);
        out.store("entry", BlockPos.CODEC, entry);
        out.store("exit", BlockPos.CODEC, exit);
        out.store("sealed", UUIDUtil.CODEC.listOf(), List.copyOf(sealed));
        out.putInt("away", away);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        min = in.read("min", BlockPos.CODEC).orElse(min);
        max = in.read("max", BlockPos.CODEC).orElse(max);
        entry = in.read("entry", BlockPos.CODEC).orElse(entry);
        exit = in.read("exit", BlockPos.CODEC).orElse(exit);
        sealed.clear();
        in.read("sealed", UUIDUtil.CODEC.listOf()).ifPresent(sealed::addAll);
        away = in.getIntOr("away", 0);
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
