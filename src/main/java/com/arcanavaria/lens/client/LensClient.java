package com.arcanavaria.lens.client;

import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.lens.ArchitectsLensItem;
import com.arcanavaria.lens.BlueprintData;
import com.arcanavaria.lens.LensGeometry;
import com.arcanavaria.lens.LensMode;
import com.arcanavaria.lens.LensRegistry;
import com.arcanavaria.lens.LensState;
import com.arcanavaria.lens.LensSettings;
import com.arcanavaria.lens.LensShapes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Lens on the client: sneak + scroll changes mode; while it is held, its work shows as a ghost of motes: the blocks a
 * shape would fill, a blueprint where it would land, or (in a symmetry mode) the copies the block under the crosshair
 * would get, with the anchor marked.
 */
@EventBusSubscriber(modid = ArcanaVaria.MODID, value = Dist.CLIENT)
public final class LensClient {
    private static final int GHOST = 0xFFD060, CORNER = 0xFFD060, ANCHOR = 0xC080FF;
    private static final int DRAWN = 1200;
    private static List<BlockPos> preview = List.of();
    private static boolean destructionPreview, exchangePreview;

    public static List<BlockPos> preview() {
        return preview;
    }

    public static boolean destructionPreview() {
        return destructionPreview;
    }

    public static boolean exchangePreview() {
        return exchangePreview;
    }

    private LensClient() {
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !p.isShiftKeyDown() || Minecraft.getInstance().screen != null || LensRegistry.held(p) == null
                || event.getScrollDeltaY() == 0) {
            return;
        }
        ClientPacketDistributor.sendToServer(new LensRegistry.ModePayload(event.getScrollDeltaY() > 0 ? -1 : 1));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        ClientLevel level = mc.level;
        boolean openMenu = LensKeys.MENU.consumeClick();
        while (LensKeys.MENU.consumeClick()) {
            // Drop key-repeat clicks while the wheel or its settings are open.
        }
        if (p != null && mc.screen == null && LensRegistry.held(p) != null && openMenu) {
            mc.setScreen(new LensWheelScreen(LensRegistry.held(p)));
        }
        if (p == null || level == null) {
            preview = List.of();
            return;
        }
        if (p.tickCount % 3 != 0) {
            return;
        }
        preview = List.of();
        destructionPreview = false;
        exchangePreview = false;
        ItemStack lens = LensRegistry.held(p);
        if (lens == null) {
            return;
        }
        LensState s = ArchitectsLensItem.state(lens);
        LensSettings settings = lens.getOrDefault(LensRegistry.SETTINGS.get(), LensSettings.DEFAULT);
        ItemStack other = p.getMainHandItem() == lens ? p.getOffhandItem() : p.getMainHandItem();
        if ((s.mode().gadgetShape() || s.mode() == LensMode.DESTROY)
                && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            if (s.mode() == LensMode.DESTROY) {
                preview = LensShapes.destruction(p, hit.getBlockPos(), hit.getDirection(), settings);
                destructionPreview = true;
            } else {
                BlockState selected = ArchitectsLensItem.selected(p, lens);
                if (selected != null) {
                    exchangePreview = settings.exchange();
                    preview = LensShapes.collect(p, s.mode(), settings, hit.getBlockPos(), hit.getDirection(), selected)
                            .stream().map(com.arcanavaria.lens.bg2.StatePos::pos).toList();
                }
            }
            return;
        }
        if (s.mode().symmetry()) {
            s.anchor().ifPresent(a -> {
                mote(level, a, ANCHOR, 1.4F);
                if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos at = hit.getBlockPos().relative(hit.getDirection());
                    BlockState st = other.getItem() instanceof BlockItem bi ? bi.getBlock().defaultBlockState()
                            : net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
                    for (LensGeometry.Copy c : LensGeometry.copies(s.mode(), a, at, st)) {
                        mote(level, c.at(), GHOST, 1.0F);
                    }
                }
            });
            return;
        }
        s.a().ifPresent(a -> mote(level, a, CORNER, 1.3F));
        s.b().ifPresent(b -> mote(level, b, CORNER, 1.3F));
        if (s.mode() == LensMode.PASTE) {
            BlueprintData d = other.get(LensRegistry.BLUEPRINT.get());
            if (d != null && s.a().isPresent()) {
                d = d.transformed(settings.rotation(), settings.reflection());
                preview = d.blocks().stream().map(entry -> s.a().get().offset(entry.at())).toList();
                int n = 0;
                for (BlueprintData.Entry e : d.blocks()) {
                    if (n++ > DRAWN) {
                        break;
                    }
                    mote(level, s.a().get().offset(e.at()), GHOST, 0.8F);
                }
            }
        } else if (s.ready()) {
            List<BlockPos> shape = LensGeometry.shape(s.mode(), s.a().get(), s.b().get());
            preview = shape;
            int step = Math.max(1, shape.size() / DRAWN);
            for (int i = 0; i < shape.size(); i += step) {
                mote(level, shape.get(i), GHOST, 0.8F);
            }
        }
    }

    private static void mote(ClientLevel level, BlockPos at, int rgb, float size) {
        level.addParticle(new DustParticleOptions(rgb, size), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0, 0, 0);
    }

    static boolean holding(LocalPlayer p) {
        return p.getItemInHand(InteractionHand.MAIN_HAND).is(LensRegistry.ARCHITECTS_LENS.get())
                || p.getItemInHand(InteractionHand.OFF_HAND).is(LensRegistry.ARCHITECTS_LENS.get());
    }
}
