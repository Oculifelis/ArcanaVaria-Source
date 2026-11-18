package com.arcanavaria.client;

import com.arcanavaria.thurible.ThuribleCloud;
import com.arcanavaria.thurible.ThuribleEffects;
import com.arcanavaria.thurible.ThuribleItem;
import com.arcanavaria.thurible.ThuribleRegistry;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.essentia.jar.BlockEntityJar;
import com.leclowndu93150.thaumaturge.content.item.PhialItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * The Thurible stage of the client test: fill one from an ignis jar, load a second from an aer phial, then (on the
 * client) hold use to swing the first at a zombie standing in front, and hit it once. Logs the fill, the resolved cloud
 * effects of a primal, an explicit compound and a compound with no entry of its own, and what the clouds did.
 */
final class ThuribleAutotest {
    private static final BlockPos JAR = new BlockPos(3, -60, 2);
    private static final Vec3 ZOMBIE = new Vec3(0.5, -60, 4.0);

    private ThuribleAutotest() {
    }

    static void setUp(ServerPlayer p, Logger log) {
        ServerLevel level = (ServerLevel) p.level();
        var server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "kill @e[type=!minecraft:player]");
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 -60 0.5 0 15");
        p.getInventory().clearContent();
        Block jar = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "jar_normal"));
        level.setBlock(JAR, jar.defaultBlockState(), 3);
        if (level.getBlockEntity(JAR) instanceof BlockEntityJar be) {
            be.setStorageContents(TTAspects.IGNIS, 64);
            be.setChanged();
        }
        ItemStack thurible = new ItemStack(ThuribleRegistry.THURIBLE.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, thurible);
        thurible.getItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(JAR), Direction.UP, JAR, false)));
        log.info("AVTEST thurible filled from jar: {} jar left={}", ThuribleItem.contents(p.getMainHandItem()),
                level.getBlockEntity(JAR) instanceof BlockEntityJar be ? be.amount() : -1);

        // a second thurible loaded from a phial in the other hand
        ItemStack second = new ItemStack(ThuribleRegistry.THURIBLE.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, second);
        p.setItemInHand(InteractionHand.OFF_HAND, PhialItem.makeFilled(holder(level, TTAspects.AER)));
        second.getItem().use(level, p, InteractionHand.MAIN_HAND);
        log.info("AVTEST thurible filled from phial: {} offhand now {}", ThuribleItem.contents(second), p.getOffhandItem());
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.MAIN_HAND, thurible);

        for (ResourceKey<IAspect> a : new ResourceKey[] {TTAspects.IGNIS, TTAspects.LUX, key("machina"), key("fabrico")}) {
            StringBuilder sb = new StringBuilder();
            for (ThuribleEffects.Resolved r : ThuribleEffects.resolve(level.registryAccess(), a)) {
                sb.append(r.effect().aspect().identifier().getPath()).append("+").append(r.boost()).append(' ');
            }
            log.info("AVTEST thurible effects {} -> [{}]", a.identifier().getPath(), sb.toString().trim());
        }

        Zombie z = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        if (z != null) {
            z.setPos(ZOMBIE);
            z.setNoAi(true);
            z.setPersistenceRequired();
            level.addFreshEntity(z);
        }
    }

    static void hit(ServerPlayer p, Logger log) {
        Zombie z = zombie(p);
        if (z != null) {
            p.attack(z);
        }
        report(p, log, "after hit");
    }

    static void report(ServerPlayer p, Logger log, String tag) {
        ServerLevel level = (ServerLevel) p.level();
        Zombie z = zombie(p);
        int clouds = level.getEntitiesOfClass(ThuribleCloud.class, p.getBoundingBox().inflate(16)).size();
        log.info("AVTEST thurible {} held={} clouds={} zombie hp={} fire={}", tag, ThuribleItem.contents(p.getMainHandItem()), clouds,
                z == null ? -1 : z.getHealth(), z == null ? -1 : z.getRemainingFireTicks());
    }

    private static Zombie zombie(ServerPlayer p) {
        return p.level().getEntitiesOfClass(Zombie.class, p.getBoundingBox().inflate(12)).stream().findFirst().orElse(null);
    }

    private static ResourceKey<IAspect> key(String path) {
        return ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", path));
    }

    private static net.minecraft.core.Holder<IAspect> holder(ServerLevel level, ResourceKey<IAspect> key) {
        return level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).getOrThrow(key);
    }
}
