package com.arcanavaria.scythe;

import com.arcanavaria.AVRegistry;
import com.arcanavaria.ArcanaVaria;
import com.fundamentum.scythe.Scythes;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Dev-server test of the four scythes and their shared inputs. */
public final class ScytheSelfTest {
    private static final List<Zombie> TARGETS = new ArrayList<>();
    private static final Vec3 AT = new Vec3(8.5, 200, 8.5);
    private static int sweeps;
    private static Zombie waveTarget;
    private static int waveTick;

    private ScytheSelfTest() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((SweepAttackEvent event) -> {
            if (event.getEntity() instanceof Reaper && event.isSweeping()) {
                sweeps++;
            }
        });
        NeoForge.EVENT_BUS.addListener(ScytheSelfTest::tick);
    }

    private static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        try {
            if (server.getTickCount() == 5) {
                ServerLevel level = server.overworld();
                level.setChunkForced(0, 0, true);
                for (int x = 5; x <= 11; x++) {
                    for (int z = 7; z <= 15; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 199, z), Blocks.STONE.defaultBlockState());
                    }
                }
            } else if (server.getTickCount() == 60) {
                run(server.overworld());
            } else if (waveTarget != null && ++waveTick == 20) {
                check(waveTarget.getHealth() < waveTarget.getMaxHealth(), "Elemental crescent wave hit a husk outside melee reach");
                ArcanaVaria.LOG.info("SCYTHETEST COMPLETE");
                finish(server.overworld());
                server.halt(false);
            }
        } catch (RuntimeException failure) {
            ArcanaVaria.LOG.error("SCYTHETEST FAIL", failure);
            finish(server.overworld());
            server.halt(false);
        }
    }

    private static void run(ServerLevel level) {
        Reaper thaumium = equip(level, AVRegistry.THAUMIUM_SCYTHE.get(), 7);
        Zombie primary = target(level, 0, 3.45);
        Zombie adjacent = target(level, 0.6, 2.7);
        thaumium.attack(primary);
        check(sweeps == 1 && primary.getHealth() < 12 && adjacent.getHealth() < adjacent.getMaxHealth(),
                "Thaumium first click: sweeps=" + sweeps + ", primary health=" + primary.getHealth() + ", adjacent health=" + adjacent.getHealth());
        primary.invulnerableTime = 0;
        float before = primary.getHealth();
        thaumium.attack(primary);
        check(sweeps == 1 && before - primary.getHealth() > 3 && before - primary.getHealth() <= 5,
                "Thaumium backswing replaced the second attack and its sweep");
        clearTargets();

        Reaper voidReaper = equip(level, AVRegistry.VOID_SCYTHE.get(), 8);
        Zombie pulled = target(level, 0, 4);
        Scythes.use(voidReaper, true, false);
        check(pulled.getDeltaMovement().z < -0.1 && pulled.getHealth() < pulled.getMaxHealth(), "Void reap pull hit and pulled a husk");
        clearTargets();

        Reaper dasher = equip(level, AVRegistry.VOID_SCYTHE.get(), 8);
        ItemStack step = dasher.getMainHandItem();
        step.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(AVRegistry.REAPERS_STEP), 1);
        Zombie outOfDash = target(level, 0, 4.75);
        Scythes.use(dasher, true, false);
        boolean dashed = step.use(level, dasher, InteractionHand.MAIN_HAND).consumesAction()
                && dasher.getDeltaMovement().horizontalDistance() > 1;
        check(dashed && outOfDash.getHealth() == outOfDash.getMaxHealth() && outOfDash.getDeltaMovement().equals(Vec3.ZERO),
                "Reaper's Step dash=" + dashed + ", reap target health=" + outOfDash.getHealth() + ", motion=" + outOfDash.getDeltaMovement());
        dasher.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        check(!ScytheItem.claimsReapersStep(dasher, false) && ScytheItem.claimsReapersStep(dasher, true),
                "a shield reserves sneak + use for Reaper's Step");
        clearTargets();

        edgeBackswing(level);
        whirlAndHarvest(level, false);
        whirlAndHarvest(level, true);

        Reaper elemental = equip(level, AVRegistry.ELEMENTAL_SCYTHE.get(), 8.5);
        waveTarget = target(level, 0, 6);
        Scythes.use(elemental, true, true);
        Scythes.use(elemental, false, true);
    }

    private static void edgeBackswing(ServerLevel level) {
        Reaper edge = equip(level, AVRegistry.EXANIMATORS_EDGE.get(), 5.5);
        Zombie victim = target(level, 0, 2);
        int before = sweeps;
        Scythes.use(edge, true, false);
        edge.attack(victim);
        victim.invulnerableTime = 0;
        edge.attack(victim);
        check(sweeps == before + 2, "the Edge does not backswing during Whirl");
        Scythes.use(edge, false, false);
        victim.setHealth(victim.getMaxHealth());
        victim.invulnerableTime = 0;
        edge.attack(victim);
        victim.invulnerableTime = 0;
        float health = victim.getHealth();
        edge.attack(victim);
        check(sweeps == before + 3 && victim.getHealth() < health, "the Edge backswing replaces its second sweep after Whirl ends");
        clearTargets();
    }

    private static void whirlAndHarvest(ServerLevel level, boolean devour) {
        Reaper edge = equip(level, AVRegistry.EXANIMATORS_EDGE.get(), 5.5);
        edge.setHealth(10);
        if (devour) {
            edge.getMainHandItem().enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(AVRegistry.SOUL_DEVOURER), 3);
        }
        Zombie victim = target(level, 0, 2);
        victim.setHealth(0.5F);
        Scythes.use(edge, true, false);
        edge.getRandom().setSeed(4096);
        for (int i = 0; i < 5; i++) {
            Scythes.tick(edge);
        }
        Scythes.use(edge, false, false);
        check(!victim.isAlive() && Scythes.souls(edge) == 1 && edge.getHealth() == (devour ? 17 : 11),
                devour ? "Harvest Souls added one health point to Soul Devourer"
                        : "the Edge's whirl killed a husk and harvested one soul and one health point");
        clearTargets();
    }

    private static Reaper equip(ServerLevel level, Item item, double damage) {
        Reaper player = new Reaper(level);
        player.snapTo(AT.x, AT.y, AT.z, 0, 0);
        player.setOnGround(true);
        player.setSpeed(0.1F);
        var stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .forEach(EquipmentSlot.MAINHAND, (attribute, mod) -> player.getAttribute(attribute).addTransientModifier(mod));
        var spec = Scythes.spec(stack);
        check(spec != null && spec.damage == damage && spec.reach == 4
                && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage
                && player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) == 4,
                item + " move damage and reach match the weapon");
        return player;
    }

    private static Zombie target(ServerLevel level, double x, double z) {
        Zombie zombie = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
        if (zombie == null) {
            throw new IllegalStateException("Could not create the husk");
        }
        zombie.snapTo(AT.x + x, AT.y, AT.z + z, 180, 0);
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        if (!level.addFreshEntity(zombie)) {
            throw new IllegalStateException("Could not add the husk");
        }
        TARGETS.add(zombie);
        return zombie;
    }

    private static void check(boolean result, String name) {
        if (!result) {
            throw new IllegalStateException(name);
        }
        ArcanaVaria.LOG.info("SCYTHETEST PASS {}", name);
    }

    private static void clearTargets() {
        TARGETS.forEach(Zombie::discard);
        TARGETS.clear();
    }

    private static void finish(ServerLevel level) {
        level.setChunkForced(0, 0, false);
        waveTarget = null;
        clearTargets();
    }

    private static final class Reaper extends FakePlayer {
        Reaper(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "ScytheTest"));
        }

        @Override
        public float getAttackStrengthScale(float partialTick) {
            return 1;
        }
    }
}
