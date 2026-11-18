package com.arcanavaria.exanimator;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.thurible.ThuribleCloud;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The summoning: a Mortuus cloud and a Tenebrae cloud from Thuribles, burning into each other at night inside a wizard
 * tower, call the Exanimator up out of the floor between them. Checked once by each new cloud (ThuribleCloud.tick).
 */
public final class ExanimatorRite {
    private static final Identifier MORTUUS = Identifier.fromNamespaceAndPath("thaumaturge", "mortuus");
    private static final Identifier TENEBRAE = Identifier.fromNamespaceAndPath("thaumaturge", "tenebrae");
    private static final Identifier LUX = Identifier.fromNamespaceAndPath("thaumaturge", "lux");

    public static boolean isLux(ResourceKey<IAspect> aspect) {
        return aspect.identifier().equals(LUX);
    }

    public static void check(ServerLevel level, ThuribleCloud cloud) {
        Identifier mine = cloud.aspect().map(ResourceKey::identifier).orElse(null);
        if (mine == null || !(mine.equals(MORTUUS) || mine.equals(TENEBRAE))) {
            return;
        }
        Identifier wanted = mine.equals(MORTUUS) ? TENEBRAE : MORTUUS;
        List<ThuribleCloud> partners = level.getEntitiesOfClass(ThuribleCloud.class, cloud.getBoundingBox().inflate(8.0),
            c -> c != cloud && c.isAlive() && c.aspect().map(a -> a.identifier().equals(wanted)).orElse(false)
                && c.distanceTo(cloud) < c.radius() + cloud.radius());
        if (partners.isEmpty() || !level.isDarkOutside()) {
            return;
        }
        ThuribleCloud other = partners.get(0);
        Vec3 mid = cloud.position().add(other.position()).scale(0.5);
        BlockPos at = BlockPos.containing(mid);
        if (!inTower(level, at)) {
            return;
        }
        if (!level.getEntitiesOfClass(Exanimator.class, new AABB(at).inflate(64.0)).isEmpty()) {
            return;
        }
        Vec3 g = Exanimator.ground(level, mid.x, mid.y, mid.z);
        if (g == null) {
            g = new Vec3(mid.x, Math.floor(mid.y), mid.z);
        }
        cloud.discard();
        other.discard();
        Exanimator boss = ExanimatorRegistry.EXANIMATOR.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (boss == null) {
            return;
        }
        boss.snapTo(g.x, g.y, g.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        boss.awaken(BlockPos.containing(g));
        level.addFreshEntity(boss);
        level.playSound(null, at, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, g.x, g.y + 0.5, g.z, 60, 1.5, 0.3, 1.5, 0.05);
    }

    private static boolean inTower(ServerLevel level, BlockPos at) {
        var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (Structure s : level.structureManager().getAllStructuresAt(at).keySet()) {
            var key = reg.getKey(s);
            if (key != null && key.getNamespace().equals(ArcanaVaria.MODID) && key.getPath().startsWith("wizard_tower")
                && level.structureManager().getStructureAt(at, s).isValid()) {
                return true;
            }
        }
        return false;
    }

    private ExanimatorRite() {
    }
}
