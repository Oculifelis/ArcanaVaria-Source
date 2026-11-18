package com.arcanavaria.warden;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

/** Planted on the ground, it grows the Greatwood Warden (one per 64 blocks). */
public class HeartseedItem extends Item {
    public HeartseedItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!level.getEntitiesOfClass(GreatwoodWarden.class, new AABB(at).inflate(64.0)).isEmpty()) {
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().sendOverlayMessage(Component.translatable("item.arcanavaria.heartseed.busy").withStyle(ChatFormatting.GREEN));
            }
            return InteractionResult.FAIL;
        }
        GreatwoodWarden warden = WardenRegistry.GREATWOOD_WARDEN.get().create(level, EntitySpawnReason.TRIGGERED);
        if (warden == null) {
            return InteractionResult.FAIL;
        }
        float yaw = ctx.getPlayer() == null ? 0.0F : ctx.getPlayer().getYRot() + 180.0F;
        warden.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0.0F);
        warden.setYHeadRot(yaw);
        warden.setYBodyRot(yaw);
        warden.awaken(at);
        level.addFreshEntity(warden);
        level.playSound(null, at, SoundEvents.ROOTED_DIRT_PLACE, warden.getSoundSource(), 1.0F, 0.6F);
        ctx.getItemInHand().consume(1, ctx.getPlayer());
        return InteractionResult.SUCCESS_SERVER;
    }
}
