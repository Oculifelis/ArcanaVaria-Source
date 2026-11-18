package com.arcanavaria.lab.boss;

import static com.arcanavaria.ArcanaVaria.id;

import com.arcanavaria.lab.LabRegistry;
import com.arcanavaria.lab.Resettable;
import com.arcanavaria.lab.WardCoreBlockEntity;
import com.arcanavaria.lab.WardState;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * A guardian altar: where a lab's mini-boss rises. The structure sets its pool, how near a player must come and the
 * reward loot table. Inside a ward it wakes when the ward seals; on its own (a lab with no ward) it is always awake.
 * Awake, the first survival player within reach raises the guardian; its death drops the reward and opens the ward.
 */
public class GuardianAltarBlockEntity extends BlockEntity implements Resettable {
    public enum Phase implements StringRepresentable {
        IDLE, ARMED, FIGHTING, DEFEATED;

        public static final com.mojang.serialization.Codec<Phase> CODEC = StringRepresentable.fromEnum(Phase::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private ResourceKey<LabPool> pool = ResourceKey.create(LabRegistry.POOL_KEY, id("default"));
    private ResourceKey<LootTable> loot = ResourceKey.create(Registries.LOOT_TABLE, id("lab/guardian_reward"));
    private int reach = 10;
    private Phase phase = Phase.IDLE;
    private Optional<UUID> boss = Optional.empty();
    private int missing;

    public GuardianAltarBlockEntity(BlockPos pos, BlockState state) {
        super(LabRegistry.GUARDIAN_ALTAR_BE.get(), pos, state);
    }

    public Phase phase() {
        return phase;
    }

    public ResourceKey<LabPool> pool() {
        return pool;
    }

    /** The ward around it sealed: wake. */
    public void arm() {
        if (phase == Phase.IDLE) {
            set(Phase.ARMED);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GuardianAltarBlockEntity be) {
        if (level.getGameTime() % 10 == 3 && level instanceof ServerLevel server) {
            be.watch(server);
        }
    }

    private void watch(ServerLevel level) {
        switch (phase) {
            case IDLE -> {
                WardCoreBlockEntity ward = WardCoreBlockEntity.around(level, worldPosition);
                if (ward == null || ward.state() == WardState.SEALED) {
                    set(Phase.ARMED);
                }
            }
            case ARMED -> {
                Vec3 c = worldPosition.getCenter();
                if (level.getNearestPlayer(c.x, c.y, c.z, reach, EntitySelector.NO_CREATIVE_OR_SPECTATOR) != null) {
                    Entity e = LabBosses.raise(level, worldPosition, pool);
                    if (e != null) {
                        boss = Optional.of(e.getUUID());
                        set(Phase.FIGHTING);
                    }
                }
            }
            case FIGHTING -> {
                // gone without dying (despawned, peaceful): after five seconds with a player near, raise another
                Vec3 c = worldPosition.getCenter();
                boolean gone = boss.isEmpty() || level.getEntity(boss.get()) == null;
                missing = gone && level.getNearestPlayer(c.x, c.y, c.z, reach * 2, false) != null ? missing + 10 : 0;
                if (missing >= 100) {
                    missing = 0;
                    boss = Optional.empty();
                    set(Phase.ARMED);
                }
            }
            default -> {
            }
        }
    }

    /** The guardian it raised died: the reward drops and the ward falls. */
    public void defeated(ServerLevel level) {
        if (phase != Phase.FIGHTING) {
            return;
        }
        boss = Optional.empty();
        set(Phase.DEFEATED);
        Vec3 c = worldPosition.getCenter().add(0, 1, 0);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, c).create(LootContextParamSets.CHEST);
        level.getServer().reloadableRegistries().getLootTable(loot).getRandomItems(params, level.getRandom().nextLong())
                .forEach(stack -> Containers.dropItemStack(level, c.x, c.y, c.z, stack));
        level.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
        WardCoreBlockEntity ward = WardCoreBlockEntity.guarding(level, worldPosition);
        if (ward != null) {
            ward.clear(level);
        }
    }

    @Override
    public void labReset() {
        if (phase == Phase.DEFEATED) {
            return;
        }
        if (level instanceof ServerLevel server && boss.isPresent()) {
            Entity e = server.getEntity(boss.get());
            if (e != null) {
                e.discard();
            }
        }
        boss = Optional.empty();
        set(Phase.IDLE);
    }

    private void set(Phase p) {
        phase = p;
        setChanged();
        BlockState s = getBlockState();
        boolean awake = p == Phase.ARMED || p == Phase.FIGHTING;
        if (level != null && s.getValue(GuardianAltarBlock.AWAKE) != awake) {
            level.setBlock(worldPosition, s.setValue(GuardianAltarBlock.AWAKE, awake), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("pool", ResourceKey.codec(LabRegistry.POOL_KEY), pool);
        out.store("loot", LootTable.KEY_CODEC, loot);
        out.putInt("reach", reach);
        out.store("phase", Phase.CODEC, phase);
        boss.ifPresent(u -> out.store("boss", UUIDUtil.CODEC, u));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        pool = in.read("pool", ResourceKey.codec(LabRegistry.POOL_KEY)).orElse(pool);
        loot = in.read("loot", LootTable.KEY_CODEC).orElse(loot);
        reach = Math.max(2, in.getIntOr("reach", 10));
        phase = in.read("phase", Phase.CODEC).orElse(Phase.IDLE);
        boss = in.read("boss", UUIDUtil.CODEC);
    }
}
